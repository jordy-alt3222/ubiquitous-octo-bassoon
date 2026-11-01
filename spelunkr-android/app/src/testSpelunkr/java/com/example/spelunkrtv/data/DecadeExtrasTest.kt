package com.example.spelunkrtv.data

import com.example.spelunkrtv.schedule.Dial
import com.example.spelunkrtv.schedule.Program
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** SPELUNKR's bundled decade top-ups must sit in the decades they fill, and play. */
class DecadeExtrasTest {
    private val decades = Json.parseToJsonElement(File("src/spelunkr/assets/decade-extras.json").readText())
        .jsonObject.getValue("decades").jsonObject

    @Test
    fun everyTapeIsFromTheDecadeItFills() {
        decades.forEach { (decade, tapes) ->
            tapes.jsonArray.map { it.jsonObject }.forEach { t ->
                val p = Program(t.getValue("id").jsonPrimitive.content, t.getValue("file").jsonPrimitive.content,
                    t.getValue("title").jsonPrimitive.content, t["year"]?.jsonPrimitive?.contentOrNull, null, 1, 0)
                assertEquals("${p.title} (${p.year})", decade.toInt(), Dial.decadeOf(p))
            }
        }
    }

    @Test
    fun onlyTheThinOldDecadesAreFilledAndEveryTapePlays() {
        assertTrue(decades.keys.all { it.toInt() in 1900..1970 })
        val tapes = decades.values.flatMap { it.jsonArray.map { t -> t.jsonObject } }
        assertTrue(tapes.size >= 100)
        assertEquals(tapes.size, tapes.map { it.getValue("id").jsonPrimitive.content }.toSet().size)
        tapes.forEach {
            assertTrue(it.getValue("file").jsonPrimitive.content.endsWith(".mp4", ignoreCase = true))
            assertTrue(it.getValue("duration").jsonPrimitive.int in 60..3 * 3600)
        }
    }
}
