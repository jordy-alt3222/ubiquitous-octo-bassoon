package com.example.spelunkrtv.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialTest {
    private fun tape(name: String, year: String?) = Program(name, "$name.mp4", name, year, null, 30 * 60_000L, 0)

    private val a70 = tape("a", "1974")
    private val b80 = tape("b", "1985")
    private val c80 = tape("c", "1989-1990")
    private val d = tape("d", null)

    @Test
    fun readsTheDecadeFromWhateverTheYearSays() {
        assertEquals(1970, Dial.decadeOf("1974"))
        assertEquals(1980, Dial.decadeOf("1989-1990"))
        assertEquals(2000, Dial.decadeOf("c. 2003"))
        assertNull(Dial.decadeOf(null))
        assertNull(Dial.decadeOf("unknown"))
        assertEquals("THE 80s", Dial.decadeName(1980))
        assertEquals("THE 2000s", Dial.decadeName(2000))
        assertEquals(980, Dial.decadeDial(1980))
        assertEquals(900, Dial.decadeDial(2000))
        assertEquals(902, Dial.decadeDial(2020))
        assertEquals(920, Dial.decadeDial(1920))
    }

    @Test
    fun theTitleSaysWhenItAiredBetterThanTheUploadYear() {
        assertEquals(1990, Dial.decadeOf(tape("CapZeyeZ Live! (1991)", "2023")))
        assertEquals(1980, Dial.decadeOf(tape("Disney Channel Halloween Block ('88)", null)))
        assertEquals(1990, Dial.decadeOf(tape("Pokemon on Kids' WB (Dec. '99)", "2021")))
        assertEquals(2000, Dial.decadeOf(tape("Toonami (Nov. '02)", null)))
        assertEquals(1970, Dial.decadeOf(tape("No year in the title", "1974")))
    }

    @Test
    fun aChannelCutToDecadesKeepsItsOwnOrder() {
        val s = Dial.filtered(listOf(c80, a70, d, b80), setOf(1980))!!
        assertEquals(listOf(c80, b80), s.programsAt(Dial.EPOCH_MS))
        assertNull("nothing from the 60s", Dial.filtered(listOf(a70, b80), setOf(1960)))
    }

    @Test
    fun aMixHasEachTapeOnceInTheSameOrderEverywhere() {
        val one = Dial.mix("decade-1980", listOf(b80, c80, a70, b80, d), setOf(1980))!!.programsAt(0)
        val two = Dial.mix("decade-1980", listOf(c80, b80, d, a70), setOf(1980))!!.programsAt(0)
        assertEquals(setOf(b80, c80), one.toSet())
        assertEquals(2, one.size)
        assertEquals("same tapes, same order, whatever order they came in", one, two)
        assertTrue(Dial.mix("my-mix", listOf(a70, d))!!.programsAt(0).containsAll(listOf(a70, d)))
    }

    @Test
    fun builtChannelsAreLiveAtAnyMoment() {
        val s = Dial.mix("decade-1980", listOf(b80, c80), setOf(1980))!!
        for (t in listOf(Dial.EPOCH_MS - 86_400_000L, Dial.EPOCH_MS, Dial.EPOCH_MS + 3_600_000L * 24 * 900)) {
            assertTrue(s.airingAt(t).contains(t))
        }
    }
}
