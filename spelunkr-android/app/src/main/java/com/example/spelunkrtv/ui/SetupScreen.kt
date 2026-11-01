package com.example.spelunkrtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelunkrtv.Screen
import com.example.spelunkrtv.data.TvSetup
import com.example.spelunkrtv.data.TvSetup.PictureShape
import com.example.spelunkrtv.data.TvSetup.Preload
import com.example.spelunkrtv.data.TvSetup.Server
import com.example.spelunkrtv.data.TvSetup.Smoothness
import com.example.spelunkrtv.data.TvSetup.Sound
import com.example.spelunkrtv.diagnostics.formatBytes

/** TV SETUP: how tapes are fetched and shown. None of it lowers their quality. */
@Composable
fun SetupScreen(
    setup: TvSetup,
    savedIndexBytes: Long,
    now: Long,
    onNavigate: (Screen) -> Unit,
    onChange: ((TvSetup) -> TvSetup) -> Unit,
    onClearSaved: () -> Unit,
) {
    val isTv = LocalIsTv.current
    val firstChoice = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (isTv) firstChoice.requestFocusSoon() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Sky)) {
        val wide = maxWidth >= 720.dp
        Column(Modifier.fillMaxSize()) {
            TopBar(now, Screen.Setup, onNavigate, showClock = wide)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("TV SETUP", style = spelunkrText(24.sp, Palette.Yellow))
                Text(
                    "None of this lowers the picture or sound. The Archive keeps one copy of each tape, and the app always " +
                        "plays that copy untouched; these change how it is fetched and shown.",
                    style = spelunkrText(13.sp, shadow = false),
                )

                Section("PICTURE") {
                    Choice(
                        "SHAPE",
                        "How a tape fills the screen when you watch. FIT shows all of it. FILL crops the edges to fill the " +
                            "screen. STRETCH fills it by stretching.",
                        listOf(PictureShape.FIT to "FIT", PictureShape.FILL to "FILL", PictureShape.STRETCH to "STRETCH"),
                        setup.shape, wide, firstChoice,
                    ) { v -> onChange { it.copy(shape = v) } }
                    Choice(
                        "MATCH FRAME RATE",
                        "Lets the TV switch to the tape's own frame rate (25, 29.97, 50…) when it can without a blackout, " +
                            "for smoother motion. The TV's own \"match content frame rate\" setting has the last word.",
                        listOf(true to "ON", false to "OFF"),
                        setup.matchFrameRate, wide,
                    ) { v -> onChange { it.copy(matchFrameRate = v) } }
                    Choice(
                        "TUNNELED PLAYBACK",
                        "Hands picture timing to the TV's video hardware. Smoother on many TVs; if the picture stays black, " +
                            "turn it off.",
                        listOf(false to "OFF", true to "ON"),
                        setup.tunneling, wide,
                    ) { v -> onChange { it.copy(tunneling = v) } }
                }

                Section("SOUND") {
                    Choice(
                        "AUDIO",
                        "SURROUND passes Dolby (AC-3) through to your TV or soundbar when a tape has it. STEREO always plays " +
                            "the two-channel track.",
                        listOf(Sound.SURROUND to "SURROUND", Sound.STEREO to "STEREO"),
                        setup.sound, wide,
                    ) { v -> onChange { it.copy(sound = v) } }
                }

                Section("STREAMING · FASTER, SAME QUALITY") {
                    Choice(
                        "DOWNLOAD BOOST",
                        "Fetches each tape over several connections at once. Busy Archive servers give each connection " +
                            "only a trickle, so more connections means quicker tuning and fewer pauses.",
                        TvSetup.CONNECTION_CHOICES.map { it to if (it == 1) "OFF" else "$it" },
                        setup.connections, wide,
                    ) { v -> onChange { it.copy(connections = v) } }
                    Choice(
                        "SMOOTHNESS",
                        "START FAST plays after 1 s. BALANCED keeps two minutes ready. SMOOTHEST waits 5 s and keeps up " +
                            "to five minutes ready, so a slow line pauses rarely.",
                        listOf(Smoothness.START_FAST to "START FAST", Smoothness.BALANCED to "BALANCED", Smoothness.SMOOTHEST to "SMOOTHEST"),
                        setup.smoothness, wide,
                    ) { v -> onChange { it.copy(smoothness = v) } }
                    Choice(
                        "PRELOAD",
                        "Fetches tape indexes ahead so changing channel is quick. NEXT DOOR: the next show and the channels " +
                            "either side. WHOLE DIAL: every channel (more data, up to 1 GB of storage).",
                        listOf(Preload.OFF to "OFF", Preload.NEXT_DOOR to "NEXT DOOR", Preload.WHOLE_DIAL to "WHOLE DIAL"),
                        setup.preload, wide,
                    ) { v -> onChange { it.copy(preload = v) } }
                    Choice(
                        "SERVER",
                        "Every tape is kept on two Archive servers. FASTEST times both and plays from the quicker. " +
                            "ARCHIVE.ORG lets archive.org choose, as the website does.",
                        listOf(Server.FASTEST to "FASTEST", Server.ARCHIVE to "ARCHIVE.ORG"),
                        setup.server, wide,
                    ) { v -> onChange { it.copy(server = v) } }
                }

                if (!com.example.spelunkrtv.data.Brand.isRu) Section("CHANNELS") {
                    Choice(
                        "PREVIEW CHANNELS",
                        "Channels the site has scheduled but not yet put on its dial (now WARP ZONE and WAKU WAKU). " +
                            "They play like any other and sit in the PREVIEW category.",
                        listOf(true to "SHOW", false to "HIDE"),
                        setup.previewChannels, wide,
                    ) { v -> onChange { it.copy(previewChannels = v) } }
                }

                Section("TOOLS") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpelunkrButton("SIGNAL CHECK", { onNavigate(Screen.Signal) })
                        SpelunkrButton("CLEAR SAVED INDEXES (${formatBytes(savedIndexBytes)})", onClearSaved)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Palette.Panel).border(3.dp, Palette.Rim).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = spelunkrText(16.sp, Palette.Yellow))
        content()
    }
}

/** One setting: what it does, and its choices with the current one in gold. */
@Composable
private fun <T> Choice(
    title: String,
    help: String,
    options: List<Pair<T, String>>,
    selected: T,
    wide: Boolean,
    focus: FocusRequester? = null,
    onPick: (T) -> Unit,
) {
    val keys = @Composable {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (value, label) ->
                SpelunkrButton(
                    label,
                    { onPick(value) },
                    gold = value == selected,
                    modifier = if (focus != null && value == selected) Modifier.focusRequester(focus) else Modifier,
                )
            }
        }
    }
    val words = @Composable { m: Modifier ->
        Column(m) {
            Text(title, style = spelunkrText(14.sp, Palette.Cyan))
            Text(help, style = spelunkrText(12.sp, shadow = false))
        }
    }
    if (wide) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            words(Modifier.weight(1f).padding(end = 12.dp))
            keys()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            words(Modifier)
            keys()
        }
    }
}
