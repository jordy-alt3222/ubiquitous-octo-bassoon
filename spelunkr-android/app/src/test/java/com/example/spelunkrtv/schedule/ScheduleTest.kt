package com.example.spelunkrtv.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTest {
    private fun program(name: String, minutes: Long) = Program(
        archiveId = name, fileName = "$name.mp4", title = name, year = null, description = null,
        durationMs = minutes * 60_000, inPointMs = 0,
    )

    private val a = program("a", 10)
    private val b = program("b", 20)
    private val c = program("c", 30)
    private val epoch = 1_000_000_000_000L
    private fun min(m: Long) = m * 60_000

    @Test
    fun firstCycleStartsAtStartIndex() {
        val v = ScheduleVersion(1, epoch, epoch, startIndex = 1, programs = listOf(a, b, c))
        assertEquals(b, v.airingAt(epoch).program)
        assertEquals(c, v.airingAt(epoch + min(20)).program)
        // First cycle is b + c (50 min), then the full list a, b, c repeats.
        assertEquals(a, v.airingAt(epoch + min(50)).program)
        assertEquals(epoch + min(50), v.airingAt(epoch + min(55)).startMs)
        assertEquals(b, v.airingAt(epoch + min(60)).program)
        assertEquals(a, v.airingAt(epoch + min(50 + 60)).program)
        assertEquals("1:2:0", v.airingAt(epoch + min(50 + 60)).key)
    }

    @Test
    fun beforeEpochRunsTheFullListBackwards() {
        val v = ScheduleVersion(1, epoch, epoch, startIndex = 2, programs = listOf(a, b, c))
        val last = v.airingAt(epoch - 1)
        assertEquals(c, last.program)
        assertEquals(epoch, last.endMs)
        assertEquals(a, v.airingAt(epoch - min(60)).program)
        assertEquals(epoch - min(60), v.airingAt(epoch - min(60)).startMs)
    }

    @Test
    fun newerVersionCutsInAtItsEffectiveTime() {
        val v1 = ScheduleVersion(1, epoch, epoch, 0, listOf(a, b, c))
        val switch = epoch + min(15) // halfway through b
        val v2 = ScheduleVersion(2, switch, switch, 0, listOf(c))
        val s = ChannelSchedule(listOf(v2, v1))
        val beforeSwitch = s.airingAt(epoch + min(12))
        assertEquals(b, beforeSwitch.program)
        assertEquals(switch, beforeSwitch.endMs)
        assertEquals(c, s.airingAt(switch).program)
        assertEquals(listOf(a, b, c), s.airings(epoch, switch + 1).map { it.program })
    }

    @Test
    fun upNextFollowsTheCurrentAiring() {
        val s = ChannelSchedule(listOf(ScheduleVersion(1, epoch, epoch, 0, listOf(a, b, c))))
        assertEquals(listOf(b, c, a), s.upNext(epoch + min(5), 3).map { it.program })
    }

    @Test
    fun videoUrlIsEncodedLikeTheSite() {
        val p = program("x", 1).copy(archiveId = "some-id", fileName = "Show (1986) & More.mp4")
        assertEquals("https://archive.org/download/some-id/Show%20%281986%29%20%26%20More.mp4", p.videoUrl)
    }
}
