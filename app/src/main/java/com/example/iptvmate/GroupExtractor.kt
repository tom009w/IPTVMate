package com.example.iptvmate

object GroupExtractor {
    fun extractGroups(channels: List<Channel>, customOrder: List<String> = emptyList(), hiddenCategories: List<String> = emptyList()): List<String> {
        val groups = mutableSetOf<String>()
        for (channel in channels) {
            val title = channel.groupTitle?.trim()
            if (!title.isNullOrBlank()) {
                groups.add(title)
            } else {
                groups.add("Uncategorized")
            }
        }

        groups.removeAll(hiddenCategories.toSet())

        val orderedGroups = mutableListOf<String>()
        for (cat in customOrder) {
            if (groups.contains(cat)) {
                orderedGroups.add(cat)
                groups.remove(cat)
            }
        }
        orderedGroups.addAll(groups.sorted())

        val hasFavorites = channels.any { it.isFavorite }
        return if (hasFavorites) {
            listOf("All Channels", "Favorites") + orderedGroups
        } else {
            listOf("All Channels") + orderedGroups
        }
    }

    fun filterChannels(channels: List<Channel>, selectedGroup: String): List<Channel> {
        if (selectedGroup == "All Channels") return channels
        if (selectedGroup == "Favorites") return channels.filter { it.isFavorite }
        return channels.filter {
            val title = it.groupTitle?.trim().let { t -> if (t.isNullOrBlank()) "Uncategorized" else t }
            title == selectedGroup
        }
    }
}
