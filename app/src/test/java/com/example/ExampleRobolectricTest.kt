package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.iptv.M3UParser
import com.example.core.network.tablo.TabloChannelDetail
import com.example.core.network.tablo.TabloChannelMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AerioTV", appName)
    }

    @Test
    fun `m3u parser extracts channels correctly`() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="ESPN" tvg-name="ESPN HD" tvg-chno="206" group-title="Sports",ESPN
            http://example.com/live/espn.m3u8
        """.trimIndent()

        val channels = M3UParser.parse(m3uContent, "src_test", "Test Source")
        assertEquals(1, channels.size)
        assertEquals("ESPN", channels[0].name)
        assertEquals("206", channels[0].channelNumber)
        assertEquals("http://example.com/live/espn.m3u8", channels[0].streamUrl)
    }

    @Test
    fun `tablo channel meta formats display correctly`() {
        val meta = TabloChannelMeta(major = 4, minor = 1, network = "NBC", callSign = "WNBC-HD")
        assertEquals("4.1", meta.displayChannelNumber)
        assertEquals("NBC (WNBC-HD)", meta.displayTitle)
    }

    @Test
    fun `tablo client recognizes demo host identifier`() {
        val client = com.example.core.network.tablo.TabloClient("src_tablo_demo")
        assertTrue(client.isDemo)
    }
}
