package com.example.spelunkrtv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spelunkrtv.data.Channel
import com.example.spelunkrtv.data.SetupStore
import com.example.spelunkrtv.data.TvSetup
import com.example.spelunkrtv.player.TapeCache
import com.example.spelunkrtv.diagnostics.CheckRow
import com.example.spelunkrtv.diagnostics.SignalCheck
import com.example.spelunkrtv.diagnostics.TapeUnderTest
import com.example.spelunkrtv.player.LivePlayer
import com.example.spelunkrtv.schedule.ChannelSchedule
import com.example.spelunkrtv.schedule.Dial
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class Screen { Home, Guide, Watch, Directory, ChannelPage, Signal, Setup, MyDial }

sealed interface Lineup {
    data object Loading : Lineup
    data class Failed(val message: String) : Lineup
    data class Ready(val channels: List<Channel>) : Lineup
}

class SpelunkrViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = com.example.spelunkrtv.data.Brand.broadcast(app)
    val player = LivePlayer(app, repo::now)

    private val setupStore = SetupStore(app)
    /** TV SETUP: how tapes are fetched and shown, never their quality. */
    val setup: StateFlow<TvSetup> = setupStore.setup

    /** Room taken by saved tape indexes, for TV SETUP. */
    private val _savedIndexBytes = MutableStateFlow(0L)
    val savedIndexBytes: StateFlow<Long> = _savedIndexBytes.asStateFlow()

    /** The channels and schedules as the lineup gives them. */
    private val _source = MutableStateFlow<Lineup>(Lineup.Loading)
    private val _sourceSchedules = MutableStateFlow<Map<String, ChannelSchedule>>(emptyMap())
    val sourceChannels: List<Channel> get() = (_source.value as? Lineup.Ready)?.channels.orEmpty()

    /** The dial: the lineup as TV SETUP shapes it (decades, MY DIAL, decade channels, MY MIX). */
    private val _lineup = MutableStateFlow<Lineup>(Lineup.Loading)
    val lineup: StateFlow<Lineup> = _lineup.asStateFlow()

    /** Schedules arrive one channel at a time; the guide shows "ADJUSTING THE ANTENNA…" until then. */
    private val _schedules = MutableStateFlow<Map<String, ChannelSchedule>>(emptyMap())
    val schedules: StateFlow<Map<String, ChannelSchedule>> = _schedules.asStateFlow()

    /** Decades the lineup has tapes from, with how many, for WATCH BY DECADE. */
    private val _decades = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val decades: StateFlow<Map<Int, Int>> = _decades.asStateFlow()

    private val _now = MutableStateFlow(repo.now())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val backStack = ArrayDeque(listOf(Screen.Home))
    private val _screen = MutableStateFlow(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _tuned = MutableStateFlow<Channel?>(null)
    val tuned: StateFlow<Channel?> = _tuned.asStateFlow()
    private var lastChannel: Channel? = null

    /** Bumped on every tune, so the screen can show the channel banner again. */
    private val _tuneCount = MutableStateFlow(0)
    val tuneCount: StateFlow<Int> = _tuneCount.asStateFlow()

    /** The channel shown on the channel page. */
    private val _pageChannel = MutableStateFlow<Channel?>(null)
    val pageChannel: StateFlow<Channel?> = _pageChannel.asStateFlow()

    /** Digits typed on the remote, before they tune. */
    private val _dial = MutableStateFlow("")
    val dial: StateFlow<String> = _dial.asStateFlow()
    private var dialJob: Job? = null

    private val _stillWatching = MutableStateFlow(false)
    val stillWatching: StateFlow<Boolean> = _stillWatching.asStateFlow()
    private var lastInputAt = System.currentTimeMillis()

    private var tuneJob: Job? = null
    private var previewJob: Job? = null

    private val signalCheck = SignalCheck(app)
    private val _signal = MutableStateFlow<List<CheckRow>>(emptyList())
    /** The latest SIGNAL CHECK, row by row as each link is tested. */
    val signal: StateFlow<List<CheckRow>> = _signal.asStateFlow()
    private var signalJob: Job? = null

    init {
        player.apply(setup.value)
        load()
        viewModelScope.launch {
            while (isActive) {
                _now.value = repo.now()
                player.tick()
                warmAhead()
                checkAsleep()
                delay(1_000)
            }
        }
    }

    fun load() {
        _source.value = Lineup.Loading
        _lineup.value = Lineup.Loading
        viewModelScope.launch {
            val channels = try {
                repo.channels(withPreviews = setup.value.previewChannels)
            } catch (e: Exception) {
                _source.value = Lineup.Failed(e.message ?: "unknown error")
                _lineup.value = _source.value
                return@launch
            }
            _source.value = Lineup.Ready(channels)
            rebuildDial()
            // The home screen's little TV plays a random channel, sound off, like the site's.
            if (_tuned.value == null && this@SpelunkrViewModel.channels.isNotEmpty()) {
                player.muted = _screen.value == Screen.Home
                tune(this@SpelunkrViewModel.channels.random())
            }
            loadSources(channels)
        }
    }

    private fun loadSources(channels: List<Channel>) {
        viewModelScope.launch {
            val gate = Semaphore(6)
            channels.map { channel -> launch { gate.withPermit { loadSource(channel) } } }.joinAll()
        }
    }

    /** One lineup channel's schedule, fetched once; the dial is rebuilt as each arrives. */
    private suspend fun loadSource(channel: Channel): ChannelSchedule? {
        _sourceSchedules.value[channel.id]?.let { return it }
        return try {
            repo.schedule(channel).also { s ->
                _sourceSchedules.update { it + (channel.id to s) }
                rebuildDial()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** The schedule a dial channel plays, waiting for the lineup schedules it is built from. */
    private suspend fun scheduleFor(channel: Channel): ChannelSchedule? {
        _schedules.value[channel.id]?.let { return it }
        val source = sourceChannels.firstOrNull { it.id == channel.id }
        if (source != null) {
            loadSource(source)
        } else {
            // A decade channel or MY MIX: built from many channels at once.
            coroutineScope { sourceChannels.map { async { loadSource(it) } }.awaitAll() }
            rebuildDial()
        }
        return _schedules.value[channel.id]
    }

    /**
     * Shapes the dial from the lineup and TV SETUP. Lineup channels are cut down to the chosen
     * decades (and dropped if that leaves nothing) or taken off by MY DIAL; decade channels and
     * MY MIX are added once every schedule they draw on has arrived.
     */
    private fun rebuildDial() {
        val sources = (_source.value as? Lineup.Ready)?.channels ?: return
        val setup = setup.value
        val loaded = _sourceSchedules.value
        val now = repo.now()
        val tapes = sources.associate { c -> c.id to loaded[c.id]?.programsAt(now).orEmpty() }
        val allLoaded = sources.all { it.id in loaded }

        val dial = ArrayList<Channel>()
        val schedules = HashMap<String, ChannelSchedule>()
        for (c in sources) {
            if (c.id in setup.hidden) continue
            val schedule = loaded[c.id]
            when {
                setup.decades.isEmpty() -> {
                    dial += c
                    schedule?.let { schedules[c.id] = it }
                }
                schedule == null -> dial += c // still arriving: kept until we know
                else -> Dial.filtered(tapes.getValue(c.id), setup.decades)?.let { s ->
                    val kept = s.programsAt(now)
                    dial += c.copy(items = kept.size, videoSeconds = kept.sumOf { it.durationMs } / 1000)
                    schedules[c.id] = s
                }
            }
        }
        val counts = tapes.values.flatten().distinctBy { it.videoUrl }.mapNotNull { Dial.decadeOf(it) }
            .groupingBy { it }.eachCount().filterValues { it >= MIN_DECADE_TAPES }.toSortedMap()
        _decades.value = counts
        if (allLoaded && setup.decadeChannels) {
            val everything = tapes.values.flatten()
            for (decade in counts.keys.filter { setup.decades.isEmpty() || it in setup.decades }) {
                val id = "decade-$decade"
                val schedule = Dial.mix(id, everything, setOf(decade)) ?: continue
                schedules[id] = schedule
                dial += Channel(
                    id = id, name = Dial.decadeName(decade), dial = Dial.decadeDial(decade),
                    color = DECADE_COLORS[(decade / 10) % DECADE_COLORS.size],
                    description = "Every channel's tapes from the ${decade}s, mixed into one.",
                    family = "decades", logoRows = emptyList(), items = schedule.programsAt(now).size,
                    videoSeconds = schedule.programsAt(now).sumOf { it.durationMs } / 1000,
                    versions = emptyList(),
                )
            }
        }
        val mixSources = sources.filter { it.id in setup.myMix }
        if (mixSources.isNotEmpty() && mixSources.all { it.id in loaded }) {
            val mix = Dial.mix(MY_MIX, mixSources.flatMap { tapes.getValue(it.id) }, setup.decades.ifEmpty { null })
            if (mix != null) {
                schedules[MY_MIX] = mix
                dial += Channel(
                    id = MY_MIX, name = "MY MIX", dial = MY_MIX_DIAL, color = 0xFFFFC107,
                    description = "Your own channel: " + mixSources.joinToString(", ") { it.name } + ", mixed into one.",
                    family = "mine", logoRows = emptyList(), items = mix.programsAt(now).size,
                    videoSeconds = mix.programsAt(now).sumOf { it.durationMs } / 1000,
                    versions = emptyList(),
                )
            }
        }
        _schedules.value = schedules
        _lineup.value = Lineup.Ready(dial.sortedBy { it.dial })
    }

    private val channels: List<Channel>
        get() = (_lineup.value as? Lineup.Ready)?.channels.orEmpty()

    /** The guide's category; null shows every channel. */
    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    fun pickCategory(category: String?) {
        _category.value = category
    }

    /** What CH ▲ / ▼ walk through: the category picked, while the channel on is in it. */
    private val dialChannels: List<Channel>
        get() {
            val pick = _category.value ?: return channels
            val inIt = channels.filter { it.category == pick }
            return if (inIt.any { it.id == _tuned.value?.id }) inIt else channels
        }

    /**
     * What to fetch ahead while the tape on screen plays comfortably, like the site's neighbour
     * warming: the next tape on this channel, so the change of programme doesn't stall, then
     * what is on the channels either side, so CH ▲ / ▼ start quickly.
     */
    private fun warmAhead() {
        val preload = setup.value.preload
        val list = channels
        val index = list.indexOfFirst { it.id == _tuned.value?.id }
        if (preload == TvSetup.Preload.OFF || index < 0 || _screen.value == Screen.Home) return
        val now = repo.now()
        val schedules = _schedules.value
        // NEXT DOOR: the channels either side. WHOLE DIAL: every channel, nearest first.
        val reach = if (preload == TvSetup.Preload.WHOLE_DIAL) list.size / 2 else 1
        val programs = buildList {
            schedules[list[index].id]?.upNext(now, 1)?.firstOrNull()?.let { add(it.program) }
            for (distance in 1..reach) {
                for (step in intArrayOf(distance, -distance)) {
                    val neighbour = list[((index + step) % list.size + list.size) % list.size]
                    schedules[neighbour.id]?.airingAt(now)?.let { add(it.program) }
                }
            }
        }
        player.warm(programs)
    }

    // --- TV SETUP -----------------------------------------------------------------------------

    fun changeSetup(change: (TvSetup) -> TvSetup) {
        val before = setup.value
        setupStore.update(change)
        player.apply(setup.value)
        refreshSavedIndexBytes()
        val after = setup.value
        if (before.previewChannels != after.previewChannels) {
            if (_category.value == com.example.spelunkrtv.data.PREVIEW) _category.value = null
            reloadChannels()
        } else if (before.decades != after.decades || before.decadeChannels != after.decadeChannels ||
            before.hidden != after.hidden || before.myMix != after.myMix
        ) {
            rebuildDial()
        }
    }

    /** Re-reads the lineup (preview channels on or off) without interrupting the channel on. */
    private fun reloadChannels() {
        viewModelScope.launch {
            val list = runCatching { repo.channels(withPreviews = setup.value.previewChannels) }.getOrNull() ?: return@launch
            _source.value = Lineup.Ready(list)
            rebuildDial()
            loadSources(list)
        }
    }

    fun clearSavedIndexes() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            TapeCache.clear(getApplication())
            refreshSavedIndexBytes()
        }
    }

    private fun refreshSavedIndexBytes() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _savedIndexBytes.value = TapeCache.bytesUsed(getApplication())
        }
    }

    // --- Tuning ------------------------------------------------------------------------------

    fun tune(channel: Channel) {
        previewJob?.cancel()
        if (_tuned.value?.id != channel.id) {
            lastChannel = _tuned.value
            _tuned.value = channel
        }
        _tuneCount.value++
        tuneJob?.cancel()
        tuneJob = viewModelScope.launch {
            player.stop()
            val schedule = scheduleFor(channel) ?: return@launch
            if (_tuned.value?.id == channel.id) player.tune(schedule)
        }
    }

    /** On TV, moving the guide's focus tunes the preview once the focus settles. */
    fun preview(channel: Channel) {
        if (_tuned.value?.id == channel.id) return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(PREVIEW_SETTLE_MS)
            tune(channel)
        }
    }

    fun channelUp() = step(+1)
    fun channelDown() = step(-1)

    private fun step(by: Int) {
        val list = dialChannels
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == _tuned.value?.id }
        tune(list[((if (index < 0) 0 else index + by) % list.size + list.size) % list.size])
    }

    fun lastChannel() {
        lastChannel?.let { tune(it) }
    }

    /** SPELUNK: a random channel, never the one already on. */
    fun spelunk() {
        val others = channels.filter { it.id != _tuned.value?.id }
        if (others.isNotEmpty()) tune(others.random())
    }

    fun dialDigit(digit: Int) {
        val typed = (_dial.value + digit).takeLast(3)
        _dial.value = typed
        dialJob?.cancel()
        dialJob = viewModelScope.launch {
            if (typed.length < 3) delay(DIAL_SETTLE_MS)
            val number = typed.toInt()
            channels.firstOrNull { it.dial == number }?.let { tune(it) }
            _dial.value = ""
        }
    }

    // --- Navigation --------------------------------------------------------------------------

    fun open(screen: Screen) {
        if (_screen.value == screen) return
        if (screen == Screen.Home) backStack.clear()
        backStack.addLast(screen)
        show(screen)
        if (screen == Screen.Signal) checkSignal()
        if (screen == Screen.Setup) refreshSavedIndexBytes()
    }

    /** Tests each link from this device to the Internet Archive, using the tape on air. */
    fun checkSignal() {
        signalJob?.cancel()
        signalJob = viewModelScope.launch {
            val state = player.state.value
            val tape = state.airing?.let { TapeUnderTest(it.program.videoUrl, state.link.tapeBitsPerSecond) }
            signalCheck.run(tape, player::servers) { rows -> _signal.value = rows }
        }
    }

    fun openChannelPage(channel: Channel) {
        _pageChannel.value = channel
        open(Screen.ChannelPage)
    }

    /** TUNE IN on the home screen: the sound comes on and the guide opens. */
    fun tuneIn() {
        player.muted = false
        open(Screen.Guide)
    }

    /** Returns false when there is nothing to go back to, so the system can close the app. */
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeLast()
        show(backStack.last())
        return true
    }

    private fun show(screen: Screen) {
        _screen.value = screen
        player.muted = screen == Screen.Home
    }

    // --- Still watching? ---------------------------------------------------------------------

    fun onUserInput() {
        lastInputAt = System.currentTimeMillis()
    }

    private fun checkAsleep() {
        if (_stillWatching.value || _screen.value == Screen.Home) return
        if (System.currentTimeMillis() - lastInputAt > ASLEEP_AFTER_MS && player.player.isPlaying) {
            _stillWatching.value = true
            player.stop()
        }
    }

    fun wake() {
        onUserInput()
        if (_stillWatching.value) {
            _stillWatching.value = false
            player.resume()
        }
    }

    // --- Lifecycle ---------------------------------------------------------------------------

    fun onAppHidden() = player.stop()

    /** Live TV does not resume where it left off: coming back means coming back to live. */
    fun onAppShown() {
        if (!_stillWatching.value) player.resume()
    }

    override fun onCleared() {
        player.release()
    }

    companion object {
        const val MY_MIX = "my-mix"
        private const val MY_MIX_DIAL = 999
        /** A decade needs this many tapes to be offered. */
        private const val MIN_DECADE_TAPES = 5
        private val DECADE_COLORS = longArrayOf(0xFF8D6E63, 0xFF26A69A, 0xFFEF6C00, 0xFF7E57C2, 0xFFEC407A, 0xFF29B6F6, 0xFF9CCC65, 0xFFFFCA28, 0xFF5C6BC0, 0xFFE53935)
        const val ASLEEP_AFTER_MS = 4 * 60 * 60 * 1000L
        private const val PREVIEW_SETTLE_MS = 600L
        private const val DIAL_SETTLE_MS = 1_500L
    }
}
