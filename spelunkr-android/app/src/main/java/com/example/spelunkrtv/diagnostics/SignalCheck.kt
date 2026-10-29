package com.example.spelunkrtv.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.spelunkrtv.data.BroadcastRepository
import com.example.spelunkrtv.data.Lineup
import com.example.spelunkrtv.player.LivePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

enum class Verdict { Checking, Ok, Warn, Fail }

data class CheckRow(val label: String, val verdict: Verdict, val detail: String)

/** The tape on air when the check started, so the Archive test fetches what the player fetches. */
data class TapeUnderTest(val url: String, val bitsPerSecond: Long?)

/**
 * SIGNAL CHECK: walks the path a tape takes to the screen, one link at a time: this device's
 * network, its clock, the Spelunkr schedule, and a real download from the Internet Archive.
 */
class SignalCheck(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun run(tape: TapeUnderTest?, emit: (List<CheckRow>) -> Unit) = withContext(Dispatchers.IO) {
        val rows = mutableListOf(
            CheckRow(NETWORK, Verdict.Checking, "Checking…"),
            CheckRow(SCHEDULE, Verdict.Checking, "Waiting…"),
            CheckRow(CLOCK, Verdict.Checking, "Waiting…"),
            CheckRow(ARCHIVE, Verdict.Checking, "Waiting…"),
        )
        fun set(index: Int, row: CheckRow) {
            rows[index] = row
            Log.i(LivePlayer.TAG, "Signal check · ${row.label}: ${row.verdict} · ${row.detail}")
            emit(rows.toList())
        }
        emit(rows.toList())

        set(0, network())
        val (scheduleRow, serverTime) = schedule()
        set(1, scheduleRow)
        set(2, clock(serverTime))
        set(3, archive(tape))
    }

    private fun network(): CheckRow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            ?: return CheckRow(NETWORK, Verdict.Fail, "No network. Connect this device to Wi-Fi or Ethernet")
        val kind = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            else -> "Network"
        } + if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) " through a VPN" else ""
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            CheckRow(NETWORK, Verdict.Ok, "$kind · Android confirms it reaches the internet")
        } else {
            CheckRow(NETWORK, Verdict.Warn, "$kind · Android couldn't confirm internet access (a sign-in page, or a blocked network?)")
        }
    }

    /** The schedule, live feed first and then the site's own copy, as the app loads it. */
    private fun schedule(): Pair<CheckRow, Long?> {
        val live = runCatching { get(BroadcastRepository.LIVE_BASE + "lineup.json") }
        live.getOrNull()?.let { r ->
            if (r.code in 200..299) return CheckRow(SCHEDULE, Verdict.Ok, "Live feed · ${channelCount(r.body)} channels · answered in ${r.firstByteMs} ms") to r.serverTime
        }
        val liveProblem = live.exceptionOrNull()?.let { networkReason(it, "the schedule server") ?: it.message }
            ?: live.getOrNull()?.let { httpReason(it.code, "The schedule server") }
        val backup = runCatching { get(BroadcastRepository.SNAPSHOT_BASE + "lineup.json") }
        backup.getOrNull()?.let { r ->
            if (r.code in 200..299) {
                return CheckRow(SCHEDULE, Verdict.Warn, "Live feed failed ($liveProblem). The backup on spelunkr.tv works · ${channelCount(r.body)} channels") to r.serverTime
            }
        }
        val backupProblem = backup.exceptionOrNull()?.let { networkReason(it, "spelunkr.tv") ?: it.message }
            ?: backup.getOrNull()?.let { httpReason(it.code, "spelunkr.tv") }
        return CheckRow(SCHEDULE, Verdict.Fail, "$liveProblem. Backup: $backupProblem") to null
    }

    private fun clock(serverTime: Long?): CheckRow {
        val device = System.currentTimeMillis()
        if (serverTime == null) {
            val shown = DateFormat.getDateTimeInstance().format(Date(device))
            return CheckRow(CLOCK, Verdict.Warn, "Couldn't compare with internet time. This device says $shown")
        }
        val off = device - serverTime
        return if (abs(off) < CLOCK_TOLERANCE_MS) {
            CheckRow(CLOCK, Verdict.Ok, "Matches internet time (within ${formatSpan(off)})")
        } else {
            CheckRow(
                CLOCK, Verdict.Warn,
                "${formatSpan(off)} ${if (off > 0) "fast" else "slow"}. The guide corrects for it, but secure connections can fail: set the date and time automatically",
            )
        }
    }

    /** Downloads the start of the tape on air, the way the player does, and times it. */
    private fun archive(tape: TapeUnderTest?): CheckRow {
        val url = tape?.url ?: FALLBACK_TAPE
        val started = millis()
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = LivePlayer.CONNECT_TIMEOUT_MS
            conn.readTimeout = LivePlayer.READ_TIMEOUT_MS
            conn.setRequestProperty("User-Agent", LivePlayer.userAgent(context))
            conn.setRequestProperty("Range", "bytes=0-${SAMPLE_BYTES - 1}")
            val code = conn.responseCode
            val firstByte = millis() - started
            val host = conn.url.host
            if (code !in 200..299) return CheckRow(ARCHIVE, Verdict.Fail, httpReason(code, "archive.org") + " · from $host")
            var read = 0L
            val buffer = ByteArray(64 * 1024)
            conn.inputStream.use { input ->
                while (read < SAMPLE_BYTES && millis() - started < SAMPLE_MAX_MS) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    read += n
                }
            }
            val readMs = (millis() - started - firstByte).coerceAtLeast(1)
            val speed = read * 8_000 / readMs
            val summary = "HTTP $code from $host · answered in ${"%.1f".format(firstByte / 1000.0)} s · " +
                "${formatBytes(read)} at about ${formatBits(speed)}"
            val needs = tape?.bitsPerSecond
            when {
                read == 0L -> CheckRow(ARCHIVE, Verdict.Fail, "archive.org answered (HTTP $code from $host) but sent no video")
                needs != null && speed < needs -> CheckRow(
                    ARCHIVE, Verdict.Warn,
                    "$summary. The tape on air needs about ${formatBits(needs)}, so expect pauses",
                )
                else -> CheckRow(ARCHIVE, Verdict.Ok, summary + (needs?.let { ". The tape on air needs about ${formatBits(it)}" } ?: ""))
            }
        } catch (e: Exception) {
            CheckRow(ARCHIVE, Verdict.Fail, networkReason(e, "archive.org") ?: "${e.javaClass.simpleName}: ${e.message}")
        } finally {
            conn.disconnect()
        }
    }

    private class Response(val code: Int, val body: String, val firstByteMs: Long, val serverTime: Long?)

    private fun get(url: String): Response {
        val started = millis()
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.useCaches = false
            val code = conn.responseCode
            val firstByte = millis() - started
            val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
            val date = conn.getHeaderFieldDate("Date", 0L).takeIf { it > 0 }
            return Response(code, body, firstByte, date)
        } finally {
            conn.disconnect()
        }
    }

    private fun channelCount(body: String) =
        runCatching { json.decodeFromString<Lineup>(body).channels.count { !it.debugOnly } }.getOrDefault(0)

    /** Monotonic, and real time even where the system clock is faked (Robolectric). */
    private fun millis() = System.nanoTime() / 1_000_000

    companion object {
        const val NETWORK = "THIS DEVICE'S NETWORK"
        const val SCHEDULE = "SPELUNKR SCHEDULE"
        const val CLOCK = "DEVICE CLOCK"
        const val ARCHIVE = "INTERNET ARCHIVE"
        private const val SAMPLE_BYTES = 2L * 1024 * 1024
        private const val SAMPLE_MAX_MS = 10_000L
        private const val CLOCK_TOLERANCE_MS = 60_000L
        /** Used only when nothing is tuned yet: a long-standing Archive item that HAPPY TREES airs. */
        private const val FALLBACK_TAPE =
            "https://archive.org/download/the-joy-painting-almost-hd/" +
                "The%20Joy%20of%20Painting%20-%20S01E01%20-%20A%20Walk%20in%20the%20Woods%20%28Season%201%20Episode%201%29.mp4"
    }
}
