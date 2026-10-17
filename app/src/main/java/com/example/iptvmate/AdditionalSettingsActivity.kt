package com.example.iptvmate

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdditionalSettingsActivity : AppCompatActivity() {

    private lateinit var etXtreamServer: EditText
    private lateinit var etXtreamUsername: EditText
    private lateinit var etXtreamPassword: EditText
    private lateinit var btnSaveXtream: Button

    private lateinit var etMacPortalUrl: EditText
    private lateinit var etMacAddress: EditText
    private lateinit var btnSaveMacPortal: Button

    private lateinit var etStalkerPortalUrl: EditText
    private lateinit var etStalkerMacAddress: EditText
    private lateinit var etStalkerSerial: EditText
    private lateinit var etStalkerDeviceId: EditText
    private lateinit var btnSaveStalker: Button

    private lateinit var repository: PlaylistRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_additional_settings)

        etXtreamServer = findViewById(R.id.etXtreamServer)
        etXtreamUsername = findViewById(R.id.etXtreamUsername)
        etXtreamPassword = findViewById(R.id.etXtreamPassword)
        btnSaveXtream = findViewById(R.id.btnSaveXtream)

        etMacPortalUrl = findViewById(R.id.etMacPortalUrl)
        etMacAddress = findViewById(R.id.etMacAddress)
        btnSaveMacPortal = findViewById(R.id.btnSaveMacPortal)

        etStalkerPortalUrl = findViewById(R.id.etStalkerPortalUrl)
        etStalkerMacAddress = findViewById(R.id.etStalkerMacAddress)
        etStalkerSerial = findViewById(R.id.etStalkerSerial)
        etStalkerDeviceId = findViewById(R.id.etStalkerDeviceId)
        btnSaveStalker = findViewById(R.id.btnSaveStalker)

        repository = PlaylistRepository(applicationContext)

        val appSettings = repository.loadAppSettings()
        if (!appSettings.xtreamServer.isNullOrBlank()) etXtreamServer.setText(appSettings.xtreamServer)
        if (!appSettings.xtreamUsername.isNullOrBlank()) etXtreamUsername.setText(appSettings.xtreamUsername)
        if (!appSettings.xtreamPassword.isNullOrBlank()) etXtreamPassword.setText(appSettings.xtreamPassword)

        if (!appSettings.macPortalUrl.isNullOrBlank()) etMacPortalUrl.setText(appSettings.macPortalUrl)
        if (!appSettings.macAddress.isNullOrBlank()) etMacAddress.setText(appSettings.macAddress)

        if (!appSettings.stalkerPortalUrl.isNullOrBlank()) etStalkerPortalUrl.setText(appSettings.stalkerPortalUrl)
        if (!appSettings.stalkerMacAddress.isNullOrBlank()) etStalkerMacAddress.setText(appSettings.stalkerMacAddress)
        if (!appSettings.stalkerSerial.isNullOrBlank()) etStalkerSerial.setText(appSettings.stalkerSerial)
        if (!appSettings.stalkerDeviceId.isNullOrBlank()) etStalkerDeviceId.setText(appSettings.stalkerDeviceId)

        btnSaveXtream.setOnClickListener {
            val server = etXtreamServer.text.toString().trim().removeSuffix("/")
            val username = etXtreamUsername.text.toString().trim()
            val password = etXtreamPassword.text.toString().trim()

            if (server.isBlank() || username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill in all Xtream Codes fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val m3uUrl = "$server/get.php?username=$username&password=$password&type=m3u_plus"
            lifecycleScope.launch {
                repository.savePlaylistUrl(m3uUrl)
                val settings = repository.loadAppSettings()
                repository.saveAppSettings(settings.copy(
                    playlistUrl = m3uUrl,
                    xtreamServer = server,
                    xtreamUsername = username,
                    xtreamPassword = password
                ))
                Toast.makeText(this@AdditionalSettingsActivity, "Xtream Codes connected successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnSaveMacPortal.setOnClickListener {
            val portalUrl = etMacPortalUrl.text.toString().trim()
            val mac = etMacAddress.text.toString().trim()

            if (portalUrl.isBlank() || mac.isBlank()) {
                Toast.makeText(this, "Please fill in Portal URL and MAC Address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val playlistLink = if (portalUrl.contains("?")) "$portalUrl&mac=$mac" else "$portalUrl?mac=$mac"
            lifecycleScope.launch {
                repository.savePlaylistUrl(playlistLink)
                val settings = repository.loadAppSettings()
                repository.saveAppSettings(settings.copy(
                    playlistUrl = playlistLink,
                    macPortalUrl = portalUrl,
                    macAddress = mac
                ))
                Toast.makeText(this@AdditionalSettingsActivity, "MAC Portal connected successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnSaveStalker.setOnClickListener {
            val portalUrl = etStalkerPortalUrl.text.toString().trim()
            val mac = etStalkerMacAddress.text.toString().trim()
            val serial = etStalkerSerial.text.toString().trim()
            val deviceId = etStalkerDeviceId.text.toString().trim()

            if (portalUrl.isBlank() || mac.isBlank()) {
                Toast.makeText(this, "Please fill in Portal URL and MAC Address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var playlistLink = if (portalUrl.contains("?")) "$portalUrl&mac=$mac" else "$portalUrl?mac=$mac"
            if (serial.isNotBlank()) playlistLink += "&serial=$serial"
            if (deviceId.isNotBlank()) playlistLink += "&device_id=$deviceId"

            lifecycleScope.launch {
                repository.savePlaylistUrl(playlistLink)
                val settings = repository.loadAppSettings()
                repository.saveAppSettings(settings.copy(
                    playlistUrl = playlistLink,
                    stalkerPortalUrl = portalUrl,
                    stalkerMacAddress = mac,
                    stalkerSerial = serial,
                    stalkerDeviceId = deviceId
                ))
                Toast.makeText(this@AdditionalSettingsActivity, "Stalker Portal connected successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
