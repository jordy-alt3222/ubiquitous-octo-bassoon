package com.example.spelunkrtv.schedule

import kotlin.random.Random

/**
 * Channels built on the device from the lineup's own tapes: a channel cut down to some decades,
 * a channel per decade, and MY MIX. They are scheduled from one fixed epoch with a fixed order,
 * so they are as "live" as the lineup's channels: anyone with the same choices sees the same
 * tape at the same second. (They don't follow the website's timing; nothing built here can.)
 */
object Dial {
    /** 2026-11-01T00:00:00Z: when every channel built here starts its first cycle. */
    const val EPOCH_MS = 1_793_491_200_000L

    private val YEAR = Regex("\\b(1[89]\\d\\d|20\\d\\d)\\b")
    /** A year in a title's brackets, "(1991)" or "(Dec. '99)": when it aired, which beats the upload year. */
    private val TITLE_YEAR = Regex("\\([^()]*?(?:\\b(1[89]\\d\\d|20\\d\\d)\\b|'(\\d\\d))[^()]*\\)")

    /** The decade a year text is from (1987 → 1980), or null when there is no year in it. */
    fun decadeOf(year: String?): Int? = year?.let { YEAR.find(it) }?.value?.toInt()?.let { it / 10 * 10 }

    /** The decade a tape aired in: from its title when the title says, else from its year. */
    fun decadeOf(program: Program): Int? {
        TITLE_YEAR.find(program.title)?.let { m ->
            val year = m.groupValues[1].toIntOrNull()
                ?: m.groupValues[2].toInt().let { yy -> if (yy >= 30) 1900 + yy else 2000 + yy }
            return year / 10 * 10
        }
        return decadeOf(program.year)
    }

    /** "THE 80s", "THE 2000s". */
    fun decadeName(decade: Int): String = if (decade >= 2000 || decade < 1920) "THE ${decade}s" else "THE ${decade % 100}s"

    /** Dial numbers for decade channels: the 1920s–1990s on 920–990; the 2000s, 2010s, 2020s on 900, 901, 902. */
    fun decadeDial(decade: Int): Int = when {
        decade >= 2000 -> 900 + (decade - 2000) / 10
        decade >= 1920 -> 900 + decade % 100
        else -> 890 + (decade - 1880) / 10
    }

    /** A channel's own tapes from [decades], in the channel's own order; null if it has none. */
    fun filtered(programs: List<Program>, decades: Set<Int>): ChannelSchedule? {
        val kept = programs.filter { decadeOf(it) in decades }
        return if (kept.isEmpty()) null else schedule(kept)
    }

    /**
     * Tapes from several channels in one: each tape once, in an order that depends only on [id]
     * and the tapes themselves, so every copy of the app shuffles them the same way.
     */
    fun mix(id: String, programs: List<Program>, decades: Set<Int>? = null): ChannelSchedule? {
        val pool = programs
            .filter { decades == null || decadeOf(it) in decades }
            .distinctBy { it.videoUrl }
            .sortedBy { it.videoUrl }
            .shuffled(Random(id.hashCode()))
        return if (pool.isEmpty()) null else schedule(pool)
    }

    private fun schedule(programs: List<Program>) =
        ChannelSchedule(listOf(ScheduleVersion(1, EPOCH_MS, EPOCH_MS, 0, programs)))
}
