package com.example.iptvmate

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerOverlayTest {

    @Test
    fun testProgressCalculation() {
        val now = 1000000L
        val start = 900000L // 100s ago
        val stop = 1100000L // 100s in future (total duration 200s)

        val progress = if (stop > start) {
            (((now - start).toDouble() / (stop - start).toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }
        assertEquals(50, progress)
    }

    @Test
    fun testChannelSwitchingIndex() {
        val channels = listOf(
            Channel(name = "C1", streamUrl = "u1", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = null, orderIndex = 0),
            Channel(name = "C2", streamUrl = "u2", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = null, orderIndex = 1),
            Channel(name = "C3", streamUrl = "u3", tvgId = null, tvgName = null, tvgLogo = null, groupTitle = null, orderIndex = 2)
        )

        var currentIndex = 1 // C2

        // Next channel (Down)
        currentIndex = (currentIndex + 1) % channels.size
        assertEquals(2, channels[currentIndex].orderIndex)

        // Previous channel (Up)
        currentIndex = (currentIndex - 1 + channels.size) % channels.size
        assertEquals(1, channels[currentIndex].orderIndex)
    }
}
