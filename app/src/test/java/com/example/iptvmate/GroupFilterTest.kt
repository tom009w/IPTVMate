package com.example.iptvmate

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupFilterTest {

    @Test
    fun testExtractGroups() {
        val channels = listOf(
            Channel(name = "C1", streamUrl = "url1", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = "Sports", orderIndex = 0),
            Channel(name = "C2", streamUrl = "url2", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = "News", orderIndex = 1),
            Channel(name = "C3", streamUrl = "url3", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = null, orderIndex = 2), // empty/missing group-title
            Channel(name = "C4", streamUrl = "url4", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = "  ", orderIndex = 3)  // blank group-title
        )

        val groups = GroupExtractor.extractGroups(channels)
        // Without favorites, should contain "All Channels", "News", "Sports", "Uncategorized" (size 4)
        assertEquals(4, groups.size)
        assertEquals("All Channels", groups[0])
        assertEquals("News", groups[1])
        assertEquals("Sports", groups[2])
        assertEquals("Uncategorized", groups[3])

        // With favorites
        val channelsWithFavs = listOf(
            Channel(name = "C1", streamUrl = "url1", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = "Sports", orderIndex = 0, isFavorite = true),
            Channel(name = "C2", streamUrl = "url2", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = "News", orderIndex = 1)
        )
        val groupsWithFavs = GroupExtractor.extractGroups(channelsWithFavs)
        assertEquals(4, groupsWithFavs.size)
        assertEquals("All Channels", groupsWithFavs[0])
        assertEquals("Favorites", groupsWithFavs[1])
    }

    @Test
    fun testFilterChannels() {
        val channels = listOf(
            Channel(name = "C1", streamUrl = "url1", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = "Sports", orderIndex = 0, isFavorite = true),
            Channel(name = "C2", streamUrl = "url2", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = "News", orderIndex = 1, isFavorite = false),
            Channel(name = "C3", streamUrl = "url3", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = null, orderIndex = 2, isFavorite = false)
        )

        // All Channels
        val all = GroupExtractor.filterChannels(channels, "All Channels")
        assertEquals(3, all.size)

        // Favorites
        val favs = GroupExtractor.filterChannels(channels, "Favorites")
        assertEquals(1, favs.size)
        assertEquals("C1", favs[0].name)

        // Sports
        val sports = GroupExtractor.filterChannels(channels, "Sports")
        assertEquals(1, sports.size)
        assertEquals("C1", sports[0].name)

        // Uncategorized (null groupTitle)
        val uncat = GroupExtractor.filterChannels(channels, "Uncategorized")
        assertEquals(1, uncat.size)
        assertEquals("C3", uncat[0].name)
    }
}
