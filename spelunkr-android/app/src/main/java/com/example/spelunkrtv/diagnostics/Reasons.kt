package com.example.spelunkrtv.diagnostics

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import javax.net.ssl.SSLException

/**
 * Plain words for why talking to [who] failed, found by walking the cause chain; null when
 * nothing in the chain was a network failure.
 */
fun networkReason(error: Throwable, who: String): String? {
    for (e in generateSequence(error) { it.cause }.take(12)) {
        when (e) {
            is UnknownHostException ->
                return "Can't find $who: there is no internet, or this network blocks it"
            is SocketTimeoutException ->
                return "$who didn't answer in time"
            is NoRouteToHostException, is ConnectException ->
                return "Couldn't connect to $who: it refused, or this network blocks it"
            is SSLException, is GeneralSecurityException ->
                return "The secure connection to $who failed. Check that the date and time are set automatically"
        }
    }
    return null
}

/** Plain words for an HTTP status that is not a success. */
fun httpReason(code: Int, who: String): String = when (code) {
    401, 403 -> "$who refused this tape (HTTP $code). It may be restricted or taken down"
    404, 410 -> "$who no longer has this tape (HTTP $code)"
    429 -> "$who says too many requests (HTTP 429). Wait a minute and try again"
    // archive.org also answers 503 for an item that no longer exists, so don't claim an outage.
    503 -> "$who says this tape is unavailable right now (HTTP 503): it's busy, or the tape is gone"
    in 500..599 -> "$who is having trouble right now (HTTP $code)"
    else -> "$who answered HTTP $code"
}
