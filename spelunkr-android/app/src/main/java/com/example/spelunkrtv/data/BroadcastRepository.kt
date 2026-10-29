package com.example.spelunkrtv.data

import com.example.spelunkrtv.schedule.ChannelSchedule
import com.example.spelunkrtv.schedule.Program
import com.example.spelunkrtv.schedule.ScheduleVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/** A channel as the app shows it. */
data class Channel(
    val id: String,
    val name: String,
    val dial: Int,
    val color: Long,
    val description: String,
    val family: String?,
    val logoRows: List<String>,
    val items: Int,
    val videoSeconds: Long,
    internal val versions: List<VersionRef>,
) {
    /** "02", "35", "600": how the site prints a dial number. */
    val dialLabel: String get() = if (dial < 100) dial.toString().padStart(2, '0') else dial.toString()
}

/**
 * Reads the same public broadcast files spelunkr.tv reads: first from the live bucket, then from
 * the snapshot the site serves next to its pages. No key or account is needed for either.
 */
class BroadcastRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val schedules = HashMap<String, ChannelSchedule>()
    private val scheduleLock = Mutex()

    /** Wall clock correction: server time minus device time, when the device is clearly off. */
    @Volatile
    var clockSkewMs: Long = 0
        private set

    fun now(): Long = System.currentTimeMillis() + clockSkewMs

    suspend fun channels(): List<Channel> {
        val lineup = json.decodeFromString<Lineup>(fetch("lineup.json", measureSkew = true))
        return lineup.channels
            .filter { !it.debugOnly && !it.comingSoon && it.dial != null && it.versions.isNotEmpty() }
            .sortedBy { it.dial }
            .map { dto ->
                Channel(
                    id = dto.id,
                    name = dto.name,
                    dial = dto.dial!!,
                    color = parseColor(dto.color),
                    description = dto.description,
                    family = dto.family,
                    logoRows = dto.logomark?.rows.orEmpty(),
                    items = dto.stats?.items ?: 0,
                    videoSeconds = dto.stats?.videoSeconds ?: 0,
                    versions = dto.versions,
                )
            }
    }

    suspend fun schedule(channel: Channel): ChannelSchedule {
        scheduleLock.withLock { schedules[channel.id] }?.let { return it }
        val versions = coroutineScope {
            channel.versions.map { ref -> async { loadVersion(ref) } }.awaitAll()
        }
        val schedule = ChannelSchedule(versions)
        scheduleLock.withLock { schedules[channel.id] = schedule }
        return schedule
    }

    private suspend fun loadVersion(ref: VersionRef): ScheduleVersion = coroutineScope {
        val manifest = async { json.decodeFromString<Manifest>(fetch(ref.manifest)) }
        // Descriptions are a nicety; a missing details file must not take the channel off the air.
        val details = async {
            ref.details?.let { path -> runCatching { json.decodeFromString<Details>(fetch(path)) }.getOrNull() }
        }
        val items = manifest.await().items
        val descriptions = details.await()?.items.orEmpty()
        ScheduleVersion(
            v = ref.v,
            epochMs = ref.epochMs,
            effectiveAtMs = ref.effectiveAtMs ?: ref.epochMs,
            startIndex = ref.startIndex,
            programs = items.mapIndexed { i, item ->
                Program(
                    archiveId = item.id,
                    fileName = item.file ?: item.mp4 ?: error("${item.id} has no file"),
                    title = item.title ?: item.id,
                    year = item.year,
                    description = descriptions.getOrNull(i)?.description,
                    durationMs = item.duration * 1000L,
                    inPointMs = (item.inS ?: 0) * 1000L,
                )
            },
        )
    }

    private suspend fun fetch(path: String, measureSkew: Boolean = false): String = withContext(Dispatchers.IO) {
        try {
            get(LIVE_BASE + path, timeoutMs = 4_000, measureSkew = measureSkew)
        } catch (e: Exception) {
            get(SNAPSHOT_BASE + path, timeoutMs = 15_000, measureSkew = measureSkew)
        }
    }

    private fun get(url: String, timeoutMs: Int, measureSkew: Boolean): String {
        val sentAt = System.currentTimeMillis()
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.useCaches = false
            val code = conn.responseCode
            if (code !in 200..299) throw java.io.IOException("HTTP $code for $url")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            if (measureSkew) {
                val serverMs = conn.getHeaderFieldDate("Date", 0L)
                if (serverMs > 0) {
                    val receivedAt = System.currentTimeMillis()
                    val skew = serverMs + (receivedAt - sentAt) / 2 - receivedAt
                    // The Date header has one-second resolution; only correct a clock that is clearly wrong.
                    clockSkewMs = if (abs(skew) > SKEW_TOLERANCE_MS) skew else 0
                }
            }
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun parseColor(hex: String): Long {
        val digits = hex.removePrefix("#")
        return (digits.toLongOrNull(16) ?: 0x888888L) or 0xFF000000L
    }

    companion object {
        const val LIVE_BASE = "https://jbuopkuluyfjwgsrigry.supabase.co/storage/v1/object/public/broadcast/"
        const val SNAPSHOT_BASE = "https://spelunkr.tv/broadcast/"
        private const val SKEW_TOLERANCE_MS = 3_000L
    }
}
