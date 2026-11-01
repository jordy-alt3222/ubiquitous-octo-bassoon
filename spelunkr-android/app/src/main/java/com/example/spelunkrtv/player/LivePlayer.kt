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
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.spelunkrtv.data.TvSetup
import com.example.spelunkrtv.schedule.Airing
import com.example.spelunkrtv.schedule.ChannelSchedule
import com.example.spelunkrtv.schedule.Program
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Plays a channel "live": whatever the schedule says is on right now, from the right second.
 * Pause and the 15-second keys let the viewer fall behind live, like the site's DVR keys;
 * forward never goes past live.
 *
 * Built for the Archive's big, slow tapes: tape indexes are kept on disk ([TapeCache]) and
 * fetched ahead ([TapeWarmer]); requests go straight to the storage server ([ArchiveLocator]);
 * a tuned tape jumps to live once, as soon as its index has arrived; and a short stall later on
 * leaves the viewer a few seconds behind rather than stalling again to catch up. Every tune and
 * failure is logged under [TAG].
 */
@OptIn(UnstableApi::class)
class LivePlayer(private val context: Context, private val clock: () -> Long) {

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
    private val locator = ArchiveLocator(userAgent(context))
    /** TV SETUP's DOWNLOAD BOOST: how many connections one big read may use. */
    @Volatile private var connections = TvSetup().connections
    private val smoothness = SmoothnessControl()
    private val trackSelector = DefaultTrackSelector(context)

