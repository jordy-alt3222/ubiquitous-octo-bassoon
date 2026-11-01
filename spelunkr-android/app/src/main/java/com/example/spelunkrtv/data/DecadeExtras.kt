package com.example.spelunkrtv.data

import android.content.Context
import com.example.spelunkrtv.schedule.Program
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tapes from archive.org's own collections (silent and feature films, Prelinger, classic TV, old
 * commercials) that top up the decades Spelunkr has little of. Built by tools/build_decade_extras.py
 * and bundled with SPELUNKR only. They go into the decade channels, never into Spelunkr's channels.
 */
object DecadeExtras {
    @Serializable
    private data class File(val decades: Map<String, List<Tape>> = emptyMap())

    @Serializable
    private data class Tape(
        val id: String,
        val file: String,
        val duration: Int,
        val title: String,
        val year: String? = null,
        val moov: Long? = null,
        val description: String? = null,
    )

    private const val ASSET = "decade-extras.json"

    /** Decade (1920 = the 20s) to its extra tapes; empty when this app carries none. */
    fun load(context: Context): Map<Int, List<Program>> {
        if (ASSET !in context.assets.list("").orEmpty()) return emptyMap()
        val json = Json { ignoreUnknownKeys = true }
        val file = json.decodeFromString<File>(context.assets.open(ASSET).bufferedReader().use { it.readText() })
        return file.decades.mapNotNull { (decade, tapes) ->
            decade.toIntOrNull()?.let { d ->
                d to tapes.map { t ->
                    Program(
                        archiveId = t.id, fileName = t.file, title = t.title, year = t.year, description = t.description,
                        durationMs = t.duration * 1000L, inPointMs = 0, indexBytes = t.moov,
                    )
                }
            }
        }.toMap()
    }
}
