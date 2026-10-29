package com.example.spelunkrtv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spelunkrtv.data.BroadcastRepository
import com.example.spelunkrtv.data.Channel
import com.example.spelunkrtv.diagnostics.CheckRow
import com.example.spelunkrtv.diagnostics.SignalCheck
import com.example.spelunkrtv.diagnostics.TapeUnderTest
import com.example.spelunkrtv.player.LivePlayer
import com.example.spelunkrtv.schedule.ChannelSchedule
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

enum class Screen { Home, Guide, Watch, Directory, ChannelPage, Signal }

sealed interface Lineup {
    data object Loading : Lineup
    data class Failed(val message: String) : Lineup
    data class Ready(val channels: List<Channel>) : Lineup
}

class SpelunkrViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BroadcastRepository()
    val player = LivePlayer(app, repo::now)

    private val _lineup = MutableStateFlow<Lineup>(Lineup.Loading)
    val lineup: StateFlow<Lineup> = _lineup.asStateFlow()

    /** Schedules arrive one channel at a time; the guide shows "ADJUSTING THE ANTENNA…" until then. */
    private val _schedules = MutableStateFlow<Map<String, ChannelSchedule>>(emptyMap())
    val schedules: StateFlow<Map<String, ChannelSchedule>> = _schedules.asStateFlow()

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
        load()
        viewModelScope.launch {
            while (isActive) {
                _now.value = repo.now()
                player.tick()
                checkAsleep()
                delay(1_000)
            }
        }
    }

    fun load() {
        _lineup.value = Lineup.Loading
        viewModelScope.launch {
            val channels = try {
                repo.channels()
            } catch (e: Exception) {
                _lineup.value = Lineup.Failed(e.message ?: "unknown error")
                return@launch
            }
            _lineup.value = Lineup.Ready(channels)
            // The home screen's little TV plays a random channel, sound off, like the site's.
            if (_tuned.value == null && channels.isNotEmpty()) {
                player.muted = _screen.value == Screen.Home
                tune(channels.random())
            }
            val gate = Semaphore(6)
            channels.forEach { channel ->
                launch {
                    gate.withPermit { scheduleOrNull(channel) }
                }
            }
        }
    }

    private suspend fun scheduleOrNull(channel: Channel): ChannelSchedule? {
        _schedules.value[channel.id]?.let { return it }
        return try {
            repo.schedule(channel).also { s -> _schedules.update { it + (channel.id to s) } }
        } catch (e: Exception) {
            null
        }
    }

    private val channels: List<Channel>
        get() = (_lineup.value as? Lineup.Ready)?.channels.orEmpty()

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
            val schedule = scheduleOrNull(channel) ?: return@launch
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
        val list = channels
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
    }

    /** Tests each link from this device to the Internet Archive, using the tape on air. */
    fun checkSignal() {
        signalJob?.cancel()
        signalJob = viewModelScope.launch {
            val state = player.state.value
            val tape = state.airing?.let { TapeUnderTest(it.program.videoUrl, state.link.tapeBitsPerSecond) }
            signalCheck.run(tape) { rows -> _signal.value = rows }
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
        const val ASLEEP_AFTER_MS = 4 * 60 * 60 * 1000L
        private const val PREVIEW_SETTLE_MS = 600L
        private const val DIAL_SETTLE_MS = 1_500L
    }
}
