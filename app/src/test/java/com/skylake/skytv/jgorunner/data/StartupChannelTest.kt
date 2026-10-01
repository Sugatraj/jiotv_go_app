package com.skylake.skytv.jgorunner.data

import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StartupChannelTest {
    private val first = OmniChannel(id = "first", name = "First", url = "http://localhost/live/first.m3u8")
    private val second = OmniChannel(id = "second", name = "Second", url = "http://localhost/live/second.m3u8")
    private val channels = listOf(first, second)

    @Test
    fun migratesLegacyAutoplayPreferences() {
        assertEquals(
            LegacyStartupSelection(StartupChannelMode.LAST_PLAYED, true),
            migrateLegacyStartupSelection(first = true, last = true)
        )
        assertEquals(
            LegacyStartupSelection(StartupChannelMode.LEGACY_FIRST, false),
            migrateLegacyStartupSelection(first = true, last = false)
        )
        assertEquals(
            LegacyStartupSelection(StartupChannelMode.NONE, false),
            migrateLegacyStartupSelection(first = false, last = false)
        )
    }

    @Test
    fun prefersTvgIdAndUsesOnlyLocalRouteIdsAsFallback() {
        assertEquals("guide.id", stableChannelId(" guide.id ", "http://localhost/live/route.m3u8"))
        assertEquals("route", stableChannelId(null, "http://localhost:5350/live/route.m3u8?token=x"))
        assertEquals("play-id", stableChannelId(null, "http://127.0.0.1:5350/play/play-id.m3u8"))
        assertNull(stableChannelId(null, "https://example.com/live/route.m3u8"))
        assertNull(stableChannelId(null, "http://notlocalhost.example/live/route.m3u8"))
    }

    @Test
    fun fixedChannelRequiresExactSavedId() {
        assertSame(second, resolveFixedChannel(channels, "second"))
        assertNull(resolveFixedChannel(channels, "removed"))
        assertNull(resolveFixedChannel(channels, null))
    }

    @Test
    fun lastPlayedUsesSavedIdBeforeLegacyDetails() {
        assertSame(
            second,
            resolveLastPlayedChannel(channels, "second", "First", first.url)
        )
        assertNull(resolveLastPlayedChannel(channels, "removed", "First", first.url))
        assertSame(first, resolveLastPlayedChannel(channels, null, "first", null))
        assertSame(second, resolveLastPlayedChannel(channels, null, null, second.url))
    }
}