    /**
     * archive.org, straight to the tape's faster storage server once it is known, complete
     * responses only, and big reads spread over several connections.
     */
    private fun network(listener: ByteCounter?) = ParallelDataSource.Factory(
        ResolvingDataSource.Factory(
            WholeResponses.Factory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent(userAgent(context))
                    // The Archive can take a while to start a big tape; ExoPlayer's default is 8 s.
                    .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                    .setReadTimeoutMs(READ_TIMEOUT_MS)
                    .setTransferListener(listener),
            ),
        ) { spec -> spec.withUri(locator.resolve(spec.uri)) },
    ) { connections }

    private val warmer = TapeWarmer(context, network(listener = null))

    val player: ExoPlayer = ExoPlayer.Builder(
        context,
        // If the TV's first-choice decoder won't start, try its others before giving up.
        DefaultRenderersFactory(context).setEnableDecoderFallback(true),
    )
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context).setDataSourceFactory(TapeCache.playerDataSource(context, network(counter))),
        )
        .setTrackSelector(trackSelector)
        .setLoadControl(smoothness)
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
    /** How far behind live stalls have left playback; caught up only once it passes a minute. */
    private var stallDelayMs = 0L
    private var paused = false
    private var noSignal = false
    /**
     * A tape was just loaded for programme time [loadTarget]: as soon as its index is in, and
     * before anything shows, move on by however long that took (none while paused).
     */
    private var syncOnIndex = false
    private var loadTarget = 0L
    private var loadAskedAt = 0L

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
    private var lastCatchUpAt = 0L
    private var lastSwitchAt = 0L
    private var bytesAtLoad = 0L
    /** When this load's first byte arrived; its speed is judged from then, not from the lookup. */
    private var firstByteAt = 0L
    /** This load has shown a picture: from then on a stall is the buffer's business, not a reason to switch servers. */
    private var loadPlayed = false
    private var indexCached = false

    private val retry = Runnable { schedule?.let { loadAt(clock() - chosenDelayMs - stallDelayMs) } }

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                queueNext()
                publish()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (syncOnIndex) syncToLive(timeline)
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
                        loadPlayed = true
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
                // Next time, try the tape's other server (or archive.org) instead of this one.
                locator.avoid(counter.host)
                // A file that ended early can't be trusted on disk either: fetch its index afresh.
                if (generateSequence<Throwable>(error) { it.cause }.any { it is java.io.EOFException }) {
                    (currentAiring() ?: schedule?.airingAt(loadTarget))?.let { TapeCache.forget(context, it.program) }
                }
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
        stallDelayMs = 0
        paused = false
        noSignal = false
        lastError = null
        failures = 0
        bytesAtTune = counter.bytes.get()
        counter.host = null
        handler.removeCallbacks(retry)
        // The tape being tuned needs the whole line.
        warmer.stop()
        loadAt(clock())
    }

    /** Back to live after the app was in the background. */
    fun resume() {
        schedule?.let { tune(it) }
    }

    fun stop() {
        handler.removeCallbacks(retry)
        warmer.stop()
        player.stop()
        waitingSince = 0
        publish()
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        warmer.stop()
        player.release()
    }

    var muted: Boolean
        get() = player.volume == 0f
        set(value) { player.volume = if (value) 0f else 1f }

    /**
     * Fetches these tapes' indexes in the background, most wanted first, but only while the tape
     * on screen is playing with plenty in hand; otherwise warming would starve it.
     */
    fun warm(programs: List<Program>) {
        if (paused || noSignal || player.playbackState != Player.STATE_READY || player.totalBufferedDuration < WARM_WHEN_BUFFERED_MS) {
            // Never compete with a tape that is struggling. Whatever was fetched stays on disk,
            // and warming picks up from there next time.
            warmer.stop()
            return
        }
        // Where they live is enough for preloading; servers are timed only for a tape about to play.
        programs.forEach { locator.lookUp(it.videoUrl, time = false) }
        warmer.warm(programs)
    }

    fun togglePause() {
        if (schedule == null) return
        if (paused) {
            chosenDelayMs = (clock() - programTime()).coerceAtLeast(0)
            stallDelayMs = 0
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
        if (!paused) {
            chosenDelayMs = clock() - target
            stallDelayMs = 0
        }
        publish()
    }

    fun forward15() {
        if (currentAiring() == null) return
        val behind = clock() - programTime()
        if (behind < LIVE_SLACK_MS) return
        val target = programTime() + minOf(SKIP_MS, behind)
        seekToProgramTime(target)
        if (!paused) {
            chosenDelayMs = (clock() - target).let { if (it < LIVE_SLACK_MS) 0 else it }
            stallDelayMs = 0
        }
        publish()
    }

    /** Both storage servers for the tape at [videoUrl], with their measured speed. Blocking. */
    fun servers(videoUrl: String): List<ArchiveLocator.Server> = locator.servers(videoUrl)

    /** TV SETUP, applied at once; nothing here changes the picture or sound the tape carries. */
    fun apply(setup: TvSetup) {
        connections = setup.connections
        smoothness.smoothness = setup.smoothness
        locator.chooseFastest = setup.server == TvSetup.Server.FASTEST
        player.videoChangeFrameRateStrategy =
            if (setup.matchFrameRate) C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS else C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
        trackSelector.parameters = trackSelector.buildUponParameters()
            // Tunneling hands timing to the TV's video hardware: smoother on many sets, so it's offered.
            .setTunnelingEnabled(setup.tunneling)
            .apply {
                when (setup.sound) {
                    // Dolby (AC-3) when a tape has it and the TV or soundbar takes it; the original otherwise.
                    TvSetup.Sound.SURROUND -> setPreferredAudioMimeTypes(MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_AC3)
                        .setMaxAudioChannelCount(Int.MAX_VALUE)
                    TvSetup.Sound.STEREO -> setPreferredAudioMimeTypes(MimeTypes.AUDIO_AAC).setMaxAudioChannelCount(2)
                }
            }
            .build()
        TapeCache.setLimit(context, if (setup.preload == TvSetup.Preload.WHOLE_DIAL) TapeCache.WHOLE_DIAL_LIMIT else TapeCache.NORMAL_LIMIT)
    }

    /** JUMP TO LIVE: back to the second everyone else is watching. */
    fun goLive() {
        if (schedule == null) return
        chosenDelayMs = 0
        stallDelayMs = 0
        if (paused) {
            paused = false
            player.play()
        }
        seekToProgramTime(clock())
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

        // A tune stuck on a crawling server while its mirror is free: switch now. What has
        // arrived of the index is already on disk, so the other server picks up from there.
        val tape = currentAiring() ?: schedule?.airingAt(loadTarget)
        val waited = if (waitingSince > 0) now - waitingSince else 0
        val loaded = total - bytesAtLoad
        if (loaded > 0 && firstByteAt == 0L) firstByteAt = now
        val loadSpeed = if (firstByteAt > 0 && now > firstByteAt) loaded * 8_000 / (now - firstByteAt) else 0
        // Nothing at all for a while, or the other server was measured at least twice as fast.
        val stuck = loaded == 0L && waited > SWITCH_AFTER_MS
        val crawling = firstByteAt > 0 && now - firstByteAt > SWITCH_AFTER_MS && loadSpeed < SLOW_SERVER_BPS
        if (tape != null && !paused && !loadPlayed && player.playbackState == Player.STATE_BUFFERING && now - lastSwitchAt > SWITCH_AFTER_MS &&
            counter.host != null && (stuck || crawling) &&
            locator.hasFasterThan(tape.program.videoUrl, counter.host, if (stuck) 0 else loadSpeed * 2)
        ) {
            Log.i(TAG, "${counter.host} is crawling (${loadSpeed / 1000} kbit/s); switching to its mirror")
            lastSwitchAt = now
            locator.avoid(counter.host)
            loadAt(loadTarget + (clock() - loadAskedAt))
        }

        if (currentAiring() != null && !paused && player.playbackState == Player.STATE_READY) {
            // A stall puts playback behind. Jumping straight back would mean another stall, so a
            // short one is simply kept (the badge shows how far behind); only once stalls add up
            // to a minute does it catch up, and then at most once a minute.
            val drift = clock() - programTime() - chosenDelayMs - stallDelayMs
            if (drift > STALL_NOTICE_MS) stallDelayMs += drift
            if (stallDelayMs > CATCH_UP_AFTER_MS && now - lastCatchUpAt > CATCH_UP_AFTER_MS) {
                Log.i(TAG, "${stallDelayMs / 1000} s behind after stalls; catching up")
                lastCatchUpAt = now
                stallDelayMs = 0
                seekToProgramTime(clock() - chosenDelayMs)
            }
        }
        publish()
    }

    /** The tape's index has arrived and it can seek: move to live now, while nothing is showing yet. */
    private fun syncToLive(timeline: Timeline) {
        if (timeline.isEmpty || player.currentMediaItemIndex >= timeline.windowCount) return
        val window = timeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
        if (window.isPlaceholder || !window.isSeekable) return
        syncOnIndex = false
        val airing = currentAiring() ?: return
        // Aim a little ahead: the first seconds of video still have to arrive after the seek.
        val target = loadTarget + if (paused) 0L else clock() - loadAskedAt + SYNC_LEAD_MS
        if (!airing.contains(target)) return
        val position = target - airing.startMs
        if (abs(position - player.currentPosition) > SYNC_MIN_MS) {
            Log.i(TAG, "Index in; moving ${(position - player.currentPosition) / 1000} s on to live")
            player.seekTo(position)
        }
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
        // Find its storage servers while the index loads, so the seek to live goes direct. The
        // next tape's are looked up later, by warming, so the two don't share the line now.
        locator.lookUp(now.program.videoUrl)
        indexCached = TapeCache.isIndexCached(context, now.program)
        Log.i(TAG, "Tuning \"${now.program.title}\" at ${(t - now.startMs) / 1000} s from ${now.program.videoUrl}" +
            (if (indexCached) " (index on disk)" else now.program.indexBytes?.let { " (index ${it / 1000} KB to fetch)" } ?: ""))
        waitingSince = SystemClock.elapsedRealtime()
        bytesAtLoad = counter.bytes.get()
        firstByteAt = 0
        loadPlayed = false
        syncOnIndex = true
        loadTarget = t
        loadAskedAt = clock()
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
        val t = clock() - chosenDelayMs - stallDelayMs
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
        // Lets the cache keep exactly this tape's index, and nothing after it.
        TapeCache.note(airing.program)
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
                indexBytes = (airing ?: schedule?.airingAt(clock()))?.program?.indexBytes,
                indexCached = indexCached,
            ),
        )
    }

    companion object {
        const val TAG = "Spelunkr"
        const val SKIP_MS = 15_000L
        const val LIVE_SLACK_MS = 2_000L
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 20_000
        private const val WARM_WHEN_BUFFERED_MS = 20_000L
        private const val SYNC_LEAD_MS = 1_500L
        private const val SYNC_MIN_MS = 1_500L
        private const val STALL_NOTICE_MS = 1_000L
        private const val CATCH_UP_AFTER_MS = 60_000L
        private const val RETRY_MS = 5_000L
        private const val RETRY_MAX_MS = 60_000L
        private const val RECEIVING_MS = 3_000L
        /** A tune still waiting after this long, below [SLOW_SERVER_BPS], tries the other server. */
        private const val SWITCH_AFTER_MS = 12_000L
        private const val SLOW_SERVER_BPS = 1_500_000L

        /** Says who is asking, so the Archive can tell this app's traffic apart. */
        fun userAgent(context: Context): String {
            val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
            return "SPELUNKR-Android/${version ?: "?"} (unofficial; Media3)"
        }
    }
}
