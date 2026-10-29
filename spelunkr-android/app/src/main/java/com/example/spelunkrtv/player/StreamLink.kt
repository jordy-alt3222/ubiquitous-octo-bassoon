package com.example.spelunkrtv.player

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.ClippingMediaSource
import com.example.spelunkrtv.diagnostics.httpReason
import com.example.spelunkrtv.diagnostics.networkReason
import java.util.concurrent.atomic.AtomicLong

/** What the app can see of its line to the Internet Archive while a tape plays. */
data class StreamLink(
    /** The Archive server actually sending the tape (after archive.org's redirect). */
    val host: String? = null,
    /** Received since tuning in. */
    val bytes: Long = 0,
    val bitsPerSecond: Long = 0,
    /** Data arrived in the last few seconds. */
    val receiving: Boolean = false,
    /** How long playback has been waiting for data; 0 while it plays. */
    val waitingMs: Long = 0,
    /** Video already downloaded ahead of the picture. */
    val bufferedMs: Long = 0,
    /** What the tape on air needs, from its own headers, once they have arrived. */
    val tapeBitsPerSecond: Long? = null,
    /** Why the last attempt failed, in plain words. */
    val error: String? = null,
    val retryInMs: Long = 0,
)

/** Counts every byte the player's network reads deliver; called on the loader threads. */
@OptIn(UnstableApi::class)
internal class ByteCounter : TransferListener {
    val bytes = AtomicLong()
    @Volatile var host: String? = null
    @Volatile var lastDataAt = 0L

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        // Once open, the source reports where archive.org's redirect actually led.
        if (isNetwork) host = (source.uri ?: dataSpec.uri).host
    }

    override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
        if (!isNetwork) return
        bytes.addAndGet(bytesTransferred.toLong())
        lastDataAt = SystemClock.elapsedRealtime()
    }

    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
}

/** Plain words for why a tape would not play. */
@OptIn(UnstableApi::class)
fun describe(error: PlaybackException): String {
    for (e in generateSequence<Throwable>(error) { it.cause }.take(12)) {
        if (e is HttpDataSource.InvalidResponseCodeException) return httpReason(e.responseCode, "archive.org")
        if (e is ClippingMediaSource.IllegalClippingException) return "This tape's file can't start part-way through"
    }
    networkReason(error, "archive.org")?.let { return it }
    return when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "No connection to archive.org"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "archive.org didn't answer in time"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "archive.org refused this tape"
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "archive.org sent the app to an insecure (http) address"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "This tape's file arrived but can't be read"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> "The tape arrived, but this device can't decode its video"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "The tape arrived, but this device's sound output failed"
        else -> "Playback failed (${error.errorCodeName})"
    }
}
