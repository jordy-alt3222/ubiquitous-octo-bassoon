package com.example.spelunkrtv.player

import android.net.Uri
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Finds the storage servers that hold a tape and plays from the faster one.
 *
 * archive.org keeps every item on two servers (d1 and its mirror d2), and their speed can differ
 * enormously: one TOONERTIA tape came off d1 at under 1 Mbit/s and off d2 at 60. archive.org's
 * redirect, and spelunkr.tv itself (which always uses d1), can land on the slow one. So, as the
 * site does, this asks archive.org's metadata where the item lives, then times a small read from
 * each server and goes straight to the faster, skipping the redirect on every request. A server
 * that fails or crawls is avoided for a few minutes.
 */
class ArchiveLocator(private val userAgent: String) {
    data class Server(val host: String, val bitsPerSecond: Long?)
    private class Location(val servers: List<Server>, val dir: String)
    private class Speed(val bitsPerSecond: Long?, val at: Long)

    private val located = ConcurrentHashMap<String, Future<Location?>>()
    private val speeds = ConcurrentHashMap<String, Speed>()
    private val avoidedAt = ConcurrentHashMap<String, Long>()
    private val lookups = Executors.newFixedThreadPool(2) { r -> Thread(r, "spelunkr-locate").apply { isDaemon = true } }
    private val probes = Executors.newFixedThreadPool(4) { r -> Thread(r, "spelunkr-probe").apply { isDaemon = true } }

    /**
     * The address to read [uri] from: the faster storage server once known, or the archive.org
     * address. Runs on the player's loading threads, so it may wait briefly for a lookup.
     */
    fun resolve(uri: Uri): Uri {
        val file = parse(uri)?.second ?: return uri
        val location = try {
            locate(uri.toString()).get(RESOLVE_WAIT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            null
        } ?: return uri
        val now = now()
        val server = location.servers
            .filter { s -> avoidedAt[s.host]?.let { now - it > AVOID_MS } ?: true }
            .maxByOrNull { speeds[it.host]?.bitsPerSecond ?: it.bitsPerSecond ?: 0L }
            ?: return uri
        return Uri.parse("https://${server.host}${location.dir}/$file")
    }

    /**
     * True when the tape at [uri] has another server, not being avoided, that was measured faster
     * than [bitsPerSecond] (any answering server will do when that is 0).
     */
    fun hasFasterThan(uri: String, host: String?, bitsPerSecond: Long): Boolean {
        val location = located[parse(Uri.parse(uri))?.first]?.takeIf { it.isDone }?.let { runCatching { it.get() }.getOrNull() } ?: return false
        val now = now()
        return location.servers.any { s ->
            val speed = speeds[s.host]?.bitsPerSecond ?: s.bitsPerSecond
            s.host != host && (avoidedAt[s.host]?.let { now - it > AVOID_MS } ?: true) && speed != null && speed > bitsPerSecond
        }
    }

    /** The server [host] just failed or crawled: use another one for a while. */
    fun avoid(host: String?) {
        if (host == null || host == "archive.org") return
        avoidedAt[host] = now()
        speeds.remove(host)
        Log.i(LivePlayer.TAG, "Avoiding $host for a few minutes")
    }

    /** Starts finding (and timing) the servers for the tape at [videoUrl], if no one has yet. */
    fun lookUp(videoUrl: String) {
        locate(videoUrl)
    }

    private fun locate(videoUrl: String): Future<Location?> {
        val (identifier, file) = parse(Uri.parse(videoUrl)) ?: return CompletedNull
        located[identifier]?.let { return it }
        val task = lookups.submit<Location?> { find(identifier, file) }
        val raced = located.putIfAbsent(identifier, task)
        if (raced != null) {
            task.cancel(true)
            return raced
        }
        return task
    }

    /** Both servers' measured speed for the tape at [videoUrl], for SIGNAL CHECK. Blocking. */
    fun servers(videoUrl: String): List<Server> =
        runCatching { locate(videoUrl).get(SIGNAL_WAIT_MS, TimeUnit.MILLISECONDS)?.servers }.getOrNull().orEmpty()
            .map { it.copy(bitsPerSecond = speeds[it.host]?.bitsPerSecond ?: it.bitsPerSecond) }

    private fun find(identifier: String, file: String): Location? {
        return try {
            val fields = listOf("d1", "d2", "dir").associateWith { name -> probes.submit<String?> { field(identifier, name) } }
                .mapValues { (_, f) -> runCatching { f.get(FIELD_WAIT_MS, TimeUnit.MILLISECONDS) }.getOrNull() }
            val dir = fields["dir"]?.takeIf { DIR.matches(it) && ".." !in it }?.trimEnd('/')
            val hosts = listOfNotNull(fields["d1"], fields["d2"]).filter { HOST.matches(it) }.distinct()
            // No answer: this item plays from its archive.org address, as before. Not asked again.
            if (dir == null || hosts.isEmpty()) return null
            // Race the same small read from each server. The first to finish sets the pace; the
            // other gets a second more, and if it can't finish by then it counts as slow.
            val race = ExecutorCompletionService<Pair<String, Long?>>(probes)
            hosts.forEach { host -> race.submit { host to speedOf(host, dir, file) } }
            val results = HashMap<String, Long?>()
            var deadline = now() + PROBE_WAIT_MS
            repeat(hosts.size) {
                val done = race.poll((deadline - now()).coerceAtLeast(0), TimeUnit.MILLISECONDS) ?: return@repeat
                val (host, speed) = runCatching { done.get() }.getOrNull() ?: return@repeat
                results[host] = speed
                if (speed != null) deadline = minOf(deadline, now() + PROBE_GRACE_MS)
            }
            val timed = hosts.map { Server(it, results[it]) }
            timed.forEach { s -> speeds[s.host] = Speed(s.bitsPerSecond, now()) }
            Log.i(LivePlayer.TAG, "$identifier is on " + timed.joinToString(" / ") { s ->
                "${s.host} (${s.bitsPerSecond?.let { "${it / 1000} kbit/s" } ?: "no answer"})"
            })
            Location(timed, dir)
        } catch (e: Exception) {
            Log.i(LivePlayer.TAG, "Couldn't find the servers for $identifier ($e); using archive.org")
            null
        }
    }

    /** Reads the first [PROBE_BYTES] of the file from one server; null if it fails or stalls. */
    private fun speedOf(host: String, dir: String, file: String): Long? {
        speeds[host]?.takeIf { now() - it.at < SPEED_FRESH_MS }?.let { return it.bitsPerSecond }
        val conn = URL("https://$host$dir/$file").openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Range", "bytes=0-${PROBE_BYTES - 1}")
            val started = now()
            if (conn.responseCode !in 200..299) return null
            var read = 0L
            val buffer = ByteArray(32 * 1024)
            conn.inputStream.use { input ->
                while (read < PROBE_BYTES && now() - started < PROBE_WAIT_MS) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    read += n
                }
            }
            read * 8_000 / (now() - started).coerceAtLeast(1)
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /** One field of archive.org's metadata, e.g. /metadata/<id>/d1 answers {"result":"ia601003.us.archive.org"}. */
    private fun field(identifier: String, name: String): String? {
        val conn = URL("https://archive.org/metadata/${Uri.encode(identifier)}/$name").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("User-Agent", userAgent)
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return RESULT.find(body)?.groupValues?.get(1)
        } finally {
            conn.disconnect()
        }
    }

