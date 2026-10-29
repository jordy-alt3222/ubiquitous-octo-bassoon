package com.example.spelunkrtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.spelunkrtv.Screen
import com.example.spelunkrtv.data.Channel
import com.example.spelunkrtv.player.LivePlayer
import com.example.spelunkrtv.schedule.ChannelSchedule
import kotlinx.coroutines.delay

private const val HALF_HOUR = 30 * 60_000L

@Composable
fun GuideScreen(
    channels: List<Channel>,
    schedules: Map<String, ChannelSchedule>,
    tuned: Channel?,
    playerState: LivePlayer.State,
    player: Player,
    now: Long,
    onNavigate: (Screen) -> Unit,
    onPreview: (Channel) -> Unit,
    onTune: (Channel) -> Unit,
    onWatch: () -> Unit,
    onViewChannel: (Channel) -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
) {
    val isTv = LocalIsTv.current
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Sky)) {
        val wide = maxWidth >= 720.dp
        Column(Modifier.fillMaxSize()) {
            TopBar(now, Screen.Guide, onNavigate, showClock = wide)
            val preview = @Composable { m: Modifier ->
                PreviewBox(tuned, playerState, player, onWatch, onChannelUp, onChannelDown, fillHeight = wide, modifier = m)
            }
            val info = @Composable { m: Modifier ->
                InfoPanel(tuned, tuned?.let { schedules[it.id] }, now, playerState, onWatch, onViewChannel, compact = !wide, modifier = m)
            }
            if (wide) {
                Row(
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    preview(Modifier.weight(0.42f).fillMaxHeight())
                    info(Modifier.weight(0.58f).fillMaxHeight())
                }
            } else {
                preview(Modifier.fillMaxWidth())
                info(Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            }
            GuideGrid(
                channels = channels,
                schedules = schedules,
                now = now,
                tunedId = tuned?.id,
                windowMs = if (wide) 6 * HALF_HOUR else 3 * HALF_HOUR,
                // TV: focus previews, OK watches. Touch: a tap tunes, a second tap watches.
                onFocus = if (isTv) onPreview else null,
                onSelect = { channel -> if (isTv || channel.id == tuned?.id) { onTune(channel); onWatch() } else onTune(channel) },
                modifier = Modifier.fillMaxWidth().weight(if (wide) 1f else 1.2f)
                    .padding(horizontal = if (wide) 16.dp else 0.dp, vertical = 4.dp),
            )
            if (wide) Ticker(channels.size)
        }
    }
}

