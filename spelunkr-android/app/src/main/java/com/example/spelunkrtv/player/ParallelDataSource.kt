package com.example.spelunkrtv.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * DOWNLOAD BOOST: reads one big request over several connections at once.
 *
 * Archive servers that are busy hand each connection only a trickle (one TOONERTIA server gave
 * 1.2 Mbit/s to one connection and 6.8 to eight), and long-distance lines cap a single
 * connection too. So the request is cut into 1 MB pieces, a few are fetched side by side, and
 * they are passed on strictly in order, starting with the first piece as it arrives. The bytes
 * are exactly those of a single download; only the wait changes.
 *
 * Small requests, and servers that don't say how big the file is, use one connection as before.
 */
@OptIn(UnstableApi::class)
class ParallelDataSource(
    private val upstream: DataSource.Factory,
    private val connections: () -> Int,
) : DataSource {

    private class Piece(val spec: DataSpec, val length: Int) {
        val data = ByteArray(length)
        val lock = ReentrantLock()
        val changed: Condition = lock.newCondition()
        @Volatile var filled = 0
        @Volatile var failure: IOException? = null
        @Volatile var cancelled = false

        fun add(n: Int) = lock.withLock { filled += n; changed.signalAll() }
        fun fail(e: IOException) = lock.withLock { failure = e; changed.signalAll() }
    }

    private val listeners = mutableListOf<TransferListener>()
    private var single: DataSource? = null
    private var request: DataSpec? = null
    private var uri: Uri? = null
    private var headers: Map<String, List<String>> = emptyMap()
    private val pieces = ArrayDeque<Piece>()
    private var width = 1
    private var nextStart = 0L
    private var end = 0L
    private var readInHead = 0
    private var remaining = 0L

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
    }

    private fun source(): DataSource = upstream.createDataSource().also { s -> listeners.forEach(s::addTransferListener) }

    override fun open(dataSpec: DataSpec): Long {
        width = connections().coerceIn(1, MAX_CONNECTIONS)
        val bounded = dataSpec.length != C.LENGTH_UNSET.toLong()
        if (width <= 1 || (bounded && dataSpec.length <= PIECE_BYTES * 2L)) {
            return openSingle(dataSpec)
        }
        // The first piece is opened right here: it says how big the file is, and where it lives.
        val firstLength = if (bounded) minOf(PIECE_BYTES.toLong(), dataSpec.length) else PIECE_BYTES.toLong()
        val first = source()
        val opened = first.open(dataSpec.subrange(0, firstLength))
        if (opened == C.LENGTH_UNSET.toLong()) {
            first.close()
            return openSingle(dataSpec)
        }
        uri = first.uri
        headers = first.responseHeaders
        end = if (bounded) {
            dataSpec.position + dataSpec.length
        } else {
            totalLength(headers) ?: run {
                // No size given (or the range was ignored): stream it on one connection after all.
                first.close()
                return openSingle(dataSpec)
            }
        }
        request = dataSpec
        remaining = end - dataSpec.position
        readInHead = 0
        pieces.clear()
        val head = Piece(dataSpec.subrange(0, opened), opened.toInt())
        pieces.addLast(head)
        nextStart = dataSpec.position + opened
        pool.execute { fill(head, first) }
        topUp()
        return remaining
    }

    private fun openSingle(dataSpec: DataSpec): Long {
        val s = source()
        single = s
        return s.open(dataSpec)
    }

    /** Keeps [width] pieces on the way, ahead of the reader. */
    private fun topUp() {
        val spec = request ?: return
        while (pieces.size < width && nextStart < end) {
            val length = minOf(PIECE_BYTES.toLong(), end - nextStart).toInt()
            val piece = Piece(spec.subrange(nextStart - spec.position, length.toLong()), length)
            pieces.addLast(piece)
            nextStart += length
            pool.execute { fill(piece, null) }
        }
    }

    /** Fetches one piece, picking up where it left off if the connection drops. */
    private fun fill(piece: Piece, opened: DataSource?) {
        var source = opened
        var attempts = 0
        while (!piece.cancelled && piece.filled < piece.length) {
            try {
                val s = source ?: source().also {
                    it.open(piece.spec.subrange(piece.filled.toLong(), (piece.length - piece.filled).toLong()))
                    source = it
                }
                val n = s.read(piece.data, piece.filled, piece.length - piece.filled)
                if (n == C.RESULT_END_OF_INPUT) throw DroppedConnectionException((piece.length - piece.filled).toLong())
                piece.add(n)
            } catch (e: IOException) {
                runCatching { source?.close() }
                source = null
                if (piece.cancelled) return
                if (++attempts > PIECE_RETRIES) {
                    piece.fail(e)
                    return
                }
                try {
                    Thread.sleep(attempts * RETRY_STEP_MS)
                } catch (_: InterruptedException) {
                    return
                }
            }
        }
        runCatching { source?.close() }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        single?.let { return it.read(buffer, offset, length) }
        if (length == 0) return 0
        if (remaining <= 0) return C.RESULT_END_OF_INPUT
        val head = pieces.first()
        val available = head.lock.withLock {
            while (head.filled <= readInHead && head.failure == null) {
                try {
                    head.changed.await(WAIT_SLICE_MS, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    // The player cancelled this load.
                    Thread.currentThread().interrupt()
                    throw InterruptedIOException()
                }
            }
            if (head.filled <= readInHead) throw head.failure!!
            head.filled - readInHead
        }
        val n = minOf(length, available)
        System.arraycopy(head.data, readInHead, buffer, offset, n)
        readInHead += n
        remaining -= n
        if (readInHead == head.length) {
            pieces.removeFirst()
            readInHead = 0
            topUp()
        }
        return n
    }

    override fun getUri(): Uri? = single?.uri ?: uri

    override fun getResponseHeaders(): Map<String, List<String>> = single?.responseHeaders ?: headers

    override fun close() {
        single?.let {
            single = null
            it.close()
            return
        }
        pieces.forEach { it.cancelled = true }
        pieces.clear()
        request = null
        uri = null
    }

    class Factory(private val upstream: DataSource.Factory, private val connections: () -> Int) : DataSource.Factory {
        override fun createDataSource(): DataSource = ParallelDataSource(upstream, connections)
    }

    companion object {
        const val PIECE_BYTES = 1024 * 1024
        const val MAX_CONNECTIONS = 8
        private const val PIECE_RETRIES = 4
        private const val RETRY_STEP_MS = 500L
        private const val WAIT_SLICE_MS = 250L
        private val pool = Executors.newCachedThreadPool { r -> Thread(r, "spelunkr-boost").apply { isDaemon = true } }

        /** The file size from "Content-Range: bytes 0-1048575/761397536", if the server gave it. */
        internal fun totalLength(headers: Map<String, List<String>>): Long? =
            headers.entries.firstOrNull { it.key.equals("Content-Range", ignoreCase = true) }
                ?.value?.firstOrNull()?.substringAfterLast('/')?.trim()?.toLongOrNull()
    }
}
