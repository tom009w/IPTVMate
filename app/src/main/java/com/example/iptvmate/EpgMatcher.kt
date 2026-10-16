package com.example.iptvmate

object EpgMatcher {

    fun matchEpg(channels: List<Channel>, epgMap: Map<String, EpgChannelData>): Map<Int, Pair<EpgProgram?, EpgProgram?>> {
        val result = mutableMapOf<Int, Pair<EpgProgram?, EpgProgram?>>()
        val now = System.currentTimeMillis()

        val epgById = epgMap.mapKeys { it.key.trim() }
        val epgByName = epgMap.values.groupBy { JioChannelMatcher.normalizeName(it.displayName) }

        for (channel in channels) {
            var epgData: EpgChannelData? = null
            if (!channel.tvgId.isNullOrBlank()) {
                epgData = epgById[channel.tvgId.trim()]
            }
            if (epgData == null && !channel.tvgName.isNullOrBlank()) {
                val norm = JioChannelMatcher.normalizeName(channel.tvgName)
                val candidates = epgByName[norm]
                if (!candidates.isNullOrEmpty()) epgData = candidates[0]
            }
            if (epgData == null) {
                val norm = JioChannelMatcher.normalizeName(channel.name)
                val candidates = epgByName[norm]
                if (!candidates.isNullOrEmpty()) epgData = candidates[0]
            }

            var currentProg: EpgProgram? = null
            var nextProg: EpgProgram? = null

            if (epgData != null && epgData.programs.isNotEmpty()) {
                val sorted = epgData.programs.sortedBy { it.startTime }
                val currentIndex = sorted.indexOfFirst { it.startTime <= now && it.stopTime > now }
                if (currentIndex != -1) {
                    currentProg = sorted[currentIndex]
                    if (currentIndex + 1 < sorted.size) {
                        nextProg = sorted[currentIndex + 1]
                    }
                } else {
                    val upcoming = sorted.firstOrNull { it.startTime > now }
                    if (upcoming != null) {
                        nextProg = upcoming
                    }
                }
            }
            result[channel.orderIndex] = Pair(currentProg, nextProg)
        }
        return result
    }

    fun getChannelPrograms(channel: Channel, epgMap: Map<String, EpgChannelData>): List<EpgProgram> {
        val epgById = epgMap.mapKeys { it.key.trim() }
        val epgByName = epgMap.values.groupBy { JioChannelMatcher.normalizeName(it.displayName) }

        var epgData: EpgChannelData? = null
        if (!channel.tvgId.isNullOrBlank()) {
            epgData = epgById[channel.tvgId.trim()]
        }
        if (epgData == null && !channel.tvgName.isNullOrBlank()) {
            val norm = JioChannelMatcher.normalizeName(channel.tvgName)
            val candidates = epgByName[norm]
            if (!candidates.isNullOrEmpty()) epgData = candidates[0]
        }
        if (epgData == null) {
            val norm = JioChannelMatcher.normalizeName(channel.name)
            val candidates = epgByName[norm]
            if (!candidates.isNullOrEmpty()) epgData = candidates[0]
        }
        return epgData?.programs?.sortedBy { it.startTime } ?: emptyList()
    }
}
