package com.safehome.app.util

import android.content.Context
import android.content.SharedPreferences
import com.safehome.app.model.ContactResponse

class TokenManager(context: Context) {

    private val gson = com.google.gson.Gson()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("safehome_prefs", Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun saveNickname(nickname: String) {
        prefs.edit().putString("nickname", nickname).apply()
    }

    fun getNickname(): String? = prefs.getString("nickname", null)

    fun saveEmail(email: String) {
        prefs.edit().putString("email", email).apply()
    }

    fun getEmail(): String? = prefs.getString("email", null)

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun saveContactCache(contacts: List<ContactResponse>) {
        prefs.edit().putString("contact_cache", gson.toJson(contacts)).apply()
    }

    fun getContactCache(): List<ContactResponse> {
        val json = prefs.getString("contact_cache", null) ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<ContactResponse>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { emptyList() }
    }

    fun getContacts(): List<Pair<String, String>> =
        getContactCache().map { it.name to it.phone }

    fun getLegacyContacts(): List<Pair<String, String>> {
        val raw = prefs.getString("emergency_contacts", "") ?: return emptyList()
        if(raw.isEmpty()) return emptyList()
        return raw.split("|").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }

    fun clearLegacyContacts() {
        prefs.edit().remove("emergency_contacts").apply()
    }

    fun saveLockScreenSos(enabled: Boolean) {
        prefs.edit().putBoolean("lockscreen_sos", enabled).apply()
    }

    fun isLockScreenSosEnabled(): Boolean {
        return prefs.getBoolean("lockscreen_sos", false)
    }

    fun saveNightMode(enabled: Boolean) {
        prefs.edit().putBoolean("night_mode", enabled).apply()
    }

    fun isNightModeEnabled(): Boolean {
        return prefs.getBoolean("night_mode", false)
    }

    fun saveAutoPoliceReport(enabled: Boolean){
        prefs.edit().putBoolean("auto_police_report",enabled).apply()
    }

    fun isAutoPoliceReportEnabled(): Boolean {
        return prefs.getBoolean("auto_police_report",false)
    }

    fun saveVoiceDetectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("voice_detection_enabled", enabled).apply()
    }

    fun isVoiceDetectionEnabled(): Boolean {
        return prefs.getBoolean("voice_detection_enabled", false)
    }
}