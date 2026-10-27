package com.example.iptvmate

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

data class ParsedPlaylist(
    val channels: List<Channel>,
    val embeddedEpgUrl: String? = null
)

object M3UParser {

    fun parsePlaylist(inputStream: InputStream, playlistUrl: String? = null, onChannelParsed: ((Int) -> Unit)? = null): ParsedPlaylist {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var embeddedEpgUrl: String? = null
        var orderIndex = 0
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentTvgLogo: String? = null
        var currentGroupTitle: String? = null
        var currentDrmScheme: String? = null
        var currentLicenseUrl: String? = null
        var currentLicenseData: String? = null
        var currentLicenseHeaders: MutableMap<String, String>? = null
        var currentChno: Int? = null
        var currentCatchup: String? = null
        var currentCatchupDays: Int? = null
        var currentCatchupSource: String? = null
        var currentManifestType: String? = null
        var currentIsRadio: Boolean = false
        var currentAspectRatio: String? = null
        var currentLanguage: String? = null
        var currentName: String = "Unknown Channel"

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("#EXTM3U")) {
                    val epgUrlAttr = extractAttribute(trimmed, "x-tvg-url")
                        ?: extractAttribute(trimmed, "url-tvg")
                        ?: extractAttribute(trimmed, "tvg-url")
                    if (!epgUrlAttr.isNullOrBlank()) {
                        embeddedEpgUrl = epgUrlAttr.trim()
                    }
                    continue
                }
                if (trimmed.startsWith("#EXTGRP:")) {
                    val grp = trimmed.substring("#EXTGRP:".length).trim()
                    if (grp.isNotEmpty()) {
                        currentGroupTitle = grp
                    }
                    continue
                }
                if (trimmed.startsWith("#EXTINF:")) {
                    currentTvgId = extractAttribute(trimmed, "tvg-id") ?: extractAttribute(trimmed, "channel-id")
                    currentTvgName = extractAttribute(trimmed, "tvg-name")
                    currentTvgLogo = extractAttribute(trimmed, "tvg-logo") ?: extractAttribute(trimmed, "logo")
                    currentGroupTitle = extractAttribute(trimmed, "group-title") ?: extractAttribute(trimmed, "group-name") ?: extractAttribute(trimmed, "group") ?: extractAttribute(trimmed, "cate-name") ?: currentGroupTitle
                    currentDrmScheme = extractAttribute(trimmed, "drm-scheme") ?: extractAttribute(trimmed, "drm_scheme") ?: extractAttribute(trimmed, "license-type")
                    currentLicenseUrl = extractAttribute(trimmed, "license-url") ?: extractAttribute(trimmed, "license_url") ?: extractAttribute(trimmed, "license-key") ?: extractAttribute(trimmed, "license_key")
                    currentChno = (extractAttribute(trimmed, "tvg-chno") ?: extractAttribute(trimmed, "tvg-num") ?: extractAttribute(trimmed, "chno"))?.toIntOrNull()
                    currentCatchup = extractAttribute(trimmed, "catchup") ?: extractAttribute(trimmed, "catchup-type")
                    currentCatchupDays = extractAttribute(trimmed, "catchup-days")?.toIntOrNull()
                    currentCatchupSource = extractAttribute(trimmed, "catchup-source")

                    val radioAttr = extractAttribute(trimmed, "radio") ?: extractAttribute(trimmed, "tvg-radio")
                    if (!radioAttr.isNullOrBlank()) {
                        currentIsRadio = radioAttr.equals("true", ignoreCase = true) || radioAttr == "1"
                    }
                    currentAspectRatio = extractAttribute(trimmed, "tvg-aspect") ?: extractAttribute(trimmed, "aspect-ratio")
                    currentLanguage = extractAttribute(trimmed, "tvg-language") ?: extractAttribute(trimmed, "language")

                    val headersAttr = extractAttribute(trimmed, "license-headers") ?: extractAttribute(trimmed, "license_headers")
                    if (!headersAttr.isNullOrBlank()) {
                        val headersMap = currentLicenseHeaders ?: mutableMapOf()
                        headersMap.putAll(parseHeadersString(headersAttr))
                        currentLicenseHeaders = headersMap
                    }

                    val commaIndex = trimmed.lastIndexOf(',')
                    if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        currentName = trimmed.substring(commaIndex + 1).trim()
                    } else {
                        currentName = "Channel ${orderIndex + 1}"
                    }
                } else if (trimmed.startsWith("#EXTHTTP:")) {
                    val jsonContent = trimmed.substring("#EXTHTTP:".length).trim()
                    try {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val parsedJson: Map<String, Any>? = Gson().fromJson(jsonContent, type)
                        if (parsedJson != null) {
                            val headersMap = (currentLicenseHeaders ?: mutableMapOf()).toMutableMap()
                            for ((k, v) in parsedJson) {
                                val valStr = v.toString()
                                val canonicalKey = when (k.trim().lowercase()) {
                                    "cookie" -> "Cookie"
                                    "referer", "referrer" -> "Referer"
                                    "origin" -> "Origin"
                                    "user-agent", "user_agent", "http-user-agent" -> "User-Agent"
                                    "authorization" -> "Authorization"
                                    else -> k.trim()
                                }
                                headersMap[canonicalKey] = valStr
                            }
                            currentLicenseHeaders = headersMap
                        }
                    } catch (_: Exception) {
                        val headersMap = (currentLicenseHeaders ?: mutableMapOf()).toMutableMap()
                        headersMap.putAll(parseHeadersString(jsonContent))
                        currentLicenseHeaders = headersMap
                    }
                } else if (trimmed.startsWith("#KODIPROP:") || trimmed.startsWith("#EXTVLCOPT:")) {
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx != -1) {
                        val propContent = trimmed.substring(colonIdx + 1).trim()
                        val eqIdx = propContent.indexOf('=')
                        if (eqIdx != -1) {
                            val key = propContent.substring(0, eqIdx).trim().lowercase()
                            val value = propContent.substring(eqIdx + 1).trim()

                            if (key.contains("license_type") || key.contains("drm_type") || key.contains("drm")) {
                                currentDrmScheme = when {
                                    value.contains("widevine", ignoreCase = true) -> "widevine"
                                    value.contains("playready", ignoreCase = true) -> "playready"
                                    value.contains("clearkey", ignoreCase = true) -> "clearkey"
                                    else -> value
                                }
                            } else if (key.contains("license_key") || key.contains("license_url") || key.contains("license")) {
                                currentLicenseUrl = value
                            } else if (key.contains("license_data")) {
                                currentLicenseData = value
                            } else if (key.contains("manifest_type") || key.contains("stream_type")) {
                                currentManifestType = value
                            } else if (key.contains("user_agent") || key.contains("http-user-agent")) {
                                val headersMap = (currentLicenseHeaders ?: mutableMapOf()).toMutableMap()
                                headersMap["User-Agent"] = value
                                currentLicenseHeaders = headersMap
                            } else if (key.contains("referer") || key.contains("http-referrer")) {
                                val headersMap = (currentLicenseHeaders ?: mutableMapOf()).toMutableMap()
                                headersMap["Referer"] = value
                                currentLicenseHeaders = headersMap
                            } else if (key.contains("headers") || key.contains("cookie") || key.contains("stream_headers")) {
                                val headersMap = (currentLicenseHeaders ?: mutableMapOf()).toMutableMap()
                                headersMap.putAll(parseHeadersString(value))
                                currentLicenseHeaders = headersMap
                            }
                        }
                    }
                } else if (trimmed.startsWith("#")) {
                    continue
                } else {
                    var streamUrl = trimmed
                    if (playlistUrl != null && !streamUrl.startsWith("http://", ignoreCase = true) && !streamUrl.startsWith("https://", ignoreCase = true)) {
                        streamUrl = resolveRelativeUrl(playlistUrl, streamUrl)
                    }

                    if (streamUrl.contains("|")) {
                        val parts = streamUrl.split("|", limit = 2)
                        streamUrl = parts[0].trim().removeSuffix("?").removeSuffix("&").removeSuffix("#").trim()
                        val headersPart = parts[1].trim()
                        val parsedHeaders = parseHeadersString(headersPart)
                        if (parsedHeaders.isNotEmpty()) {
                            val headersMap = (currentLicenseHeaders ?: mutableMapOf()).toMutableMap()
                            for ((k, v) in parsedHeaders) {
                                val kLower = k.lowercase()
                                if (kLower == "license_key" || kLower == "license-key" || kLower == "license_url") {
                                    if (currentLicenseUrl.isNullOrBlank()) {
                                        currentLicenseUrl = v
                                    }
                                } else if (kLower == "license_type" || kLower == "drm_type" || kLower == "drm") {
                                    if (currentDrmScheme.isNullOrBlank()) {
                                        currentDrmScheme = v
                                    }
                                } else if (kLower == "license_data") {
                                    if (currentLicenseData.isNullOrBlank()) {
                                        currentLicenseData = v
                                    }
                                } else if (kLower == "manifest_type" || kLower == "stream_type") {
                                    if (currentManifestType.isNullOrBlank()) {
                                        currentManifestType = v
                                    }
                                } else {
                                    val canonicalKey = when (kLower) {
                                        "cookie" -> "Cookie"
                                        "referer", "referrer" -> "Referer"
                                        "origin" -> "Origin"
                                        "user-agent", "user_agent", "http-user-agent" -> "User-Agent"
                                        "authorization" -> "Authorization"
                                        else -> k
                                    }
                                    headersMap[canonicalKey] = v
                                }
                            }
                            currentLicenseHeaders = headersMap
                        }
                    } else {
                        streamUrl = streamUrl.removeSuffix("?").removeSuffix("&").removeSuffix("#").trim()
                    }

                    var finalLicenseUrl = currentLicenseUrl
                    var finalLicenseHeaders = currentLicenseHeaders?.toMap()

                    if (!finalLicenseUrl.isNullOrBlank() && !finalLicenseUrl.trim().startsWith("{") && finalLicenseUrl.contains("|")) {
                        val parts = finalLicenseUrl.split("|", limit = 2)
                        finalLicenseUrl = parts[0].trim().removeSuffix("?").removeSuffix("&").removeSuffix("#").trim()
                        val headersPart = parts[1].trim()
                        val parsedHeaders = parseHeadersString(headersPart)
                        if (parsedHeaders.isNotEmpty()) {
                            finalLicenseHeaders = (finalLicenseHeaders ?: emptyMap()) + parsedHeaders
                        }
                    }

                    if (currentDrmScheme.isNullOrBlank() && !finalLicenseUrl.isNullOrBlank()) {
                        val lowerUrl = finalLicenseUrl.lowercase()
                        currentDrmScheme = when {
                            lowerUrl.contains("clearkey") || lowerUrl.contains("/clearkey") -> "clearkey"
                            lowerUrl.contains("playready") -> "playready"
                            lowerUrl.contains("widevine") -> "widevine"
                            else -> null
                        }
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
                            licenseUrl = finalLicenseUrl,
                            licenseHeaders = finalLicenseHeaders,
                            chno = currentChno,
                            catchup = currentCatchup,
                            catchupDays = currentCatchupDays,
                            catchupSource = currentCatchupSource,
                            manifestType = currentManifestType,
                            isRadio = currentIsRadio,
                            aspectRatio = currentAspectRatio,
                            language = currentLanguage,
                            licenseData = currentLicenseData
                        )
                    )
                    onChannelParsed?.invoke(channels.size)
                    currentTvgId = null
                    currentTvgName = null
                    currentTvgLogo = null
                    currentGroupTitle = null
                    currentDrmScheme = null
                    currentLicenseUrl = null
                    currentLicenseHeaders = null
                    currentChno = null
                    currentCatchup = null
                    currentCatchupDays = null
                    currentCatchupSource = null
                    currentManifestType = null
                    currentIsRadio = false
                    currentAspectRatio = null
                    currentLanguage = null
                    currentLicenseData = null
                    currentName = "Unknown Channel"
                }
            }
        }
        return ParsedPlaylist(channels, embeddedEpgUrl)
    }

    fun parse(inputStream: InputStream, playlistUrl: String? = null, onChannelParsed: ((Int) -> Unit)? = null): List<Channel> {
        return parsePlaylist(inputStream, playlistUrl, onChannelParsed).channels
    }

    private fun parseHeadersString(headersStr: String): Map<String, String> {
        val map = mutableMapOf<String, String>()

        fun addCanonical(k: String, v: String) {
            val keyTrimmed = k.trim()
            if (keyTrimmed.isEmpty()) return
            val canonicalKey = when (keyTrimmed.lowercase()) {
                "cookie" -> "Cookie"
                "referer", "referrer" -> "Referer"
                "origin" -> "Origin"
                "user-agent", "user_agent", "http-user-agent" -> "User-Agent"
                "authorization" -> "Authorization"
                else -> keyTrimmed
            }
            map[canonicalKey] = v.trim().removeSurrounding("\"")
        }

        if (headersStr.contains(":") && !headersStr.contains("=")) {
            val parts = headersStr.split(":", limit = 2)
            if (parts.size == 2) {
                addCanonical(parts[0], parts[1])
            }
            return map
        }
        val pairs = headersStr.split(Regex("[&\\n]+"))
        for (pair in pairs) {
            val trimmedPair = pair.trim()
            if (trimmedPair.isEmpty()) continue
            val eqIndex = trimmedPair.indexOf('=')
            if (eqIndex != -1) {
                val k = trimmedPair.substring(0, eqIndex)
                val v = trimmedPair.substring(eqIndex + 1)
                addCanonical(k, v)
            } else {
                val colonIndex = trimmedPair.indexOf(':')
                if (colonIndex != -1) {
                    val k = trimmedPair.substring(0, colonIndex)
                    val v = trimmedPair.substring(colonIndex + 1)
                    addCanonical(k, v)
                }
            }
        }
        return map
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
