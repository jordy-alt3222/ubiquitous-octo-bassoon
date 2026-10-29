package com.example.spelunkrtv.schedule

import java.net.URLEncoder

/** One tape as it airs: where it lives on the Internet Archive and which part of it plays. */
data class Program(
    val archiveId: String,
    val fileName: String,
    val title: String,
    val year: String?,
    val description: String?,
    /** Time on air, already trimmed. */
    val durationMs: Long,
    /** Where in the file the aired part begins. */
    val inPointMs: Long,
) {
    val videoUrl: String
        get() = "https://archive.org/download/$archiveId/" +
            URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")

    val archivePageUrl: String get() = "https://archive.org/details/$archiveId"
}

/** A program placed on the wall clock. [key] is stable for one airing (version:cycle:index). */
data class Airing(
    val program: Program,
    val startMs: Long,
    val endMs: Long,
    val key: String,
) {
    val durationMs: Long get() = endMs - startMs
    fun contains(t: Long) = t in startMs until endMs
}

/**
 * One published schedule for a channel. The first cycle starts at [epochMs] with the tape at
 * [startIndex]; after it the full list repeats back to back forever, and before [epochMs] it
 * repeats backwards the same way. Everyone who reads the same files at the same wall-clock
 * time lands on the same tape at the same second, which is what makes the channels "live".
 */
class ScheduleVersion(
    val v: Int,
    val epochMs: Long,
    val effectiveAtMs: Long,
    val startIndex: Int,
    val programs: List<Program>,
) {
    init {
        require(programs.isNotEmpty()) { "v$v has no programs" }
        require(startIndex in programs.indices) { "v$v startIndex $startIndex out of range" }
        require(programs.all { it.durationMs > 0 }) { "v$v has a program with no runtime" }
    }

    private val cycleMs = programs.sumOf { it.durationMs }
    private val firstCycleMs = programs.subList(startIndex, programs.size).sumOf { it.durationMs }

    fun airingAt(t: Long): Airing {
        val cycle: Long
        val cycleStart: Long
        val first: Int
        when {
            t >= epochMs && t < epochMs + firstCycleMs -> {
                cycle = 0; cycleStart = epochMs; first = startIndex
            }
            t >= epochMs -> {
                val k = (t - epochMs - firstCycleMs) / cycleMs
                cycle = k + 1; cycleStart = epochMs + firstCycleMs + k * cycleMs; first = 0
            }
            else -> {
                val k = ceilDiv(epochMs - t, cycleMs)
                cycle = -k; cycleStart = epochMs - k * cycleMs; first = 0
            }
        }
        var start = cycleStart
        for (i in first until programs.size) {
            val p = programs[i]
            if (t < start + p.durationMs) return Airing(p, start, start + p.durationMs, "$v:$cycle:$i")
            start += p.durationMs
        }
        error("unreachable: $t is past the end of cycle $cycle")
    }

    private fun ceilDiv(a: Long, b: Long) = (a + b - 1) / b
}

/** Every published version of a channel's schedule; a newer version takes over at its effectiveAtMs. */
class ChannelSchedule(versions: List<ScheduleVersion>) {
    private val versions = versions.sortedBy { it.v }

    init {
        require(this.versions.isNotEmpty()) { "no published version" }
    }

    private fun versionAt(t: Long): ScheduleVersion =
        versions.lastOrNull { it.effectiveAtMs <= t }
            ?: versions.minWith(compareBy<ScheduleVersion> { it.effectiveAtMs }.thenByDescending { it.v })

    fun airingAt(t: Long): Airing {
        val version = versionAt(t)
        val airing = version.airingAt(t)
        // A newer version cuts in at its effective time, even mid-tape.
        val cut = versions.filter { it.v > version.v && it.effectiveAtMs > t }.minOfOrNull { it.effectiveAtMs }
        return if (cut != null && cut < airing.endMs) airing.copy(endMs = cut) else airing
    }

    /** Airings overlapping [fromMs, untilMs), in order. */
    fun airings(fromMs: Long, untilMs: Long): List<Airing> {
        val out = ArrayList<Airing>()
        var t = fromMs
        while (t < untilMs) {
            val a = airingAt(t)
            out += a
            t = a.endMs
        }
        return out
    }

    /** The [count] airings after the one on at [t]. */
    fun upNext(t: Long, count: Int): List<Airing> {
        val out = ArrayList<Airing>(count)
        var next = airingAt(t).endMs
        repeat(count) {
            val a = airingAt(next)
            out += a
            next = a.endMs
        }
        return out
    }
}
