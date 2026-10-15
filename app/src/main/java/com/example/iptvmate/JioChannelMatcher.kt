package com.example.iptvmate

import android.util.Log

object JioChannelMatcher {

    private const val TAG = "JioChannelMatcher"

    fun matchChannels(m3uChannels: List<Channel>, apiChannels: List<JioChannelItem>): List<Channel> {
        val apiById = apiChannels.associateBy { it.channel_id?.trim() }
        val apiByName = apiChannels.groupBy { normalizeName(it.channel_name) }

        var matchedDrmCount = 0
        val sampleMatches = mutableListOf<String>()

        val matchedChannels = m3uChannels.map { channel ->
            var matchedItem: JioChannelItem? = null

            // 1. Match M3U entries using tvg-id against channel_id first
            if (!channel.tvgId.isNullOrBlank()) {
                matchedItem = apiById[channel.tvgId.trim()]
            }

            // 2. Use normalized tvg-name (or name fallback) against channel_name as a fallback
            if (matchedItem == null) {
                val lookupName = channel.tvgName.takeIf { !it.isNullOrBlank() } ?: channel.name
                val normName = normalizeName(lookupName)
                if (!normName.isNullOrBlank()) {
                    val candidates = apiByName[normName]
                    if (candidates != null && candidates.size == 1) {
                        matchedItem = candidates[0]
                    }
                }
            }

            if (matchedItem != null && !matchedItem.key_url.isNullOrBlank()) {
                matchedDrmCount++
                if (sampleMatches.size < 3) {
                    sampleMatches.add("${channel.name} -> ID: ${matchedItem.channel_id}")
                }
                channel.copy(
                    drmScheme = "widevine",
                    licenseUrl = matchedItem.key_url.trim()
                )
            } else {
                channel
            }
        }

        logDebug(TAG, "API Channels parsed: ${apiChannels.size}")
        logDebug(TAG, "M3U Channels matched with non-empty key_url: $matchedDrmCount")
        if (sampleMatches.isNotEmpty()) {
            logDebug(TAG, "Sample DRM matches: ${sampleMatches.joinToString(", ")}")
        }

        return matchedChannels
    }

    private fun logDebug(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (e: Throwable) {
            println("[$tag] $message")
        }
    }

    fun normalizeName(name: String?): String? {
        if (name.isNullOrBlank()) return null
        return name.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}
