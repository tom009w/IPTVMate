package com.example.iptvmate

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

object M3UParser {

    fun parse(inputStream: InputStream, playlistUrl: String? = null): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var orderIndex = 0
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentTvgLogo: String? = null
        var currentGroupTitle: String? = null
        var currentDrmScheme: String? = null
        var currentLicenseUrl: String? = null
        var currentName: String = "Unknown Channel"

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("#EXTM3U")) {
                    continue
                }
                if (trimmed.startsWith("#EXTINF:")) {
                    currentTvgId = extractAttribute(trimmed, "tvg-id")
                    currentTvgName = extractAttribute(trimmed, "tvg-name")
                    currentTvgLogo = extractAttribute(trimmed, "tvg-logo")
                    currentGroupTitle = extractAttribute(trimmed, "group-title")
                    currentDrmScheme = extractAttribute(trimmed, "drm-scheme")
                    currentLicenseUrl = extractAttribute(trimmed, "license-url")

                    val commaIndex = trimmed.lastIndexOf(',')
                    if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        currentName = trimmed.substring(commaIndex + 1).trim()
                    } else {
                        currentName = "Channel ${orderIndex + 1}"
                    }
                } else if (trimmed.startsWith("#KODIPROP:") || trimmed.startsWith("#EXTVLCOPT:")) {
                    if (trimmed.contains("license_type", ignoreCase = true) || trimmed.contains("drm", ignoreCase = true)) {
                        if (trimmed.contains("widevine", ignoreCase = true)) {
                            currentDrmScheme = "widevine"
                        }
                    }
                    if (trimmed.contains("license_key", ignoreCase = true) || trimmed.contains("license", ignoreCase = true)) {
                        val parts = trimmed.split("=", limit = 2)
                        if (parts.size == 2) {
                            currentLicenseUrl = parts[1].trim()
                        }
                    }
                } else if (trimmed.startsWith("#")) {
                    continue
                } else {
                    var streamUrl = trimmed
                    if (playlistUrl != null && !streamUrl.startsWith("http://", ignoreCase = true) && !streamUrl.startsWith("https://", ignoreCase = true)) {
                        streamUrl = resolveRelativeUrl(playlistUrl, streamUrl)
                    }
                    channels.add(
                        Channel(
                            name = currentName,
                            streamUrl = streamUrl,
                            tvgId = currentTvgId,
                            tvgName = currentTvgName,
                            tvgLogo = currentTvgLogo,
                            groupTitle = currentGroupTitle ?: "Uncategorized",
                            orderIndex = orderIndex++,
                            drmScheme = currentDrmScheme,
                            licenseUrl = currentLicenseUrl,
                            licenseHeaders = null
                        )
                    )
                    currentTvgId = null
                    currentTvgName = null
                    currentTvgLogo = null
                    currentGroupTitle = null
                    currentDrmScheme = null
                    currentLicenseUrl = null
                    currentName = "Unknown Channel"
                }
            }
        }
        return channels
    }

    private fun extractAttribute(line: String, attr: String): String? {
        val regex = Regex("$attr=\"([^\"]*)\"")
        val match = regex.find(line)
        if (match != null) return match.groupValues[1]

        val regexNoQuote = Regex("$attr=([^\\s,]*)")
        val matchNoQuote = regexNoQuote.find(line)
        return matchNoQuote?.groupValues?.get(1)
    }

    private fun resolveRelativeUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            val base = URL(baseUrl)
            URL(base, relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }
}
