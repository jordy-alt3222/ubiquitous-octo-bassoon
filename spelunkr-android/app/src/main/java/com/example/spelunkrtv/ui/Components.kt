package com.example.spelunkrtv.ui

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.spelunkrtv.data.Channel
import com.example.spelunkrtv.diagnostics.formatBits
import com.example.spelunkrtv.diagnostics.formatBytes
import com.example.spelunkrtv.player.LivePlayer
import com.example.spelunkrtv.player.StreamLink
import kotlinx.coroutines.delay

/**
 * A panel button that works the same under a finger and a D-pad: Compose's clickable takes
 * DPAD_CENTER/ENTER, and focus shows as the site's yellow rim.
 */
@Composable
fun SpelunkrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gold: Boolean = false,
    enabled: Boolean = true,
) {
    val isTv = LocalIsTv.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val fill: Brush = if (gold) Palette.Gold else SolidColor(Palette.PanelDeep)
    Box(
        modifier
            .background(fill)
            .border(if (focused) 3.dp else 1.dp, if (focused) Palette.Yellow else Palette.Rim)
            .remoteFocusable()
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (isTv) 14.dp else 12.dp, vertical = if (isTv) 4.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = spelunkrText(if (isTv) 16.sp else 15.sp, if (gold || focused) Palette.Yellow else Color.White),
            maxLines = 1,
            color = if (enabled) Color.Unspecified else Palette.Muted,
        )
    }
}

/**
 * Compose lets a clickable take focus only in key mode. A TV can start in touch mode, so on TV
 * every focus stop is made focusable outright: the guide can then land on the tuned channel
 * before the first button press.
 */
@Composable
fun Modifier.remoteFocusable(): Modifier =
    if (LocalIsTv.current) focusProperties { canFocus = true } else this

/** Focus something a lazy list may not have laid out yet: keep asking for a few frames. */
suspend fun FocusRequester.requestFocusSoon() {
    repeat(30) {
        withFrameNanos { }
        if (runCatching { requestFocus() }.getOrDefault(false)) return
    }
}

/** Navy panel with the blue rim the site draws around every box. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .background(Palette.Panel)
            .border(3.dp, Palette.Rim)
            .padding(10.dp),
    ) { content() }
}

/**
 * A channel's pixel logomark from the lineup. The site colours the letters from the channel's
 * own colour; this palette is the app's approximation of that (c rim, k field, the rest accents).
 */
