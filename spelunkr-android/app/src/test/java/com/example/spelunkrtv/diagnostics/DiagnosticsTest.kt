package com.example.spelunkrtv.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLHandshakeException

class DiagnosticsTest {
    @Test
    fun namesNetworkFailuresFoundAnywhereInTheCauseChain() {
        val wrapped = IOException("load failed", IOException("open failed", UnknownHostException("Unable to resolve host \"archive.org\"")))
        assertEquals("Can't find archive.org: there is no internet, or this network blocks it", networkReason(wrapped, "archive.org"))
        assertEquals("archive.org didn't answer in time", networkReason(SocketTimeoutException(), "archive.org"))
        assertTrue(networkReason(ConnectException(), "archive.org")!!.startsWith("Couldn't connect to archive.org"))
        assertTrue(networkReason(SSLHandshakeException("bad"), "archive.org")!!.contains("date and time"))
        assertTrue(networkReason(IOException(CertPathValidatorException("expired")), "archive.org")!!.contains("date and time"))
    }

    @Test
    fun leavesNonNetworkFailuresToTheCaller() {
        assertNull(networkReason(IllegalStateException("decoder"), "archive.org"))
    }

    @Test
    fun explainsHttpStatuses() {
        assertEquals("archive.org no longer has this tape (HTTP 404)", httpReason(404, "archive.org"))
        assertTrue(httpReason(403, "archive.org").contains("refused"))
        assertTrue(httpReason(503, "archive.org").contains("unavailable"))
        assertTrue(httpReason(502, "archive.org").contains("having trouble"))
        assertEquals("archive.org answered HTTP 302", httpReason(302, "archive.org"))
    }

    @Test
    fun formatsRatesSizesAndSpans() {
        assertEquals("850 kbit/s", formatBits(850_000))
        assertEquals("1.2 Mbit/s", formatBits(1_234_567))
        assertEquals("820 KB", formatBytes(820_400))
        assertEquals("12.4 MB", formatBytes(12_400_000))
        assertEquals("2 s", formatSpan(2_400))
        assertEquals("3 min 4 s", formatSpan(-184_000))
        assertEquals("3 h 12 min", formatSpan((3 * 3600 + 12 * 60) * 1000L))
        assertEquals("41 days", formatSpan(41L * 86_400_000))
    }
}
