package com.example.spelunkrtv.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/** archive.org closed a connection before sending all it said it would. */
class DroppedConnectionException(missing: Long) : IOException("The server closed the connection with $missing bytes still to come")

/**
 * Turns a response that ends before its stated length into an error instead of the end of the
 * file. Slow Archive servers do drop connections part-way; read as "end of file", that makes
 * the player give up on the tape (and the cache remember a file that short). As an error,
 * ExoPlayer simply reconnects and carries on from the same byte.
 */
@OptIn(UnstableApi::class)
class WholeResponses(private val upstream: DataSource) : DataSource {
    private var remaining = C.LENGTH_UNSET.toLong()

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long = upstream.open(dataSpec).also { remaining = it }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val n = upstream.read(buffer, offset, length)
        if (n == C.RESULT_END_OF_INPUT) {
            if (remaining > 0) throw DroppedConnectionException(remaining)
        } else if (remaining != C.LENGTH_UNSET.toLong()) {
            remaining -= n
        }
        return n
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() = upstream.close()

    class Factory(private val upstream: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource = WholeResponses(upstream.createDataSource())
    }
}
