package com.example.iptvmate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream

class XmltvParserTest {

    @Test
    fun testXmltvParsingAndMatching() {
        val xmlContent = """
            <tv>
              <channel id="bbc1">
                <display-name>BBC One</display-name>
              </channel>
              <programme start="20231025120000 +0000" stop="20231025130000 +0000" channel="bbc1">
                <title>BBC News at Noon</title>
              </programme>
            </tv>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        val epgMap = XmltvParser.parse(inputStream)

        assertEquals(1, epgMap.size)
        val channelData = epgMap["bbc1"]
        assertNotNull(channelData)
        assertEquals("BBC One", channelData?.displayName)
        assertEquals(1, channelData?.programs?.size)
        assertEquals("BBC News at Noon", channelData?.programs?.get(0)?.title)

        // Test time handling
        val parsedTime = XmltvParser.parseXmltvTime("20231025120000 +0000")
        assert(parsedTime > 0L)

        // Test channel matching
        val m3uChannels = listOf(
            Channel(
                name = "BBC One HD",
                streamUrl = "http://example.com/bbc1.m3u8",
                tvgId = "bbc1",
                tvgName = "BBC One",
                tvgLogo = null,
                groupTitle = "News",
                orderIndex = 0
            )
        )

        val matchedEpg = EpgMatcher.matchEpg(m3uChannels, epgMap)
        assertNotNull(matchedEpg[0])
    }

    @Test
    fun testMissingDataAndInvalidTime() {
        val xmlContent = """
            <tv>
              <channel id="invalid">
              </channel>
              <programme start="invalid_time" stop="" channel="invalid">
                <title></title>
              </programme>
            </tv>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        val epgMap = XmltvParser.parse(inputStream)
        assertEquals(1, epgMap.size)

        val time = XmltvParser.parseXmltvTime("invalid_time")
        assertEquals(0L, time)
    }
}
