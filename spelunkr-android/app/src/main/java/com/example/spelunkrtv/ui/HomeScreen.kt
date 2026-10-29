package com.example.spelunkrtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.spelunkrtv.Lineup
import com.example.spelunkrtv.data.Channel
import com.example.spelunkrtv.player.LivePlayer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    lineup: Lineup,
    tuned: Channel?,
    playerState: LivePlayer.State,
    player: Player,
    onTuneIn: () -> Unit,
    onRetry: () -> Unit,
    onSignal: () -> Unit,
) {
    val isTv = LocalIsTv.current
    val tuneInFocus = remember { FocusRequester() }
    val ready = lineup as? Lineup.Ready
    LaunchedEffect(ready != null) { if (ready != null) runCatching { tuneInFocus.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Sky)
            // "or press any key." Back still leaves; volume and system keys never reach the app.
            // Acting on key-up keeps the rest of the press from landing on the guide that opens.
            .onPreviewKeyEvent { e ->
                if (ready == null || !isTv || e.key == Key.Back) return@onPreviewKeyEvent false
                if (e.type == KeyEventType.KeyUp) onTuneIn()
                true
            }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isTv) 6.dp else 10.dp),
    ) {
        Text("SPELUNKR", style = spelunkrText(if (isTv) 36.sp else 40.sp, Palette.Yellow))

        // The little TV set, playing a random channel with the sound off.
        Box(
            Modifier.size(width = if (isTv) 260.dp else 260.dp, height = 156.dp)
                .background(Color(0xFF1E1E22)).border(3.dp, Color(0xFF3A3A40)).padding(12.dp),
        ) {
            VideoSurface(player, Modifier.fillMaxSize())
            when {
                playerState.noSignal -> NoSignal(playerState.link)
                playerState.tuning -> TuningSlate(tuned, playerState.link)
            }
            if (tuned != null) {
                Text(
                    "CH ${tuned.dialLabel} · ${tuned.name}",
                    style = spelunkrText(13.sp, Palette.Yellow),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                )
            }
        }

        Text(
            "We are a free alternative TV network\nbuilt from the Internet Archive's forgotten videos.",
            style = spelunkrText(16.sp),
            textAlign = TextAlign.Center,
        )
        Text("Always live, forever.", style = spelunkrText(16.sp, Palette.Yellow))

        when (lineup) {
            Lineup.Loading -> Text("PLEASE STAND BY", style = spelunkrText(20.sp, Palette.Yellow))
            is Lineup.Failed -> {
                Text("TECHNICAL DIFFICULTIES", style = spelunkrText(20.sp, Palette.Yellow))
                Text(lineup.message, style = spelunkrText(12.sp, Palette.Muted), textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpelunkrButton("TRY AGAIN", onRetry, gold = true)
                    SpelunkrButton("SIGNAL CHECK", onSignal)
                }
            }
            is Lineup.Ready -> {
                val hours = lineup.channels.sumOf { it.videoSeconds } / 3600
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(lineup.channels.size.toString(), "CHANNELS", "AND COUNTING")
                    Stat("24/7", "LIVE SYNC", null)
                    Stat(NumberFormat.getIntegerInstance(Locale.US).format(hours), "HOURS", "AND COUNTING")
                }
                SpelunkrButton("▶ TUNE IN", onTuneIn, gold = true, modifier = Modifier.focusRequester(tuneInFocus))
                if (isTv) Text("or press any key.", style = spelunkrText(13.sp))
            }
        }

        Spacer(Modifier.height(if (isTv) 0.dp else 8.dp))
        Text("Spelunkr is free to watch. No account, no ads.", style = spelunkrText(13.sp), textAlign = TextAlign.Center)
        Text("ALL PROGRAMMING COURTESY OF THE INTERNET ARCHIVE", style = spelunkrText(12.sp, Palette.Yellow), textAlign = TextAlign.Center)
    }
}

@Composable
private fun Stat(big: String, label: String, small: String?) {
    Column(
        Modifier.width(96.dp).background(Palette.Panel).border(3.dp, Palette.Rim).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(big, style = spelunkrText(26.sp, Palette.Yellow))
        Text(label, style = spelunkrText(12.sp))
        if (small != null) Text(small, style = spelunkrText(9.sp, Palette.Muted, shadow = false))
    }
}
