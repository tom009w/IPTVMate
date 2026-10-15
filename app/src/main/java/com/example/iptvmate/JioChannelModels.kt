package com.example.iptvmate

data class JioChannelsResponse(
    val code: Int?,
    val message: String?,
    val result: List<JioChannelItem>?
)

data class JioChannelItem(
    val channel_id: String?,
    val channel_name: String?,
    val channel_url: String?,
    val key_url: String?
)
