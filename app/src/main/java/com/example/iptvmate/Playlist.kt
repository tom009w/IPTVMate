package com.example.iptvmate

import java.io.Serializable
import java.util.UUID

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val epgUrl: String? = null
) : Serializable
