package com.example.iptvmate

import java.io.Serializable

data class Channel(
    val id: Long = 0,
    val name: String,
    val streamUrl: String,
    val tvgId: String?,
    val tvgName: String?,
    val tvgLogo: String?,
    val groupTitle: String?,
    val orderIndex: Int,
    val drmScheme: String? = null,
    val licenseUrl: String? = null,
    val licenseHeaders: Map<String, String>? = null,
    val isFavorite: Boolean = false,
    val chno: Int? = null,
    val catchup: String? = null,
    val catchupDays: Int? = null,
    val catchupSource: String? = null,
    val manifestType: String? = null,
    val isRadio: Boolean = false,
    val aspectRatio: String? = null,
    val language: String? = null,
    val licenseData: String? = null
) : Serializable
