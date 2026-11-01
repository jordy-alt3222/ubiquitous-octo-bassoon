package com.example.spelunkrtv.data

import android.content.Context
import com.example.spelunkrtv.schedule.ChannelSchedule
import com.example.spelunkrtv.schedule.Program
import com.example.spelunkrtv.schedule.ScheduleVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * ru-splunker's channels: a lineup built by tools/build_ru_lineup.py from playable archive.org
 * video and bundled with the app. Each channel runs its tapes back to back from one fixed
 * epoch, so every copy of the app is on the same tape at the same second, like Spelunkr.
 */
class RuBroadcast(private val context: Context) : Broadcast {
    @Serializable
    private data class RuLineup(val epochMs: Long, val channels: List<RuChannel>)

    @Serializable
    private data class RuChannel(
        val id: String,
        val dial: Int,
        val name: String,
        val family: String? = null,
        val color: String = "#888888",
        val description: String = "",
        val items: List<RuTape> = emptyList(),
    )

    @Serializable
    private data class RuTape(
        val id: String,
        val file: String,
        val duration: Int,
        val title: String,
        val year: String? = null,
        val moov: Long? = null,
        val description: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val lineup: RuLineup by lazy {
        json.decodeFromString(context.assets.open("ru-lineup.json").bufferedReader().use { it.readText() })
    }
    @Volatile private var skewMs = 0L

    override fun now(): Long = System.currentTimeMillis() + skewMs

    override suspend fun channels(withPreviews: Boolean): List<Channel> = withContext(Dispatchers.IO) {
        measureSkew()
        lineup.channels.filter { it.items.isNotEmpty() }.sortedBy { it.dial }.map { c ->
            Channel(
                id = c.id,
                name = c.name,
                dial = c.dial,
                color = (c.color.removePrefix("#").toLongOrNull(16) ?: 0x888888L) or 0xFF000000L,
                description = c.description,
                family = c.family,
                logoRows = emptyList(),
                items = c.items.size,
                videoSeconds = c.items.sumOf { it.duration.toLong() },
                versions = emptyList(),
            )
        }
    }

    override suspend fun schedule(channel: Channel): ChannelSchedule {
        val c = lineup.channels.first { it.id == channel.id }
        val programs = c.items.map { t ->
            Program(
                archiveId = t.id,
                fileName = t.file,
                title = t.title,
                year = t.year,
                description = t.description,
                durationMs = t.duration * 1000L,
                inPointMs = 0,
                indexBytes = t.moov,
            )
        }
        return ChannelSchedule(listOf(ScheduleVersion(1, lineup.epochMs, lineup.epochMs, 0, programs)))
    }

    /** The schedule is in the app, so the clock check uses archive.org's own time. */
    private fun measureSkew() {
        runCatching {
            val sent = System.currentTimeMillis()
            val conn = URL("https://archive.org/").openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.requestMethod = "HEAD"
            val server = conn.getHeaderFieldDate("Date", 0L)
            conn.disconnect()
            if (server > 0) {
                val received = System.currentTimeMillis()
                val skew = server + (received - sent) / 2 - received
                skewMs = if (abs(skew) > 3_000) skew else 0
            }
        }
    }
}
