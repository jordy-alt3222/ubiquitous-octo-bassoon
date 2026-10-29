package com.example.spelunkrtv

import android.app.UiModeManager
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import com.example.spelunkrtv.ui.ChannelPage
import com.example.spelunkrtv.ui.DirectoryScreen
import com.example.spelunkrtv.ui.GuideScreen
import com.example.spelunkrtv.ui.HomeScreen
import com.example.spelunkrtv.ui.LocalIsTv
import com.example.spelunkrtv.ui.Palette
import com.example.spelunkrtv.ui.SignalScreen
import com.example.spelunkrtv.ui.SpelunkrButton
import com.example.spelunkrtv.ui.WatchScreen
import com.example.spelunkrtv.ui.spelunkrText
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: SpelunkrViewModel by viewModels()
    private val isTv by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            (getSystemService(UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalIsTv provides isTv) {
                SpelunkrApp(vm, onExit = ::finish)
            }
        }
        // The screen stays on while a tape plays, and may sleep when nothing does.
        vm.player.player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        })
        // Phones watch full screen in landscape; TVs are always landscape and full screen already.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.screen.collect { screen -> if (!isTv) setPhoneFullScreen(screen == Screen.Watch) }
            }
        }
    }

    private fun setPhoneFullScreen(on: Boolean) {
        requestedOrientation =
            if (on) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (on) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onAppShown()
    }

    override fun onStop() {
        vm.onAppHidden()
        super.onStop()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) vm.onUserInput()
        return super.dispatchTouchEvent(ev)
    }

    /** Remote keys that mean the same thing on every screen of a TV set. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        vm.onUserInput()
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        if (vm.stillWatching.value) {
            vm.wake()
            return true
        }
        val watching = vm.screen.value == Screen.Watch || vm.screen.value == Screen.Guide
        val code = event.keyCode
        val handled = when {
            !watching -> false
            code in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> { vm.dialDigit(code - KeyEvent.KEYCODE_0); true }
            code in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> { vm.dialDigit(code - KeyEvent.KEYCODE_NUMPAD_0); true }
            code == KeyEvent.KEYCODE_CHANNEL_UP || code == KeyEvent.KEYCODE_PAGE_UP -> { vm.channelUp(); true }
            code == KeyEvent.KEYCODE_CHANNEL_DOWN || code == KeyEvent.KEYCODE_PAGE_DOWN -> { vm.channelDown(); true }
            code == KeyEvent.KEYCODE_LAST_CHANNEL -> { vm.lastChannel(); true }
            code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || code == KeyEvent.KEYCODE_MEDIA_PLAY ||
                code == KeyEvent.KEYCODE_MEDIA_PAUSE -> { vm.player.togglePause(); true }
            code == KeyEvent.KEYCODE_MEDIA_REWIND -> { vm.player.rewind15(); true }
            code == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { vm.player.forward15(); true }
            code == KeyEvent.KEYCODE_GUIDE -> { vm.open(Screen.Guide); true }
            else -> false
        }
        return handled || super.dispatchKeyEvent(event)
    }
}

@Composable
fun SpelunkrApp(vm: SpelunkrViewModel, onExit: () -> Unit) {
    val lineup by vm.lineup.collectAsStateWithLifecycle()
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val tuned by vm.tuned.collectAsStateWithLifecycle()
    val tuneCount by vm.tuneCount.collectAsStateWithLifecycle()
    val pageChannel by vm.pageChannel.collectAsStateWithLifecycle()
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val dial by vm.dial.collectAsStateWithLifecycle()
    val stillWatching by vm.stillWatching.collectAsStateWithLifecycle()
    val signal by vm.signal.collectAsStateWithLifecycle()
    val channels = (lineup as? Lineup.Ready)?.channels.orEmpty()
    val player = vm.player.player

    BackHandler { if (!vm.back()) onExit() }

    Box(Modifier.fillMaxSize().background(Palette.Purple)) {
        when (screen) {
            Screen.Home -> HomeScreen(
                lineup, tuned, playerState, player,
                onTuneIn = vm::tuneIn, onRetry = vm::load, onSignal = { vm.open(Screen.Signal) },
            )
            Screen.Guide -> GuideScreen(
                channels = channels,
                schedules = schedules,
                tuned = tuned,
                playerState = playerState,
                player = player,
                now = now,
                onNavigate = vm::open,
                onPreview = vm::preview,
                onTune = vm::tune,
                onWatch = { vm.open(Screen.Watch) },
                onViewChannel = vm::openChannelPage,
                onChannelUp = vm::channelUp,
                onChannelDown = vm::channelDown,
            )
            Screen.Watch -> WatchScreen(
                tuned = tuned,
                schedule = tuned?.let { schedules[it.id] },
                state = playerState,
                player = player,
                now = now,
                tuneCount = tuneCount,
                dial = dial,
                onGuide = { vm.open(Screen.Guide) },
                onChannelUp = vm::channelUp,
                onChannelDown = vm::channelDown,
                onTogglePause = vm.player::togglePause,
                onRewind = vm.player::rewind15,
                onForward = vm.player::forward15,
                onSpelunk = vm::spelunk,
                onSignal = { vm.open(Screen.Signal) },
            )
            Screen.Signal -> SignalScreen(
                rows = signal,
                tuned = tuned,
                state = playerState,
                now = now,
                onNavigate = vm::open,
                onTestAgain = vm::checkSignal,
            )
            Screen.Directory -> DirectoryScreen(channels, now, onNavigate = vm::open, onOpen = vm::openChannelPage)
            Screen.ChannelPage -> pageChannel?.let { channel ->
                ChannelPage(
                    channel = channel,
                    schedule = schedules[channel.id],
                    now = now,
                    onNavigate = vm::open,
                    onWatch = { vm.tune(it); vm.open(Screen.Watch) },
                )
            }
        }
        if (stillWatching) StillWatching(onWake = vm::wake)
    }
}

/** After four hours with no button pressed the tape stops, as on the site. */
@Composable
private fun StillWatching(onWake: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).background(Palette.Panel).border(3.dp, Palette.Rim).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("STILL WATCHING?", style = spelunkrText(26.sp, Palette.Yellow))
            Text(
                "The tape stopped after four hours with no button pressed. Press any button to tune back in.",
                style = spelunkrText(14.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            SpelunkrButton("I'M STILL HERE", onWake, gold = true, modifier = Modifier.focusRequester(focus))
        }
    }
}
