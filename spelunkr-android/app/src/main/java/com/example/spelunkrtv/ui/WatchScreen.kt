package com.example.spelunkrtv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.spelunkrtv.data.Channel
import com.example.spelunkrtv.player.LivePlayer
import com.example.spelunkrtv.schedule.ChannelSchedule
import kotlinx.coroutines.delay

private const val CONTROLS_HIDE_MS = 5_000L
private const val BANNER_MS = 4_000L

@Composable
fun WatchScreen(
    tuned: Channel?,
    schedule: ChannelSchedule?,
    state: LivePlayer.State,
    player: Player,
    now: Long,
    tuneCount: Int,
    dial: String,
    onGuide: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    onTogglePause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSpelunk: () -> Unit,
) {
    val isTv = LocalIsTv.current
    var controls by remember { mutableStateOf(false) }
    var activity by remember { mutableIntStateOf(0) }
    var banner by remember { mutableStateOf(true) }
    val rootFocus = remember { FocusRequester() }
    val pauseFocus = remember { FocusRequester() }

    LaunchedEffect(controls, activity) {
        if (controls) {
            delay(CONTROLS_HIDE_MS)
            controls = false
        }
    }
    LaunchedEffect(controls) {
        runCatching { if (controls && isTv) pauseFocus.requestFocus() else rootFocus.requestFocus() }
    }
    LaunchedEffect(tuneCount) {
        banner = true
        delay(BANNER_MS)
        banner = false
    }
    BackHandler(enabled = controls) { controls = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { activity++; false }
            .focusRequester(rootFocus)
            .onKeyEvent { event ->
                if (controls) return@onKeyEvent false
                val down = event.type == KeyEventType.KeyDown
                when (event.key) {
                    Key.DirectionUp -> { if (down) onChannelUp(); true }
                    Key.DirectionDown -> { if (down) onChannelDown(); true }
                    Key.DirectionLeft -> { if (down) onRewind(); true }
                    Key.DirectionRight -> { if (down) onForward(); true }
                    // On key-up, so the release does not press the PAUSE key that takes focus.
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { if (!down) controls = true; true }
                    else -> false
                }
            }
            .focusable()
            .then(
                if (isTv) Modifier
                else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    controls = !controls
                },
            ),
    ) {
        VideoSurface(player, Modifier.fillMaxSize())
        when {
            state.noSignal -> NoSignal(large = true)
            state.tuning -> TuningSlate(tuned, large = true)
        }

        val airing = state.airing ?: schedule?.airingAt(now)
        if ((banner || controls) && tuned != null) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CH ${tuned.dialLabel}", style = spelunkrText(if (isTv) 36.sp else 28.sp, Palette.Yellow))
                    Spacer(Modifier.width(12.dp))
                    PixelLogo(tuned, 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(tuned.name, style = spelunkrText(22.sp))
                    FamilyTag(tuned.family)
                }
                airing?.let {
                    Text(
                        "NOW: ${it.program.title}",
                        style = spelunkrText(18.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (dial.isNotEmpty()) {
            Text(
                dial,
                style = spelunkrText(64.sp, Palette.Yellow),
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
            )
        }

        if (controls) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000))))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (airing != null) ProgressRow(elapsedMs = now - airing.startMs, totalMs = airing.durationMs)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpelunkrButton("GUIDE", onGuide)
                    SpelunkrButton("CH ▼", onChannelDown)
                    SpelunkrButton("CH ▲", onChannelUp)
                    SpelunkrButton(
                        if (state.paused) "▶ PLAY" else "❚❚ PAUSE",
                        onTogglePause,
                        modifier = Modifier.focusRequester(pauseFocus),
                    )
                    SpelunkrButton("◀◀ 15S", onRewind)
                    SpelunkrButton("15S ▶▶", onForward, enabled = !state.live || state.paused)
                    SpelunkrButton("SPELUNK", onSpelunk, gold = true)
                    Spacer(Modifier.width(8.dp))
                    LiveBadge(state.live, state.behindLiveMs, state.paused)
                }
            }
        }
    }
}
