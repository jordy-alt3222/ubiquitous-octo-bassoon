package com.example.spelunkrtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.spelunkrtv.schedule.ChannelSchedule

/** DIRECTORY: every channel on the dial, with what it is about. */
@Composable
fun DirectoryScreen(channels: List<Channel>, now: Long, onNavigate: (Screen) -> Unit, onOpen: (Channel) -> Unit) {
    val isTv = LocalIsTv.current
    val first = remember { FocusRequester() }
    LaunchedEffect(channels.isNotEmpty()) { if (isTv && channels.isNotEmpty()) first.requestFocusSoon() }
    Column(Modifier.fillMaxSize().background(Palette.Sky)) {
        TopBar(now, Screen.Directory, onNavigate, showClock = isTv)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(if (isTv) 280.dp else 300.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(channels, key = { it.id }) { channel ->
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                Column(
                    Modifier
                        .then(if (channel == channels.first()) Modifier.focusRequester(first) else Modifier)
                        .background(Palette.Panel)
                        .border(3.dp, if (focused) Palette.Yellow else Palette.Rim)
                        .remoteFocusable()
                        .clickable(interactionSource = interaction, indication = null) { onOpen(channel) }
                        .padding(10.dp),
                ) {
                    ChannelHeader(channel)
                    Text(
                        channel.description,
                        style = spelunkrText(12.sp, shadow = false),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(statsLine(channel), style = spelunkrText(11.sp, Palette.Muted, shadow = false), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** One channel: what it is, and what is coming up on it. */
@Composable
fun ChannelPage(
    channel: Channel,
    schedule: ChannelSchedule?,
    now: Long,
    onNavigate: (Screen) -> Unit,
    onWatch: (Channel) -> Unit,
) {
    val isTv = LocalIsTv.current
    val watchFocus = remember { FocusRequester() }
    LaunchedEffect(channel.id) { if (isTv) runCatching { watchFocus.requestFocus() } }
    Column(Modifier.fillMaxSize().background(Palette.Sky)) {
        TopBar(now, Screen.ChannelPage, onNavigate, showClock = isTv)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column {
                        ChannelHeader(channel, large = true)
                        Text(channel.description, style = spelunkrText(14.sp, shadow = false), modifier = Modifier.padding(top = 8.dp))
                        Text(statsLine(channel), style = spelunkrText(12.sp, Palette.Muted), modifier = Modifier.padding(top = 6.dp))
                        SpelunkrButton(
                            "▶ WATCH",
                            { onWatch(channel) },
                            gold = true,
                            modifier = Modifier.padding(top = 10.dp).focusRequester(watchFocus),
                        )
                    }
                }
            }
            item { Text("SCHEDULE", style = spelunkrText(16.sp, Palette.Yellow), modifier = Modifier.padding(top = 8.dp)) }
            if (schedule == null) {
                item { Text("ADJUSTING THE ANTENNA…", style = spelunkrText(14.sp, Palette.Muted)) }
            } else {
                val current = schedule.airingAt(now)
                items(listOf(current) + schedule.upNext(now, 11), key = { it.key }) { airing ->
                    Row(
                        Modifier.fillMaxWidth().background(Palette.PanelDeep).border(1.dp, Palette.Rim).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (airing === current) "NOW" else timeOfDay(airing.startMs),
                            style = spelunkrText(14.sp, Palette.Yellow),
                            modifier = Modifier.width(96.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(airing.program.title, style = spelunkrText(14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            airing.program.year?.let { Text(it, style = spelunkrText(11.sp, Palette.Cyan, shadow = false)) }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(runtime(airing.durationMs), style = spelunkrText(12.sp, Palette.Muted))
                    }
                }
            }
            item {
                Text(
                    "ALL PROGRAMMING COURTESY OF THE INTERNET ARCHIVE",
                    style = spelunkrText(11.sp, Palette.Yellow),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ChannelHeader(channel: Channel, large: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(channel.dialLabel, style = spelunkrText(if (large) 34.sp else 26.sp, Palette.Yellow))
        Spacer(Modifier.width(8.dp))
        PixelLogo(channel, if (large) 40.dp else 30.dp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(channel.name, style = spelunkrText(if (large) 22.sp else 16.sp, Palette.Yellow), maxLines = 1)
            FamilyTag(channel.family)
        }
    }
}

private fun statsLine(channel: Channel) = "${channel.items} TAPES · ${channel.videoSeconds / 3600} HOURS"
