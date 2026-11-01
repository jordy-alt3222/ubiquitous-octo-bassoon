package com.example.spelunkrtv.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TV SETUP. None of these lower the picture or sound: the Archive keeps one copy of each tape,
 * and the app always plays that copy untouched. They change how it is fetched and shown.
 */
data class TvSetup(
    val shape: PictureShape = PictureShape.FIT,
    val matchFrameRate: Boolean = true,
    val tunneling: Boolean = false,
    val sound: Sound = Sound.SURROUND,
    val connections: Int = 4,
    val smoothness: Smoothness = Smoothness.BALANCED,
    val preload: Preload = Preload.NEXT_DOOR,
    val server: Server = Server.FASTEST,
    /** Channels the site has scheduled but not yet put on its dial. */
    val previewChannels: Boolean = true,
) {
    enum class PictureShape { FIT, FILL, STRETCH }
    enum class Sound { SURROUND, STEREO }
    enum class Smoothness { START_FAST, BALANCED, SMOOTHEST }
    enum class Preload { OFF, NEXT_DOOR, WHOLE_DIAL }
    enum class Server { FASTEST, ARCHIVE }

    companion object {
        val CONNECTION_CHOICES = listOf(1, 2, 4, 6)
    }
}

/** Keeps TV SETUP on the device. */
class SetupStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tv-setup", Context.MODE_PRIVATE)
    private val _setup = MutableStateFlow(read())
    val setup: StateFlow<TvSetup> = _setup.asStateFlow()

    fun update(change: (TvSetup) -> TvSetup) {
        val next = change(_setup.value)
        _setup.value = next
        prefs.edit()
            .putString("shape", next.shape.name)
            .putBoolean("matchFrameRate", next.matchFrameRate)
            .putBoolean("tunneling", next.tunneling)
            .putString("sound", next.sound.name)
            .putInt("connections", next.connections)
            .putString("smoothness", next.smoothness.name)
            .putString("preload", next.preload.name)
            .putString("server", next.server.name)
            .putBoolean("previewChannels", next.previewChannels)
            .apply()
    }

    private fun read(): TvSetup {
        val d = TvSetup()
        return TvSetup(
            shape = enumOr(prefs.getString("shape", null), d.shape),
            matchFrameRate = prefs.getBoolean("matchFrameRate", d.matchFrameRate),
            tunneling = prefs.getBoolean("tunneling", d.tunneling),
            sound = enumOr(prefs.getString("sound", null), d.sound),
            connections = prefs.getInt("connections", d.connections).takeIf { it in TvSetup.CONNECTION_CHOICES } ?: d.connections,
            smoothness = enumOr(prefs.getString("smoothness", null), d.smoothness),
            preload = enumOr(prefs.getString("preload", null), d.preload),
            server = enumOr(prefs.getString("server", null), d.server),
            previewChannels = prefs.getBoolean("previewChannels", d.previewChannels),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
