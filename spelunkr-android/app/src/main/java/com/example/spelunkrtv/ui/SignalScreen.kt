package com.example.spelunkrtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelunkrtv.Screen
import com.example.spelunkrtv.data.Channel
import com.example.spelunkrtv.diagnostics.CheckRow
import com.example.spelunkrtv.diagnostics.Verdict
import com.example.spelunkrtv.diagnostics.formatBits
import com.example.spelunkrtv.diagnostics.formatBytes
import com.example.spelunkrtv.player.LivePlayer

/** SIGNAL CHECK: is this device actually talking to the Internet Archive, and if not, where it breaks. */
@Composable
fun SignalScreen(
    rows: List<CheckRow>,
    tuned: Channel?,
    state: LivePlayer.State,
    now: Long,
    onNavigate: (Screen) -> Unit,
    onTestAgain: () -> Unit,
) {
    val isTv = LocalIsTv.current
    val testFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (isTv) testFocus.requestFocusSoon() }
    val checking = rows.any { it.verdict == Verdict.Checking }

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Sky)) {
        val wide = maxWidth >= 720.dp
        Column(Modifier.fillMaxSize()) {
            TopBar(now, Screen.Signal, onNavigate, showClock = wide)
            Column(
                Modifier.fillMaxSize().then(if (wide) Modifier else Modifier.verticalScroll(rememberScrollState()))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SIGNAL CHECK", style = spelunkrText(24.sp, Palette.Yellow))
                        Text("Is this device talking to the Internet Archive?", style = spelunkrText(14.sp))
                    }
                    SpelunkrButton(
                        if (checking) "CHECKING…" else "TEST AGAIN",
                        onTestAgain,
                        gold = true,
                        modifier = Modifier.focusRequester(testFocus),
                    )
                }
                val rightNow = @Composable { m: Modifier -> RightNow(tuned, state, m) }
                val checks = @Composable { m: Modifier -> Checks(rows, m) }
                if (wide) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rightNow(Modifier.weight(0.42f))
                        checks(Modifier.weight(0.58f))
                    }
                } else {
                    rightNow(Modifier.fillMaxWidth())
                    checks(Modifier.fillMaxWidth())
                }
                Text(
                    "On a computer, adb logcat -s Spelunkr shows each tune, the server it reached and any error, as it happens.",
                    style = spelunkrText(11.sp, Palette.Muted, shadow = false),
                )
            }
        }
    }
}

/** What the player is doing this second, straight from its byte counter. */
@Composable
private fun RightNow(tuned: Channel?, state: LivePlayer.State, modifier: Modifier) {
    val link = state.link
    Panel(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("RIGHT NOW", style = spelunkrText(15.sp, Palette.Yellow))
            LinkIndicator(state, large = true)
            Line("CHANNEL", tuned?.let { "CH ${it.dialLabel} · ${it.name}" } ?: "None yet")
            Line("TAPE", state.airing?.program?.title ?: "—")
            Line("SERVER", link.host ?: "Nothing has answered yet")
            Line("RECEIVED", "${formatBytes(link.bytes)} since tuning in · now ${formatBits(link.bitsPerSecond)}")
            Line("BUFFERED", "${link.bufferedMs / 1000} s of video ready ahead")
            Line("THIS TAPE", link.tapeBitsPerSecond?.let { "needs about ${formatBits(it)}" } ?: "Size not known until it starts")
            link.error?.let { Line("LAST PROBLEM", it, Palette.Live) }
        }
    }
}

@Composable
private fun Line(label: String, value: String, color: Color = Color.White) {
    Row {
        Text(label, style = spelunkrText(12.sp, Palette.Cyan, shadow = false), modifier = Modifier.width(110.dp))
        Text(value, style = spelunkrText(12.sp, color, shadow = false))
    }
}

@Composable
private fun Checks(rows: List<CheckRow>, modifier: Modifier) {
    Panel(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CHECKS", style = spelunkrText(15.sp, Palette.Yellow))
            if (rows.isEmpty()) Text("Press TEST AGAIN.", style = spelunkrText(13.sp))
            rows.forEach { row ->
                Row {
                    VerdictTag(row.verdict)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(row.label, style = spelunkrText(13.sp, Palette.Yellow))
                        Text(row.detail, style = spelunkrText(12.sp, shadow = false))
                    }
                }
            }
        }
    }
}

@Composable
private fun VerdictTag(verdict: Verdict) {
    val (text, color) = when (verdict) {
        Verdict.Checking -> "…" to Palette.Muted
        Verdict.Ok -> "OK" to Palette.Ok
        Verdict.Warn -> "!" to Palette.Warn
        Verdict.Fail -> "✕" to Palette.Live
    }
    Box(Modifier.width(44.dp).border(2.dp, color).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
        Text(text, style = spelunkrText(14.sp, color, shadow = false), textAlign = TextAlign.Center)
    }
}
