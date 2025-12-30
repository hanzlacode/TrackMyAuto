package com.brogaming.trackmyauto.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object AppPrefs {

    private const val PREF_NAME = "trackmyauto_prefs"
    private const val KEY_AUTO_START_ON_BOOT = "auto_start_on_boot"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setAutoStartOnBoot(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_START_ON_BOOT, enabled) }
    }

    fun isAutoStartOnBoot(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_START_ON_BOOT, false)
    }

    // package: com.brogaming.trackmyauto.data.storage

    private const val KEY_OWNER_ID = "Owner_id"

    fun setOwnerId(context: Context, id: String) {
        prefs(context).edit { putString(KEY_OWNER_ID, id) }
    }

    fun getOwnerId(context: Context): String? {
        return prefs(context).getString(KEY_OWNER_ID, null)
    }

}
