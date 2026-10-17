package com.example.iptvmate

object GroupExtractor {
    fun extractGroups(channels: List<Channel>, customOrder: List<String> = emptyList(), hiddenCategories: List<String> = emptyList(), recentChannels: List<Channel> = emptyList()): List<String> {
        val groups = mutableSetOf<String>()
        for (channel in channels) {
            val title = channel.groupTitle?.trim()
            if (!title.isNullOrBlank()) {
                groups.add(title)
            } else {
                groups.add("Uncategorized")
            }
        }

        val hasFavorites = channels.any { it.isFavorite }
        val hasRecent = recentChannels.isNotEmpty()

        val allAvailable = mutableSetOf<String>()
        allAvailable.addAll(groups)
        if (hasFavorites) allAvailable.add("Favorites")
        if (hasRecent) allAvailable.add("Recent Channels")

        allAvailable.removeAll(hiddenCategories.toSet())

        val finalOrdered = mutableListOf<String>()
        for (cat in customOrder) {
            if (allAvailable.contains(cat)) {
                finalOrdered.add(cat)
                allAvailable.remove(cat)
            }
        }
        finalOrdered.addAll(allAvailable.sorted())

        return listOf("All Channels") + finalOrdered
    }

    fun filterChannels(channels: List<Channel>, selectedGroup: String, recentChannels: List<Channel> = emptyList()): List<Channel> {
        if (selectedGroup == "All Channels") return channels
        if (selectedGroup == "Favorites") return channels.filter { it.isFavorite }
        if (selectedGroup == "Recent Channels") {
            val channelMap = channels.associateBy { it.streamUrl }
            return recentChannels.mapNotNull { channelMap[it.streamUrl] }
        }
        return channels.filter {
            val title = it.groupTitle?.trim().let { t -> if (t.isNullOrBlank()) "Uncategorized" else t }
            title == selectedGroup
        }
    }
}
