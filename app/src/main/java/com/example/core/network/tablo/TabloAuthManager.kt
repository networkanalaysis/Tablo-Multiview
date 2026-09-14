package com.example.core.network.tablo

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TabloUserSession(
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val tabloHostIp: String = "",
    val tabloServerId: String = "",
    val tabloDeviceName: String = "",
    val tunerCount: Int = 2
)

class TabloAuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tablo_user_session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<TabloUserSession> = _session.asStateFlow()

    private fun loadSession(): TabloUserSession {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val host = prefs.getString(KEY_HOST_IP, "") ?: ""
        val serverId = prefs.getString(KEY_SERVER_ID, "") ?: ""
        val devName = prefs.getString(KEY_DEV_NAME, "") ?: ""
        val tuners = prefs.getInt(KEY_TUNERS, 2)
        return TabloUserSession(
            isLoggedIn = loggedIn,
            email = email,
            tabloHostIp = host,
            tabloServerId = serverId,
            tabloDeviceName = devName,
            tunerCount = tuners
        )
    }

    fun saveSession(
        email: String,
        hostIp: String,
        serverId: String,
        deviceName: String,
        tuners: Int
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_HOST_IP, hostIp)
            .putString(KEY_SERVER_ID, serverId)
            .putString(KEY_DEV_NAME, deviceName)
            .putInt(KEY_TUNERS, tuners)
            .apply()

        _session.value = TabloUserSession(
            isLoggedIn = true,
            email = email,
            tabloHostIp = hostIp,
            tabloServerId = serverId,
            tabloDeviceName = deviceName,
            tunerCount = tuners
        )
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _session.value = TabloUserSession()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_HOST_IP = "key_host_ip"
        private const val KEY_SERVER_ID = "key_server_id"
        private const val KEY_DEV_NAME = "key_dev_name"
        private const val KEY_TUNERS = "key_tuners"
    }
}
