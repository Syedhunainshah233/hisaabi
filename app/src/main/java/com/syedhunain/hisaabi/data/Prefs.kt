package com.syedhunain.hisaabi.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest

/** Small app settings, kept on the phone and exposed as Compose state. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("hisaabi_prefs", Context.MODE_PRIVATE)

    var romanUrdu by mutableStateOf(sp.getBoolean(KEY_UR, true))
        private set
    var dark by mutableStateOf(sp.getBoolean(KEY_DARK, false))
        private set
    var currency by mutableStateOf(sp.getString(KEY_CUR, "Rs") ?: "Rs")
        private set
    var business by mutableStateOf(sp.getString(KEY_BIZ, "") ?: "")
        private set
    var pinHash by mutableStateOf(sp.getString(KEY_PIN, "") ?: "")
        private set

    var googleEmail by mutableStateOf(sp.getString(KEY_G_EMAIL, "") ?: "")
        private set
    var googleName by mutableStateOf(sp.getString(KEY_G_NAME, "") ?: "")
        private set
    var lastBackupAt by mutableLongStateOf(sp.getLong(KEY_LAST_BACKUP, 0L))
        private set
    var seenWelcome by mutableStateOf(sp.getBoolean(KEY_WELCOME, false))
        private set
    var adsOffUntil by mutableLongStateOf(sp.getLong(KEY_ADS_OFF, 0L))
        private set
    /** Rewarded videos watched towards the next ad-free day. */
    var rewardsWatched by mutableIntStateOf(sp.getInt(KEY_REWARDS, 0))
        private set

    fun updateRewardsWatched(n: Int) {
        rewardsWatched = n; sp.edit().putInt(KEY_REWARDS, n).apply()
    }

    fun setGoogleAccount(email: String, name: String) {
        googleEmail = email; googleName = name
        sp.edit().putString(KEY_G_EMAIL, email).putString(KEY_G_NAME, name).apply()
    }

    fun clearGoogleAccount() {
        googleEmail = ""; googleName = ""
        sp.edit().remove(KEY_G_EMAIL).remove(KEY_G_NAME).apply()
    }

    fun markBackup(time: Long) {
        lastBackupAt = time; sp.edit().putLong(KEY_LAST_BACKUP, time).apply()
    }

    fun markWelcomeSeen() {
        seenWelcome = true; sp.edit().putBoolean(KEY_WELCOME, true).apply()
    }

    fun turnAdsOffUntil(time: Long) {
        adsOffUntil = time; sp.edit().putLong(KEY_ADS_OFF, time).apply()
    }

    fun updateRomanUrdu(v: Boolean) {
        romanUrdu = v; sp.edit().putBoolean(KEY_UR, v).apply()
    }

    fun updateDark(v: Boolean) {
        dark = v; sp.edit().putBoolean(KEY_DARK, v).apply()
    }

    fun updateCurrency(v: String) {
        val c = v.trim().ifEmpty { "Rs" }
        currency = c; sp.edit().putString(KEY_CUR, c).apply()
    }

    fun updateBusiness(v: String) {
        business = v.trim(); sp.edit().putString(KEY_BIZ, business).apply()
    }

    fun setPin(pin: String?) {
        pinHash = if (pin.isNullOrEmpty()) "" else hash(pin)
        sp.edit().putString(KEY_PIN, pinHash).apply()
    }

    fun checkPin(pin: String): Boolean = pinHash.isNotEmpty() && hash(pin) == pinHash

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(("hisaabi:" + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val KEY_UR = "roman_urdu"
        const val KEY_DARK = "dark"
        const val KEY_CUR = "currency"
        const val KEY_BIZ = "business"
        const val KEY_PIN = "pin"
        const val KEY_G_EMAIL = "google_email"
        const val KEY_G_NAME = "google_name"
        const val KEY_LAST_BACKUP = "last_backup"
        const val KEY_WELCOME = "seen_welcome"
        const val KEY_ADS_OFF = "ads_off_until"
        const val KEY_REWARDS = "rewards_watched"
    }
}
