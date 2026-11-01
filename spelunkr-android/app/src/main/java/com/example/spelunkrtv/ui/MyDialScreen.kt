package com.example.spelunkrtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelunkrtv.Screen
import com.example.spelunkrtv.data.Channel
import com.example.spelunkrtv.data.TvSetup

/** MY DIAL: which channels are on the dial, and which go into MY MIX. */
@Composable
fun MyDialScreen(
    channels: List<Channel>,
    setup: TvSetup,
    now: Long,
    onNavigate: (Screen) -> Unit,
    onChange: ((TvSetup) -> TvSetup) -> Unit,
) {
    val isTv = LocalIsTv.current
    val first = remember { FocusRequester() }
    LaunchedEffect(channels.isNotEmpty()) { if (isTv && channels.isNotEmpty()) first.requestFocusSoon() }
    Column(Modifier.fillMaxSize().background(Palette.Sky)) {
        TopBar(now, Screen.MyDial, onNavigate, showClock = isTv)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Column(Modifier.padding(bottom = 6.dp)) {
                    Text("MY DIAL", style = spelunkrText(24.sp, Palette.Yellow))
                    Text(
                        "ON DIAL / OFF: whether a channel is on your dial. ★ MIX: mix it into MY MIX (999), your own " +
                            "channel. Star a few, then find MY MIX in the guide.",
                        style = spelunkrText(13.sp, shadow = false),
                    )
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpelunkrButton("ALL ON", { onChange { it.copy(hidden = emptySet()) } }, modifier = Modifier.focusRequester(first))
                        SpelunkrButton("CLEAR MY MIX", { onChange { it.copy(myMix = emptySet()) } })
                    }
                }
            }
            items(channels, key = { it.id }) { channel ->
                val on = channel.id !in setup.hidden
                val mixed = channel.id in setup.myMix
                Row(
                    Modifier.fillMaxWidth().background(Palette.Panel).border(1.dp, Palette.Rim).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(channel.dialLabel, style = spelunkrText(20.sp, Palette.Yellow), modifier = Modifier.width(56.dp))
                    PixelLogo(channel, 28.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(channel.name, style = spelunkrText(15.sp, if (on) Palette.Yellow else Palette.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(channel.category, style = spelunkrText(11.sp, Palette.Muted, shadow = false))
                    }
                    SpelunkrButton(
                        if (on) "ON DIAL" else "OFF",
                        { onChange { s -> s.copy(hidden = if (on) s.hidden + channel.id else s.hidden - channel.id) } },
                        gold = on,
                    )
                    Spacer(Modifier.width(6.dp))
                    SpelunkrButton(
                        if (mixed) "★ MIX" else "☆ MIX",
                        { onChange { s -> s.copy(myMix = if (mixed) s.myMix - channel.id else s.myMix + channel.id) } },
                        gold = mixed,
                    )
                }
            }
        }
    }
}
