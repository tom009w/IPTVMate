package com.example.iptvmate

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class M3UParserTest {

    @Test
    fun testValidM3UParse() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="ch1" tvg-name="Channel 1" tvg-logo="http://logo.png" group-title="News",BBC News
            http://example.com/stream1.m3u8
            #EXTINF:-1 group-title="Sports",ESPN
            http://example.com/stream2.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8))
        val channels = M3UParser.parse(inputStream)

        assertEquals(2, channels.size)

        assertEquals("BBC News", channels[0].name)
        assertEquals("http://example.com/stream1.m3u8", channels[0].streamUrl)
        assertEquals("ch1", channels[0].tvgId)
        assertEquals("Channel 1", channels[0].tvgName)
        assertEquals("http://logo.png", channels[0].tvgLogo)
        assertEquals("News", channels[0].groupTitle)
        assertEquals(0, channels[0].orderIndex)

        assertEquals("ESPN", channels[1].name)
        assertEquals("http://example.com/stream2.m3u8", channels[1].streamUrl)
        assertEquals(null, channels[1].tvgId)
        assertEquals("Sports", channels[1].groupTitle)
        assertEquals(1, channels[1].orderIndex)
    }

    @Test
    fun testRelativeUrlResolution() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1,Local Channel
            stream3.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8))
        val channels = M3UParser.parse(inputStream, "http://example.com/playlist/playlist.m3u")

        assertEquals(1, channels.size)
        assertEquals("Local Channel", channels[0].name)
        assertEquals("http://example.com/playlist/stream3.m3u8", channels[0].streamUrl)
    }

    @Test
    fun testMalformedAndEmptyEntries() {
        val m3uContent = """
            #EXTM3U
            
            # This is a comment
            #EXTINF:-1,Incomplete Info without url
            
            #EXTINF:-1,Valid Channel
            http://example.com/valid.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8))
        val channels = M3UParser.parse(inputStream)

        assertEquals(1, channels.size)
        assertEquals("Valid Channel", channels[0].name)
        assertEquals("http://example.com/valid.m3u8", channels[0].streamUrl)
    }

    @Test
    fun testDrmPlaylistParse() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 drm-scheme="widevine" license-url="http://example.com/license",Secure DASH Channel
            http://example.com/manifest.mpd
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8))
        val channels = M3UParser.parse(inputStream)

        assertEquals(1, channels.size)
        assertEquals("Secure DASH Channel", channels[0].name)
        assertEquals("http://example.com/manifest.mpd", channels[0].streamUrl)
        assertEquals("widevine", channels[0].drmScheme)
        assertEquals("http://example.com/license", channels[0].licenseUrl)
    }

    @Test
    fun testNonJioDrmPlaylistParse() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 group-title="Encrypted",Non-Jio DRM Channel
            #KODIPROP:inputstream.adaptive.license_type=widevine
            #KODIPROP:inputstream.adaptive.license_key=https://license.server.com/get|Authorization=Bearer secret_token&User-Agent=CustomAgent
            http://example.com/encrypted_manifest.mpd
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8))
        val channels = M3UParser.parse(inputStream)

        assertEquals(1, channels.size)
        assertEquals("Non-Jio DRM Channel", channels[0].name)
        assertEquals("http://example.com/encrypted_manifest.mpd", channels[0].streamUrl)
        assertEquals("widevine", channels[0].drmScheme)
        assertEquals("https://license.server.com/get", channels[0].licenseUrl)
        assertEquals("Bearer secret_token", channels[0].licenseHeaders?.get("Authorization"))
        assertEquals("CustomAgent", channels[0].licenseHeaders?.get("User-Agent"))
    }
}
