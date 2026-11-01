package com.example.spelunkrtv.data

import com.example.spelunkrtv.schedule.ChannelSchedule
import com.example.spelunkrtv.schedule.Program
import com.example.spelunkrtv.schedule.ScheduleVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** ru-splunker's bundled lineup must be playable as it stands. */
class RuLineupTest {
    private val lineup = Json.parseToJsonElement(File("src/ru/assets/ru-lineup.json").readText()).jsonObject
    private val channels = lineup.getValue("channels").jsonArray.map { it.jsonObject }

    @Test
    fun everyChannelHasTapesAndItsOwnDial() {
        assertTrue(channels.size >= 5)
        assertEquals(channels.size, channels.map { it.getValue("dial").jsonPrimitive.int }.toSet().size)
        channels.forEach { assertTrue("${it["name"]} is empty", it.getValue("items").jsonArray.size >= 10) }
    }

    @Test
    fun everyTapeIsAPlayableMp4OnlyOnce() {
        val tapes = channels.flatMap { it.getValue("items").jsonArray.map { t -> t.jsonObject } }
        assertEquals(tapes.size, tapes.map { it.getValue("id").jsonPrimitive.content }.toSet().size)
        tapes.forEach { t ->
            assertTrue(t.getValue("file").jsonPrimitive.content.endsWith(".mp4", ignoreCase = true))
            assertTrue(t.getValue("duration").jsonPrimitive.int in 120..6 * 3600)
        }
    }

    @Test
    fun onlyTheUssrEra() {
        channels.flatMap { it.getValue("items").jsonArray }.forEach { t ->
            t.jsonObject["year"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.let { year ->
                assertTrue("${t.jsonObject["title"]} is from $year", year <= 1991)
            }
        }
    }

    @Test
    fun everyChannelIsLiveAtAnyMoment() {
        val epoch = lineup.getValue("epochMs").jsonPrimitive.long
        channels.forEach { c ->
            val programs = c.getValue("items").jsonArray.map { t ->
                val o = t.jsonObject
                Program(o.getValue("id").jsonPrimitive.content, o.getValue("file").jsonPrimitive.content, "", null, null,
                    o.getValue("duration").jsonPrimitive.int * 1000L, 0)
            }
            val schedule = ChannelSchedule(listOf(ScheduleVersion(1, epoch, epoch, 0, programs)))
            for (t in listOf(epoch - 86_400_000L, epoch, epoch + 3_600_000L * 24 * 400)) {
                val airing = schedule.airingAt(t)
                assertTrue(airing.contains(t))
            }
        }
    }
}
