package com.nkust.suggest.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nkust.suggest.data.model.UserProfile

class SecurePreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "nkust_secure_user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_name", profile.name)
            .putString("guest_type", profile.guestType)
            .putString("email", profile.email)
            .putString("phone", profile.phone)
            .putString("secrecy_type", profile.secrecyType)
            .putString("done_open", profile.doneOpen)
            .putString("mail_password", profile.mailPassword)
            .apply()
    }

    fun loadUserProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString("user_name", "") ?: "",
            guestType = prefs.getString("guest_type", "1") ?: "1",
            email = prefs.getString("email", "") ?: "",
            phone = prefs.getString("phone", "") ?: "",
            secrecyType = prefs.getString("secrecy_type", "9") ?: "9",
            doneOpen = prefs.getString("done_open", "1") ?: "1",
            mailPassword = prefs.getString("mail_password", "") ?: ""
        )
    }

    fun saveAppLang(lang: String) {
        prefs.edit().putString("app_lang", lang).apply()
    }

    fun loadAppLang(): String {
        return prefs.getString("app_lang", "zh") ?: "zh"
    }

    fun saveDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("dark_mode", isDark).apply()
    }

    fun loadDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }
}