@Composable
fun PixelLogo(channel: Channel, size: Dp, modifier: Modifier = Modifier) {
    val rows = channel.logoRows
    val base = Color(channel.color)
    if (rows.isEmpty()) {
        Box(
            modifier.size(size).background(base).border(1.dp, Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(channel.name.take(1), style = spelunkrText((size.value * 0.55f).sp, Color.White))
        }
        return
    }
    val colors = mapOf(
        'c' to base,
        'k' to lerpColor(base, Color.Black, 0.72f),
        'd' to lerpColor(base, Color.Black, 0.35f),
        'p' to lerpColor(base, Color.White, 0.35f),
        'l' to lerpColor(base, Color.White, 0.75f),
        'g' to Color.White,
    )
    Canvas(modifier.size(size)) {
        val cell = this.size.width / rows.maxOf { it.length }
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = colors[ch] ?: if (ch == '.') null else base
                if (color != null) {
                    drawRect(color, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
)

/** The video picture. One player feeds whichever screen is showing. */
@OptIn(UnstableApi::class)
@Composable
fun VideoSurface(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                setKeepContentOnPlayerReset(false)
                // The remote drives the app, not the view: keep focus out of it.
                isFocusable = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                (videoSurfaceView as? SurfaceView)?.isFocusable = false
                this.player = player
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null },
        modifier = modifier.background(Color.Black),
    )
}

/**
 * ONE MOMENT PLEASE / TUNING... over the picture while a tape loads from the Archive, with what
 * the line to the Archive is doing underneath, so a slow tape and a dead line look different.
 */
@Composable
fun TuningSlate(channel: Channel?, link: StreamLink, modifier: Modifier = Modifier, large: Boolean = false) {
    var dots by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400); dots = dots % 3 + 1
        }
    }
    Column(
        modifier.fillMaxSize().background(Palette.PanelDeep),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ONE MOMENT PLEASE", style = spelunkrText(if (large) 40.sp else 22.sp, Palette.Yellow), textAlign = TextAlign.Center)
        Text("TUNING" + ".".repeat(dots), style = spelunkrText(if (large) 22.sp else 14.sp))
        if (channel != null) {
            Text("CH ${channel.dialLabel} · ${channel.name}", style = spelunkrText(if (large) 18.sp else 12.sp, Palette.Muted))
        }
        val small = spelunkrText(if (large) 16.sp else 10.sp, Color.White, shadow = false)
        val waited = link.waitingMs / 1000
        Text(
            if (link.bytes == 0L) "CALLING ARCHIVE.ORG… ${waited}s"
            else "RECEIVING FROM ARCHIVE.ORG · ${formatBytes(link.bytes)} · ${formatBits(link.bitsPerSecond)}",
            style = small,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (large) 16.dp else 6.dp, start = 8.dp, end = 8.dp),
        )
        if (waited >= SLOW_ARCHIVE_S) {
            // The site's own words for a slow Archive.
            Text(
                "THE ARCHIVE IS RUNNING SLOW. TAPES MAY TAKE A WHILE TO START.",
                style = small.copy(color = Palette.Yellow),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

private const val SLOW_ARCHIVE_S = 15

/** NO SIGNAL, with why in plain words and when the app tries again. */
@Composable
fun NoSignal(link: StreamLink, modifier: Modifier = Modifier, large: Boolean = false) {
    Column(
        modifier.fillMaxSize().background(Color.Black).padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("NO SIGNAL", style = spelunkrText(if (large) 44.sp else 22.sp, Color.White))
        val small = spelunkrText(if (large) 16.sp else 10.sp, Palette.Muted, shadow = false)
        link.error?.let { Text(it, style = small.copy(color = Color.White), textAlign = TextAlign.Center, maxLines = 3) }
        Text("TRYING AGAIN IN ${(link.retryInMs + 999) / 1000}s", style = small, modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * The line to the Internet Archive at a glance: green while video arrives or is safely
 * buffered, amber while the app waits for archive.org, red when it can't get a tape.
 */
@Composable
fun LinkIndicator(state: LivePlayer.State, modifier: Modifier = Modifier, large: Boolean = false) {
    val link = state.link
    val (color, text) = when {
        state.noSignal -> Palette.Live to "NO SIGNAL FROM ARCHIVE.ORG"
        state.tuning && !link.receiving && link.waitingMs >= 1_000 -> Palette.Warn to "WAITING FOR ARCHIVE.ORG ${link.waitingMs / 1000}s"
        link.receiving -> Palette.Ok to "ARCHIVE.ORG ▼ ${formatBits(link.bitsPerSecond)}"
        state.tuning -> Palette.Warn to "CALLING ARCHIVE.ORG"
        state.airing != null -> Palette.Ok to "ARCHIVE.ORG OK · ${link.bufferedMs / 1000}s BUFFERED"
        else -> Palette.Muted to "NOT TUNED"
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (large) 12.dp else 8.dp).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = spelunkrText(if (large) 15.sp else 11.sp, color, shadow = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** LIVE, or how far behind live the viewer is after pausing or rewinding. */
@Composable
fun LiveBadge(live: Boolean, behindMs: Long, paused: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            paused -> Text("PAUSED", style = spelunkrText(14.sp, Palette.Muted))
            live -> {
                Box(Modifier.size(10.dp).background(Palette.Live))
                Spacer(Modifier.width(6.dp))
                Text("LIVE", style = spelunkrText(14.sp))
            }
            else -> Text("-${runtime(behindMs)}", style = spelunkrText(14.sp, Palette.Muted))
        }
    }
}

@Composable
fun FamilyTag(family: String?) {
    if (family.isNullOrBlank()) return
    Text(
        family.uppercase().replace('-', ' '),
        style = spelunkrText(11.sp, Palette.Muted, shadow = false),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier.padding(start = 6.dp),
    )
}
