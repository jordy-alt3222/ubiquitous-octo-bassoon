package com.example.spelunkrtv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.spelunkrtv.schedule.Airing
import com.example.spelunkrtv.schedule.ChannelSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays a channel "live": whatever the schedule says is on right now, from the right second.
 * Pause and the 15-second keys let the viewer fall behind live, like the site's DVR keys;
 * forward never goes past live. If buffering leaves playback well behind where the viewer
 * chose to be, it jumps back into sync. Every tune and failure is logged under [TAG].
 */
@OptIn(UnstableApi::class)
class LivePlayer(context: Context, private val clock: () -> Long) {

    data class State(
        val airing: Airing? = null,
        val paused: Boolean = false,
        val behindLiveMs: Long = 0,
        val tuning: Boolean = false,
        val noSignal: Boolean = false,
        val link: StreamLink = StreamLink(),
    ) {
        val live: Boolean get() = !paused && behindLiveMs < LIVE_SLACK_MS
    }

    private val counter = ByteCounter()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context).setDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent(userAgent(context))
                    // The Archive can take a while to start a big tape; ExoPlayer's default is 8 s.
                    .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                    .setReadTimeoutMs(READ_TIMEOUT_MS)
                    .setTransferListener(counter),
            ),
        )
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val airings = HashMap<String, Airing>()
    private var schedule: ChannelSchedule? = null
    /** How far behind live the viewer chose to be (pause, rewind). */
    private var chosenDelayMs = 0L
    private var paused = false
    private var noSignal = false

    // What the link to the Archive is doing, for the screen and the signal check.
    private var bytesAtTune = 0L
    private var lastTickBytes = 0L
    private var lastTickAt = 0L
    private var bitsPerSecond = 0L
    /** elapsedRealtime when playback started waiting for data; 0 while it plays. */
    private var waitingSince = 0L
    private var lastError: String? = null
    private var failures = 0
    private var retryAt = 0L
    private var lastResyncAt = 0L

    private val retry = Runnable { schedule?.let { loadAt(clock() - chosenDelayMs) } }

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                queueNext()
                publish()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val now = SystemClock.elapsedRealtime()
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (waitingSince > 0) {
                            Log.i(TAG, "Playing after ${now - waitingSince} ms; ${counter.bytes.get() - bytesAtTune} bytes from ${counter.host}")
                        }
                        noSignal = false
                        lastError = null
                        failures = 0
                        waitingSince = 0
                    }
                    Player.STATE_BUFFERING -> if (waitingSince == 0L) waitingSince = now
                    Player.STATE_ENDED -> playNextTape()
                }
                publish()
            }

            override fun onPlayerError(error: PlaybackException) {
                noSignal = true
                lastError = describe(error)
                failures++
                val wait = minOf(RETRY_MAX_MS, RETRY_MS shl (failures - 1).coerceAtMost(4))
                retryAt = SystemClock.elapsedRealtime() + wait
                Log.w(TAG, "No signal (${error.errorCodeName}): $lastError. Trying again in ${wait / 1000} s", error)
                publish()
                handler.removeCallbacks(retry)
                handler.postDelayed(retry, wait)
            }
        })
    }

    fun tune(schedule: ChannelSchedule) {
        this.schedule = schedule
        chosenDelayMs = 0
        paused = false
        noSignal = false
        lastError = null
        failures = 0
        bytesAtTune = counter.bytes.get()
        counter.host = null
        handler.removeCallbacks(retry)
        loadAt(clock())
    }

    /** Back to live after the app was in the background. */
    fun resume() {
        schedule?.let { tune(it) }
    }

    fun stop() {
        handler.removeCallbacks(retry)
        player.stop()
        waitingSince = 0
        publish()
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        player.release()
    }

    var muted: Boolean
        get() = player.volume == 0f
        set(value) { player.volume = if (value) 0f else 1f }

    fun togglePause() {
        if (schedule == null) return
        if (paused) {
            chosenDelayMs = (clock() - programTime()).coerceAtLeast(0)
            paused = false
            player.play()
        } else {
            paused = true
            player.pause()
        }
        publish()
    }

    fun rewind15() {
        val airing = currentAiring() ?: return
        val target = (programTime() - SKIP_MS).coerceAtLeast(airing.startMs)
        seekToProgramTime(target)
        if (!paused) chosenDelayMs = clock() - target
        publish()
    }

    fun forward15() {
        if (currentAiring() == null) return
        val behind = clock() - programTime()
        if (behind < LIVE_SLACK_MS) return
        val target = programTime() + minOf(SKIP_MS, behind)
        seekToProgramTime(target)
        if (!paused) chosenDelayMs = (clock() - target).let { if (it < LIVE_SLACK_MS) 0 else it }
        publish()
    }

    /** Called about once a second while the app is in front. */
    fun tick() {
        val now = SystemClock.elapsedRealtime()
        val total = counter.bytes.get()
        if (lastTickAt > 0 && now > lastTickAt) {
            val instant = (total - lastTickBytes) * 8_000 / (now - lastTickAt)
            bitsPerSecond = (bitsPerSecond + instant) / 2
        }
        lastTickAt = now
        lastTickBytes = total

        // Catch up after a stall, but not over and over: on a slow line every catch-up costs
        // another wait, and chasing live in a loop would never show a picture.
        val airing = currentAiring()
        if (airing != null && !paused && player.playbackState == Player.STATE_READY && now - lastResyncAt > RESYNC_COOLDOWN_MS) {
            val drift = clock() - programTime() - chosenDelayMs
            if (drift > RESYNC_MS) {
                Log.i(TAG, "${drift / 1000} s behind live after a stall; catching up")
                lastResyncAt = now
                seekToProgramTime(clock() - chosenDelayMs)
            }
        }
        publish()
    }

    private fun currentAiring(): Airing? = player.currentMediaItem?.mediaId?.let { airings[it] }

    private fun programTime(): Long {
        val airing = currentAiring() ?: return clock()
        return airing.startMs + player.currentPosition
    }

    private fun seekToProgramTime(t: Long) {
        val airing = currentAiring()
        if (airing != null && airing.contains(t)) player.seekTo(t - airing.startMs) else loadAt(t)
    }

    private fun loadAt(t: Long) {
        val schedule = schedule ?: return
        val now = schedule.airingAt(t)
        val next = schedule.airingAt(now.endMs)
        airings.clear()
        Log.i(TAG, "Tuning \"${now.program.title}\" at ${(t - now.startMs) / 1000} s from ${now.program.videoUrl}")
        if (waitingSince == 0L) waitingSince = SystemClock.elapsedRealtime()
        player.setMediaItems(listOf(mediaItem(now), mediaItem(next)), 0, t - now.startMs)
        player.prepare()
        player.playWhenReady = !paused
        publish()
    }

    /**
     * The queue ran dry, which happens when a tape is shorter than its listing. Go on to the
     * next tape rather than reloading the one that just ran out, which would end again at once.
     */
    private fun playNextTape() {
        val t = clock() - chosenDelayMs
        val ended = currentAiring()
        Log.i(TAG, "\"${ended?.program?.title}\" ran out before its listed end; moving on")
        loadAt(if (ended != null && ended.contains(t)) ended.endMs else t)
    }

    /** Keep exactly one tape queued after the one playing, and drop the ones already played. */
    private fun queueNext() {
        val schedule = schedule ?: return
        val index = player.currentMediaItemIndex
        if (index > 0) player.removeMediaItems(0, index)
        if (player.mediaItemCount == 1) {
            val current = currentAiring() ?: return
            player.addMediaItem(mediaItem(schedule.airingAt(current.endMs)))
        }
    }

    private fun mediaItem(airing: Airing): MediaItem {
        airings[airing.key] = airing
        val inPoint = airing.program.inPointMs
        return MediaItem.Builder()
            .setMediaId(airing.key)
            .setUri(airing.program.videoUrl)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(inPoint)
                    .setEndPositionMs(inPoint + airing.durationMs)
                    .build(),
            )
            .build()
    }

    /** The bitrate the tape on air asks for, from its own headers. */
    private fun tapeBitrate(): Long? {
        val formats = player.currentTracks.groups.flatMap { group ->
            (0 until group.length).filter { group.isTrackSelected(it) }.map { group.getTrackFormat(it) }
        }.ifEmpty { player.currentTracks.groups.filter { it.length > 0 }.map { it.getTrackFormat(0) } }
        return formats.map { it.bitrate.toLong() }.filter { it > 0 }.sum().takeIf { it > 0 }
    }

    private fun publish() {
        val airing = currentAiring()
        val now = SystemClock.elapsedRealtime()
        _state.value = State(
            airing = airing,
            paused = paused,
            behindLiveMs = if (airing == null) 0 else (clock() - programTime()).coerceAtLeast(0),
            tuning = schedule != null && !noSignal &&
                (player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE),
            noSignal = noSignal,
            link = StreamLink(
                host = counter.host,
                bytes = counter.bytes.get() - bytesAtTune,
                bitsPerSecond = bitsPerSecond,
                receiving = counter.lastDataAt > 0 && now - counter.lastDataAt < RECEIVING_MS,
                waitingMs = if (waitingSince > 0) now - waitingSince else 0,
                bufferedMs = player.totalBufferedDuration,
                tapeBitsPerSecond = tapeBitrate(),
                error = lastError,
                retryInMs = if (noSignal) (retryAt - now).coerceAtLeast(0) else 0,
            ),
        )
    }

    companion object {
        const val TAG = "Spelunkr"
        const val SKIP_MS = 15_000L
        const val LIVE_SLACK_MS = 2_000L
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 20_000
        private const val RESYNC_MS = 8_000L
        private const val RESYNC_COOLDOWN_MS = 60_000L
        private const val RETRY_MS = 5_000L
        private const val RETRY_MAX_MS = 60_000L
        private const val RECEIVING_MS = 3_000L

        /** Says who is asking, so the Archive can tell this app's traffic apart. */
        fun userAgent(context: Context): String {
            val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
            return "SPELUNKR-Android/${version ?: "?"} (unofficial; Media3)"
        }
    }
}