    /** archive.org/download/<identifier>/<file> into its parts, the file kept exactly as encoded. */
    private fun parse(uri: Uri): Pair<String, String>? {
        if (uri.host != "archive.org") return null
        val segments = uri.pathSegments
        if (segments.size < 3 || segments[0] != "download") return null
        val identifier = segments[1]
        return identifier to uri.encodedPath!!.substringAfter("/download/$identifier/")
    }

    /** Monotonic, in milliseconds. */
    private fun now() = System.nanoTime() / 1_000_000

    private object CompletedNull : Future<Location?> {
        override fun cancel(mayInterruptIfRunning: Boolean) = false
        override fun isCancelled() = false
        override fun isDone() = true
        override fun get(): Location? = null
        override fun get(timeout: Long, unit: TimeUnit): Location? = null
    }

    private companion object {
        const val RESOLVE_WAIT_MS = 4_000L
        const val SIGNAL_WAIT_MS = 15_000L
        const val FIELD_WAIT_MS = 8_000L
        const val PROBE_WAIT_MS = 4_000L
        const val PROBE_GRACE_MS = 1_000L
        const val PROBE_BYTES = 512L * 1024
        const val SPEED_FRESH_MS = 10 * 60_000L
        const val AVOID_MS = 5 * 60_000L
        val RESULT = Regex("\"result\"\\s*:\\s*\"([^\"]+)\"")
        // The same checks the site makes before trusting these fields in a URL.
        val HOST = Regex("^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$")
        val DIR = Regex("^/[A-Za-z0-9._~/-]+$")
    }
}
