package com.example.spelunkrtv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.spelunkrtv.schedule.Airing
import com.example.spelunkrtv.schedule.ChannelSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays a channel "live": whatever the schedule says is on right now, from the right second.
 * Pause and the 15-second keys let the viewer fall behind live, like the site's DVR keys;
 * forward never goes past live. If buffering leaves playback well behind where the viewer
 * chose to be, it jumps back into sync.
 */
class LivePlayer(context: Context, private val clock: () -> Long) {

    data class State(
        val airing: Airing? = null,
        val paused: Boolean = false,
        val behindLiveMs: Long = 0,
        val tuning: Boolean = false,
        val noSignal: Boolean = false,
    ) {
        val live: Boolean get() = !paused && behindLiveMs < LIVE_SLACK_MS
    }

    val player: ExoPlayer = ExoPlayer.Builder(context)
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

    private val retry = Runnable { schedule?.let { loadAt(clock() - chosenDelayMs) } }

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                queueNext()
                publish()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> noSignal = false
                    // The queue ran dry (a tape shorter than its listing): pick the schedule up again.
                    Player.STATE_ENDED -> loadAt(clock() - chosenDelayMs)
                }
                publish()
            }

            override fun onPlayerError(error: PlaybackException) {
                noSignal = true
                publish()
                handler.removeCallbacks(retry)
                handler.postDelayed(retry, RETRY_MS)
            }
        })
    }

    fun tune(schedule: ChannelSchedule) {
        this.schedule = schedule
        chosenDelayMs = 0
        paused = false
        noSignal = false
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
        val airing = currentAiring()
        if (airing != null && !paused && player.playbackState == Player.STATE_READY) {
            val drift = clock() - programTime() - chosenDelayMs
            if (drift > RESYNC_MS || drift < -RESYNC_MS) seekToProgramTime(clock() - chosenDelayMs)
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
        player.setMediaItems(listOf(mediaItem(now), mediaItem(next)), 0, t - now.startMs)
        player.prepare()
        player.playWhenReady = !paused
        publish()
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

    private fun publish() {
        val airing = currentAiring()
        _state.value = State(
            airing = airing,
            paused = paused,
            behindLiveMs = if (airing == null) 0 else (clock() - programTime()).coerceAtLeast(0),
            tuning = schedule != null && !noSignal &&
                (player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE),
            noSignal = noSignal,
        )
    }

    companion object {
        const val SKIP_MS = 15_000L
        const val LIVE_SLACK_MS = 2_000L
        private const val RESYNC_MS = 8_000L
        private const val RETRY_MS = 5_000L
    }
}
