package com.example.spelunkrtv.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParallelDataSourceTest {
    private val file = Random(7).nextBytes(5 * 1024 * 1024 + 321_123)
    private val uri = Uri.parse("https://archive.org/download/item/tape.mp4")

    /** A pretend Archive server: answers ranges, in uneven reads, and can drop connections. */
    private class FakeServer(val file: ByteArray, val dropEvery: Int = 0, val saysSize: Boolean = true) : DataSource.Factory {
        val opens = AtomicInteger()
        private val reads = AtomicInteger()

        override fun createDataSource(): DataSource = object : DataSource {
            private var position = 0
            private var end = 0
            private var headers: Map<String, List<String>> = emptyMap()
            private val random = Random(opens.get())

            override fun addTransferListener(transferListener: TransferListener) = Unit
            override fun open(dataSpec: DataSpec): Long {
                opens.incrementAndGet()
                position = dataSpec.position.toInt()
                end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) file.size else (dataSpec.position + dataSpec.length).toInt()
                headers = if (saysSize) mapOf("Content-Range" to listOf("bytes $position-${end - 1}/${file.size}")) else emptyMap()
                return (end - position).toLong()
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (position >= end) return C.RESULT_END_OF_INPUT
                if (dropEvery > 0 && reads.incrementAndGet() % dropEvery == 0) throw IOException("dropped")
                val n = minOf(length, end - position, 1 + random.nextInt(48 * 1024))
                System.arraycopy(file, position, buffer, offset, n)
                position += n
                return n
            }
            override fun getUri(): Uri? = null
            override fun getResponseHeaders(): Map<String, List<String>> = headers
            override fun close() = Unit
        }
    }

    private fun readAll(source: DataSource, spec: DataSpec): ByteArray {
        val expected = source.open(spec)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(70_000)
        val random = Random(3)
        while (true) {
            val n = source.read(buffer, 0, 1 + random.nextInt(buffer.size))
            if (n == C.RESULT_END_OF_INPUT) break
            out.write(buffer, 0, n)
        }
        source.close()
        assertEquals(expected, out.size().toLong())
        return out.toByteArray()
    }

    @Test
    fun piecesComeOutExactlyAsOneDownload() {
        val server = FakeServer(file)
        val got = readAll(ParallelDataSource(server) { 4 }, DataSpec(uri, 123_457, C.LENGTH_UNSET.toLong()))
        assertArrayEquals(file.copyOfRange(123_457, file.size), got)
        assertTrue("used several connections", server.opens.get() > 4)
    }

    @Test
    fun boundedRangesToo() {
        val got = readAll(ParallelDataSource(FakeServer(file)) { 6 }, DataSpec(uri, 1_000, 3_500_000))
        assertArrayEquals(file.copyOfRange(1_000, 3_501_000), got)
    }

    @Test
    fun droppedConnectionsPickUpWhereTheyLeftOff() {
        val got = readAll(ParallelDataSource(FakeServer(file, dropEvery = 37)) { 4 }, DataSpec(uri, 0, C.LENGTH_UNSET.toLong()))
        assertArrayEquals(file, got)
    }

    @Test
    fun smallReadsUseOneConnection() {
        val server = FakeServer(file)
        val got = readAll(ParallelDataSource(server) { 4 }, DataSpec(uri, 4_096, 16))
        assertArrayEquals(file.copyOfRange(4_096, 4_112), got)
        assertEquals(1, server.opens.get())
    }

    @Test
    fun aServerThatWontSayTheSizeIsReadAsOneStream() {
        val got = readAll(ParallelDataSource(FakeServer(file, saysSize = false)) { 4 }, DataSpec(uri, 77, C.LENGTH_UNSET.toLong()))
        assertArrayEquals(file.copyOfRange(77, file.size), got)
    }

    @Test
    fun boostOffIsOneConnection() {
        val server = FakeServer(file)
        val got = readAll(ParallelDataSource(server) { 1 }, DataSpec(uri, 0, C.LENGTH_UNSET.toLong()))
        assertArrayEquals(file, got)
        assertEquals(1, server.opens.get())
    }

    @Test
    fun readsTheFileSizeFromContentRange() {
        assertEquals(761_397_536L, ParallelDataSource.totalLength(mapOf("content-range" to listOf("bytes 0-1048575/761397536"))))
        assertEquals(null, ParallelDataSource.totalLength(mapOf("Content-Range" to listOf("bytes 0-1048575/*"))))
    }
}
