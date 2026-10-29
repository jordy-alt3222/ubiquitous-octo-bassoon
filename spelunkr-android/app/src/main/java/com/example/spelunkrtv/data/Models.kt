package com.example.spelunkrtv.data

import kotlinx.serialization.Serializable

/*
 * Wire format of the public broadcast files that spelunkr.tv itself reads:
 *   lineup.json                    every channel and its published schedule versions
 *   manifests/<channel>/v<n>.json  the tapes a version plays, in order
 *   details/<channel>/v<n>.json    one description per tape, same order as the manifest
 * Unknown keys are ignored, so new fields on the site do not break the app.
 */

@Serializable
data class Lineup(
    val generatedAt: String? = null,
    val channels: List<ChannelDto> = emptyList(),
)

@Serializable
data class ChannelDto(
    val id: String,
    val name: String,
    val emoji: String = "",
    val color: String = "#888888",
    val description: String = "",
    val family: String? = null,
    val logomark: Logomark? = null,
    val dial: Int? = null,
    val debugOnly: Boolean = false,
    val comingSoon: Boolean = false,
    val stats: Stats? = null,
    val versions: List<VersionRef> = emptyList(),
)

@Serializable
data class Logomark(val rows: List<String> = emptyList())

@Serializable
data class Stats(val items: Int = 0, val videoSeconds: Long = 0)

@Serializable
data class VersionRef(
    val v: Int,
    val manifest: String,
    val details: String? = null,
    val epochMs: Long,
    val effectiveAtMs: Long? = null,
    val startIndex: Int = 0,
)

@Serializable
data class Manifest(
    val channel: String,
    val version: Int,
    val items: List<ManifestItem> = emptyList(),
)

@Serializable
data class ManifestItem(
    /** Internet Archive item identifier. */
    val id: String,
    val file: String? = null,
    val mp4: String? = null,
    /** Seconds on air, already trimmed to [inS]..[outS]. */
    val duration: Int,
    val inS: Int? = null,
    val outS: Int? = null,
    val title: String? = null,
    val year: String? = null,
)

@Serializable
data class Details(val items: List<ItemDetail> = emptyList())

@Serializable
data class ItemDetail(val description: String? = null)
