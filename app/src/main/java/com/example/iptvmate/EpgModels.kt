package com.example.iptvmate

import java.io.Serializable

data class EpgProgram(
    val channelId: String,
    val title: String,
    val startTime: Long, // epoch millis
    val stopTime: Long   // epoch millis
) : Serializable

data class EpgChannelData(
    val channelId: String,
    var displayName: String?,
    val programs: MutableList<EpgProgram> = mutableListOf()
) : Serializable
