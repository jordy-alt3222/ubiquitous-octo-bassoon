package com.example.spelunkrtv.diagnostics

import java.util.Locale
import kotlin.math.abs

/** 850 kbit/s, 1.2 Mbit/s. */
fun formatBits(bitsPerSecond: Long): String = when {
    bitsPerSecond >= 1_000_000 -> String.format(Locale.US, "%.1f Mbit/s", bitsPerSecond / 1_000_000.0)
    else -> "${bitsPerSecond / 1_000} kbit/s"
}

/** 820 KB, 12.4 MB, 1.3 GB. */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
    else -> "${bytes / 1_000} KB"
}

/** 2 s, 3 min 4 s, 3 h 12 min, 41 days: for gaps and waits, sign ignored. */
fun formatSpan(ms: Long): String {
    val s = abs(ms) / 1000
    return when {
        s < 60 -> "$s s"
        s < 3_600 -> "${s / 60} min ${s % 60} s"
        s < 86_400 -> "${s / 3_600} h ${s % 3_600 / 60} min"
        else -> "${s / 86_400} days"
    }
}