@Composable
fun TopBar(now: Long, current: Screen, onNavigate: (Screen) -> Unit, showClock: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(Palette.HeaderBar).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).background(Palette.Yellow).border(2.dp, Color.Black),
            contentAlignment = Alignment.Center,
        ) { Text("S", style = spelunkrText(22.sp, Color.Black, shadow = false)) }
        Spacer(Modifier.width(8.dp))
        if (showClock) {
            Column {
                Text("Spelunkr", style = spelunkrText(15.sp, Palette.Yellow))
                Text("TV Guide", style = spelunkrText(15.sp, Palette.Yellow))
            }
        }
        Spacer(Modifier.weight(1f))
        listOf(Screen.Home to "HOME", Screen.Guide to "GUIDE", Screen.Directory to "DIRECTORY").forEach { (screen, label) ->
            SpelunkrButton(label, { onNavigate(screen) }, gold = screen == current, modifier = Modifier.padding(start = 6.dp))
        }
        if (showClock) {
            Text(clockText(now), style = spelunkrText(16.sp), modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun PreviewBox(
    tuned: Channel?,
    state: LivePlayer.State,
    player: Player,
    onWatch: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    fillHeight: Boolean,
    modifier: Modifier,
) {
    val isTv = LocalIsTv.current
    Panel(modifier) {
        Column {
            // Wide: the picture takes what the panel leaves (letterboxed by the player). Narrow: 16:9.
            val size = if (fillHeight) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            Box(size.clickable(enabled = !isTv, onClick = onWatch)) {
                VideoSurface(player, Modifier.fillMaxSize())
                when {
                    state.noSignal -> NoSignal()
                    state.tuning -> TuningSlate(tuned)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SpelunkrButton("▼", onChannelDown)
                Text("CH ${tuned?.dialLabel ?: "--"}", style = spelunkrText(20.sp), modifier = Modifier.padding(horizontal = 10.dp))
                SpelunkrButton("▲", onChannelUp)
                Spacer(Modifier.weight(1f))
                Text("SPELUNKR.TV", style = spelunkrText(11.sp, Palette.Yellow))
            }
        }
    }
}

@Composable
fun InfoPanel(
    channel: Channel?,
    schedule: ChannelSchedule?,
    now: Long,
    state: LivePlayer.State,
    onWatch: () -> Unit,
    onViewChannel: (Channel) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Panel(modifier) {
        if (channel == null) {
            Text("PLEASE STAND BY", style = spelunkrText(18.sp, Palette.Yellow))
            return@Panel
        }
        Column(if (compact) Modifier else Modifier.fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelLogo(channel, 30.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(channel.name, style = spelunkrText(18.sp, Palette.Yellow), maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CH ${channel.dialLabel}", style = spelunkrText(12.sp, Palette.Cyan))
                        FamilyTag(channel.family)
                    }
                }
                LiveBadge(state.live, state.behindLiveMs, state.paused)
            }
            val airing = schedule?.airingAt(now)
            if (airing == null) {
                Text("ADJUSTING THE ANTENNA…", style = spelunkrText(14.sp, Palette.Muted), modifier = Modifier.padding(top = 8.dp))
                return@Column
            }
            Text("NOW PLAYING", style = spelunkrText(13.sp, Palette.Yellow), modifier = Modifier.padding(top = 6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    airing.program.title,
                    style = spelunkrText(if (compact) 16.sp else 20.sp),
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                airing.program.year?.let { Text("  $it", style = spelunkrText(14.sp, Palette.Cyan)) }
            }
            airing.program.description?.let {
                Text(
                    it,
                    style = spelunkrText(13.sp, shadow = false),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            ProgressRow(elapsedMs = now - airing.startMs, totalMs = airing.durationMs)
            if (!compact) {
                Text("UP NEXT", style = spelunkrText(13.sp, Palette.Yellow), modifier = Modifier.padding(top = 4.dp))
                schedule.upNext(now, 2).forEach { next ->
                    Row {
                        Text(timeOfDay(next.startMs), style = spelunkrText(14.sp, Palette.Yellow), modifier = Modifier.width(90.dp))
                        Text(next.program.title, style = spelunkrText(14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (!compact) Spacer(Modifier.weight(1f))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpelunkrButton("WATCH", onWatch, gold = true)
                SpelunkrButton("VIEW CHANNEL", { onViewChannel(channel) })
            }
        }
    }
}

@Composable
fun ProgressRow(elapsedMs: Long, totalMs: Long) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(runtime(elapsedMs), style = spelunkrText(14.sp))
        Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(8.dp).background(Palette.PanelDeep).border(1.dp, Palette.Rim)) {
            val fraction = if (totalMs > 0) (elapsedMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Palette.Yellow))
        }
        Text(runtime(totalMs - elapsedMs), style = spelunkrText(14.sp))
    }
}

/**
 * The listings grid. Each channel row is one focus stop, so the D-pad walks channels up and
 * down; the programmes inside are sized by how much of the time window they fill.
 */
@Composable
fun GuideGrid(
    channels: List<Channel>,
    schedules: Map<String, ChannelSchedule>,
    now: Long,
    tunedId: String?,
    windowMs: Long,
    onFocus: ((Channel) -> Unit)?,
    onSelect: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTv.current
    val start = now - now % HALF_HOUR
    val end = start + windowMs
    val channelColumn = if (isTv) 170.dp else 112.dp
    val rowHeight = if (isTv) 48.dp else 56.dp
    val listState = rememberLazyListState()
    val focusTarget = remember { FocusRequester() }
    val firstFocusId = remember(channels.isNotEmpty()) { tunedId ?: channels.firstOrNull()?.id }

    LaunchedEffect(isTv, firstFocusId) {
        if (!isTv || firstFocusId == null) return@LaunchedEffect
        val index = channels.indexOfFirst { it.id == firstFocusId }.coerceAtLeast(0)
        listState.scrollToItem(index)
        delay(50)
        runCatching { focusTarget.requestFocus() }
    }

    Column(modifier.border(3.dp, Palette.Rim).background(Palette.PanelDeep)) {
        Row(Modifier.fillMaxWidth().background(Palette.HeaderBar).padding(vertical = 3.dp)) {
            Text("CHANNEL", style = spelunkrText(13.sp, Palette.Yellow), modifier = Modifier.width(channelColumn).padding(start = 8.dp))
            BoxWithConstraints(Modifier.weight(1f)) {
                val slots = (windowMs / HALF_HOUR).toInt()
                for (i in 0 until slots) {
                    val labelEvery = if (slots > 4) 2 else 1
                    if (i % labelEvery == 0) {
                        Text(
                            timeOfDay(start + i * HALF_HOUR),
                            style = spelunkrText(13.sp, Palette.Yellow),
                            modifier = Modifier.offset(x = maxWidth * (i.toFloat() / slots) + 4.dp),
                        )
                    }
                }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(channels, key = { _, c -> c.id }) { _, channel ->
                GuideRow(
                    channel = channel,
                    schedule = schedules[channel.id],
                    start = start,
                    end = end,
                    tuned = channel.id == tunedId,
                    channelColumn = channelColumn,
                    onFocus = onFocus,
                    onSelect = onSelect,
                    modifier = Modifier.height(rowHeight)
                        .then(if (channel.id == firstFocusId) Modifier.focusRequester(focusTarget) else Modifier),
                )
            }
        }
    }
}

@Composable
private fun GuideRow(
    channel: Channel,
    schedule: ChannelSchedule?,
    start: Long,
    end: Long,
    tuned: Boolean,
    channelColumn: androidx.compose.ui.unit.Dp,
    onFocus: ((Channel) -> Unit)?,
    onSelect: (Channel) -> Unit,
    modifier: Modifier,
) {
    val isTv = LocalIsTv.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .then(if (tuned) Modifier.background(Palette.Selected) else Modifier.background(Palette.Panel))
            .border(if (focused) 3.dp else 0.5.dp, if (focused) Palette.Yellow else Palette.Rim)
            .onFocusChanged { if (it.isFocused) onFocus?.invoke(channel) }
            .clickable(interactionSource = interaction, indication = null) { onSelect(channel) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.width(channelColumn).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                channel.dialLabel,
                style = spelunkrText(if (isTv) 26.sp else 22.sp, Palette.Yellow),
                modifier = Modifier.width(if (channel.dial >= 100) 54.dp else 40.dp),
            )
            PixelLogo(channel, if (isTv) 26.dp else 24.dp)
            if (isTv) {
                Column(Modifier.padding(start = 6.dp)) {
                    Text(channel.name, style = spelunkrText(13.sp, Palette.Yellow), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    FamilyTag(channel.family)
                }
            }
        }
        Row(Modifier.weight(1f).fillMaxHeight()) {
            if (schedule == null) {
                Text(
                    "ADJUSTING THE ANTENNA…",
                    style = spelunkrText(13.sp, Palette.Muted),
                    modifier = Modifier.align(Alignment.CenterVertically).padding(start = 8.dp),
                )
            } else {
                schedule.airings(start, end).forEach { airing ->
                    val shown = minOf(airing.endMs, end) - maxOf(airing.startMs, start)
                    Column(
                        Modifier.weight(shown.toFloat()).fillMaxHeight().border(0.5.dp, Palette.Rim).padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            airing.program.title + (airing.program.year?.let { " $it" } ?: ""),
                            style = spelunkrText(13.sp, shadow = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(runtime(airing.durationMs), style = spelunkrText(10.sp, Palette.Muted, shadow = false), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun Ticker(channelCount: Int) {
    Row(
        Modifier.fillMaxWidth().background(Palette.PanelDeep).border(1.dp, Palette.Rim).padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text("$channelCount CHANNELS · ♥ DONATE TO THE ARCHIVE", style = spelunkrText(14.sp))
        Spacer(Modifier.weight(1f))
        Text("Like cable TV, but weirder. SPELUNKR.TV", style = spelunkrText(14.sp, Palette.Yellow))
    }
}
