package com.syedhunain.hisaabi.data

import android.content.Context
import androidx.compose.runtime.getValue
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
    }
}
