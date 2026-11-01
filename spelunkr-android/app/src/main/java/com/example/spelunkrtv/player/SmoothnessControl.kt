package com.example.spelunkrtv.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.example.spelunkrtv.data.TvSetup.Smoothness

/**
 * TV SETUP's SMOOTHNESS, switchable while a tape plays. Three buffering profiles share one
 * memory pool; the one chosen decides when to start, how much to bank, and how much to refill
 * after a stall. None of them changes what is played, only when.
 */
@OptIn(UnstableApi::class)
class SmoothnessControl : LoadControl {
    private val allocator = DefaultAllocator(/* trimOnReset = */ true, C.DEFAULT_BUFFER_SEGMENT_SIZE)

    private val profiles: Map<Smoothness, DefaultLoadControl> = mapOf(
        // Starts after one second; banks a minute.
        Smoothness.START_FAST to profile(minMs = 30_000, maxMs = 60_000, startMs = 1_000, restartMs = 5_000),
        // Starts after 2.5 s; banks two minutes; refills ten seconds after a stall.
        Smoothness.BALANCED to profile(minMs = 60_000, maxMs = 120_000, startMs = 2_500, restartMs = 10_000),
        // Starts after five seconds; banks up to five minutes; refills half a minute after a stall,
        // so a line slower than the tape stops rarely, for longer.
        Smoothness.SMOOTHEST to profile(minMs = 120_000, maxMs = 300_000, startMs = 5_000, restartMs = 30_000),
    )

    @Volatile var smoothness: Smoothness = Smoothness.BALANCED

    private val active: DefaultLoadControl get() = profiles.getValue(smoothness)

    private fun profile(minMs: Int, maxMs: Int, startMs: Int, restartMs: Int) =
        DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(minMs, maxMs, startMs, restartMs)
            .build()

    // Every profile follows the player's life, so any of them can take over at once.
    override fun onPrepared(playerId: PlayerId) = profiles.values.forEach { it.onPrepared(playerId) }

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection?>,
    ) = profiles.values.forEach { it.onTracksSelected(parameters, trackGroups, trackSelections) }

    override fun onStopped(playerId: PlayerId) = profiles.values.forEach { it.onStopped(playerId) }

    override fun onReleased(playerId: PlayerId) = profiles.values.forEach { it.onReleased(playerId) }

    override fun getAllocator(playerId: PlayerId): Allocator = allocator

    override fun getBackBufferDurationUs(playerId: PlayerId): Long = active.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean = active.retainBackBufferFromKeyframe(playerId)

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean = active.shouldContinueLoading(parameters)

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long,
    ): Boolean = active.shouldContinuePreloading(playerId, timeline, mediaPeriodId, bufferedDurationUs)

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean = active.shouldStartPlayback(parameters)
}
