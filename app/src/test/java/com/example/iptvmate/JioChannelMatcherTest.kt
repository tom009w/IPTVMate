package com.example.iptvmate

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JioChannelMatcherTest {

    @Test
    fun testJsonParsingAndMatchingStructure() {
        val jsonResponse = """
            {
              "code": 200,
              "message": "Success",
              "result": [
                {
                  "channel_id": "143",
                  "channel_name": "CNBC TV18 Prime",
                  "channel_url": "http://192.168.1.128:5350/live/mpd/143",
                  "key_url": "http://192.168.1.128:5350/live/key/143"
                },
                {
                  "channel_id": "144",
                  "channel_name": "Free Channel",
                  "channel_url": "http://192.168.1.128:5350/live/m3u8/144",
                  "key_url": null
                }
              ]
            }
        """.trimIndent()

        val response = Gson().fromJson(jsonResponse, JioChannelsResponse::class.java)
        assertEquals(200, response.code)
        assertEquals("Success", response.message)
        assertNotNull(response.result)
        assertEquals(2, response.result?.size)

        val apiChannels = response.result!!
        assertEquals("143", apiChannels[0].channel_id)
        assertEquals("CNBC TV18 Prime", apiChannels[0].channel_name)
        assertEquals("http://192.168.1.128:5350/live/key/143", apiChannels[0].key_url)

        val m3uChannels = listOf(
            Channel(id = 1, name = "CNBC TV18", streamUrl = "http://192.168.1.128:5350/live/mpd/143", tvgId = "143", tvgName = "CNBC TV18 Prime", tvgLogo = null, groupTitle = null, orderIndex = 0),
            Channel(id = 2, name = "Free Channel", streamUrl = "http://192.168.1.128:5350/live/m3u8/144", tvgId = null, tvgName = "Free Channel", tvgLogo = null, groupTitle = null, orderIndex = 1)
        )

        val matched = JioChannelMatcher.matchChannels(m3uChannels, apiChannels)

        assertEquals(2, matched.size)
        assertEquals("http://192.168.1.128:5350/live/key/143", matched[0].licenseUrl)
        assertEquals("widevine", matched[0].drmScheme)

        assertNull(matched[1].licenseUrl)
        assertNull(matched[1].drmScheme)
    }

    @Test
    fun testFallbackToNormalizedTvgName() {
        val m3uChannels = listOf(
            Channel(id = 1, name = "Display Name", streamUrl = "http://ex.com/1", tvgId = "wrong_id", tvgName = "Star Sports HD", tvgLogo = null, groupTitle = null, orderIndex = 0)
        )

        val apiChannels = listOf(
            JioChannelItem(channel_id = "other_id", channel_name = "Star Sports HD", channel_url = "http://ex.com/1", key_url = "http://license.com/star")
        )

        val matched = JioChannelMatcher.matchChannels(m3uChannels, apiChannels)

        assertEquals(1, matched.size)
        assertEquals("http://license.com/star", matched[0].licenseUrl)
        assertEquals("widevine", matched[0].drmScheme)
    }
}
