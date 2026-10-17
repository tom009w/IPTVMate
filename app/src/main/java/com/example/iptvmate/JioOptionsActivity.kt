package com.example.iptvmate

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.net.URI

class JioOptionsActivity : AppCompatActivity() {

    private lateinit var rgCategoryMode: RadioGroup
    private lateinit var rbModeDefault: RadioButton
    private lateinit var rbModeSplit: RadioButton
    private lateinit var rbModeLanguage: RadioButton

    private lateinit var rgSubFilter: RadioGroup
    private lateinit var rbSubAll: RadioButton
    private lateinit var rbSubHide: RadioButton
    private lateinit var rbSubOnly: RadioButton

    private lateinit var switchHighQuality: SwitchCompat
    private lateinit var gridLanguages: GridLayout
    private lateinit var btnApplyJioOptions: Button

    private lateinit var repository: PlaylistRepository
    private val languageCheckboxes = mutableMapOf<String, CheckBox>()

    private val availableLanguages = arrayOf(
        "Hindi", "Marathi", "Punjabi", "Urdu", "Bengali", "English",
        "Malayalam", "Tamil", "Gujarati", "Odia", "Telugu", "Bhojpuri",
        "Kannada", "Assamese", "Nepali", "French", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jio_options)

        rgCategoryMode = findViewById(R.id.rgCategoryMode)
        rbModeDefault = findViewById(R.id.rbModeDefault)
        rbModeSplit = findViewById(R.id.rbModeSplit)
        rbModeLanguage = findViewById(R.id.rbModeLanguage)

        rgSubFilter = findViewById(R.id.rgSubFilter)
        rbSubAll = findViewById(R.id.rbSubAll)
        rbSubHide = findViewById(R.id.rbSubHide)
        rbSubOnly = findViewById(R.id.rbSubOnly)

        switchHighQuality = findViewById(R.id.switchHighQuality)
        gridLanguages = findViewById(R.id.gridLanguages)
        btnApplyJioOptions = findViewById(R.id.btnApplyJioOptions)

        repository = PlaylistRepository(applicationContext)

        for (lang in availableLanguages) {
            val cb = CheckBox(this).apply {
                text = lang
                setTextColor(android.graphics.Color.WHITE)
                textSize = 15f
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(16, 8, 16, 8)
                }
            }
            gridLanguages.addView(cb)
            languageCheckboxes[lang] = cb
        }

        val appSettings = repository.loadAppSettings()
        when (appSettings.jioCategoryMode) {
            "split" -> rbModeSplit.isChecked = true
            "language" -> rbModeLanguage.isChecked = true
            else -> rbModeDefault.isChecked = true
        }

        when (appSettings.jioSubFilter) {
            "hide" -> rbSubHide.isChecked = true
            "only" -> rbSubOnly.isChecked = true
            else -> rbSubAll.isChecked = true
        }

        switchHighQuality.isChecked = appSettings.jioQuality == "high"

        val selectedLangs = appSettings.jioLanguages.toSet()
        for ((lang, cb) in languageCheckboxes) {
            cb.isChecked = selectedLangs.contains(lang)
        }

        val layoutQualityRow = switchHighQuality as View
        layoutQualityRow.setOnClickListener {
            switchHighQuality.isChecked = !switchHighQuality.isChecked
        }

        btnApplyJioOptions.setOnClickListener {
            val categoryMode = when {
                rbModeSplit.isChecked -> "split"
                rbModeLanguage.isChecked -> "language"
                else -> null
            }

            val subFilter = when {
                rbSubHide.isChecked -> "hide"
                rbSubOnly.isChecked -> "only"
                else -> null
            }

            val quality = if (switchHighQuality.isChecked) "high" else null

            val checkedLanguages = languageCheckboxes.filter { it.value.isChecked }.keys.toList()

            lifecycleScope.launch {
                val currentUrl = repository.loadAppSettings().playlistUrl ?: ""
                val updatedUrl = buildJioPlaylistUrl(currentUrl, categoryMode, checkedLanguages, subFilter, quality)

                repository.savePlaylistUrl(updatedUrl)
                val settings = repository.loadAppSettings()
                repository.saveAppSettings(settings.copy(
                    playlistUrl = updatedUrl,
                    jioCategoryMode = categoryMode,
                    jioLanguages = checkedLanguages,
                    jioSubFilter = subFilter,
                    jioQuality = quality
                ))

                Toast.makeText(this@JioOptionsActivity, "JioTV Go options applied & playlist updated", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun buildJioPlaylistUrl(baseUrl: String, mode: String?, languages: List<String>, subFilter: String?, quality: String?): String {
        if (baseUrl.isBlank()) return baseUrl
        try {
            val baseUri = URI(baseUrl)
            val scheme = baseUri.scheme ?: "http"
            val authority = baseUri.authority ?: baseUri.host
            val path = baseUri.path ?: ""

            val queryParams = mutableMapOf<String, String>()
            val rawQuery = baseUri.query
            if (!rawQuery.isNullOrBlank()) {
                for (param in rawQuery.split("&")) {
                    val pair = param.split("=", limit = 2)
                    if (pair.isNotEmpty() && pair[0].isNotBlank()) {
                        val key = pair[0].trim().lowercase()
                        if (key != "c" && key != "l" && key != "sub" && key != "q") {
                            queryParams[pair[0]] = if (pair.size > 1) pair[1] else ""
                        }
                    }
                }
            }

            if (!mode.isNullOrBlank()) queryParams["c"] = mode
            if (languages.isNotEmpty()) queryParams["l"] = languages.joinToString(",")
            if (!subFilter.isNullOrBlank()) queryParams["sub"] = subFilter
            if (!quality.isNullOrBlank()) queryParams["q"] = quality

            val newQuery = if (queryParams.isNotEmpty()) {
                "?" + queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            } else {
                ""
            }

            return "$scheme://$authority$path$newQuery"
        } catch (e: Exception) {
            val cleanUrl = baseUrl.substringBefore("?")
            val params = mutableListOf<String>()
            if (!mode.isNullOrBlank()) params.add("c=$mode")
            if (languages.isNotEmpty()) params.add("l=${languages.joinToString(",")}")
            if (!subFilter.isNullOrBlank()) params.add("sub=$subFilter")
            if (!quality.isNullOrBlank()) params.add("q=$quality")

            return if (params.isNotEmpty()) "$cleanUrl?${params.joinToString("&")}" else cleanUrl
        }
    }
}
