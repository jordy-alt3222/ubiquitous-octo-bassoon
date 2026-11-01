package com.example.spelunkrtv.data

import android.content.Context
import com.example.spelunkrtv.BuildConfig
import com.example.spelunkrtv.schedule.ChannelSchedule

/** Where an app's channels and their schedules come from. */
interface Broadcast {
    /** Wall-clock time, corrected when the device's clock is clearly off. */
    fun now(): Long
    suspend fun channels(withPreviews: Boolean = false): List<Channel>
    suspend fun schedule(channel: Channel): ChannelSchedule
}

/** Which app this build is: SPELUNKR (spelunkr.tv's channels) or ru-splunker (Soviet & retro Russian channels). */
object Brand {
    val isRu: Boolean = BuildConfig.LINEUP == "ru"
    val name: String = if (isRu) "RU-SPLUNKER" else "SPELUNKR"
    val guideName: String = if (isRu) "Ru-splunker" else "Spelunkr"
    val tagline: String = if (isRu) "Soviet TV, film and cartoons, 1922–1991, from the Internet Archive." else "Like cable TV, but weirder. SPELUNKR.TV"
    val about: String = if (isRu) {
        "A free alternative TV network\nof the USSR's films, cartoons and television, from the Internet Archive."
    } else {
        "We are a free alternative TV network\nbuilt from the Internet Archive's forgotten videos."
    }

    fun broadcast(context: Context): Broadcast = if (isRu) RuBroadcast(context) else BroadcastRepository()
}
