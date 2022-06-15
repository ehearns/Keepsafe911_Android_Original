package com.keepSafe911.utils

import android.content.Context
import android.content.SharedPreferences
import com.keepSafe911.BuildConfig

object AppPreference {

    private var prefREM: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null

    private fun initPreference(context: Context) {
        prefREM = context.getSharedPreferences(BuildConfig.sharePrefName, Context.MODE_PRIVATE)
        editor = prefREM?.edit()
    }

    fun getIntPreference(context: Context, key: String, defaultValue: Int = 0): Int {
        prefREM = context.getSharedPreferences(BuildConfig.sharePrefName, Context.MODE_PRIVATE)
        return prefREM?.getInt(key, defaultValue) ?: defaultValue
    }

    fun getStringPreference(context: Context, key: String, defaultValue: String = ""): String {
        prefREM = context.getSharedPreferences(BuildConfig.sharePrefName, Context.MODE_PRIVATE)
        return prefREM?.getString(key, defaultValue) ?: defaultValue
    }

    fun getBooleanPreference(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        prefREM = context.getSharedPreferences(BuildConfig.sharePrefName, Context.MODE_PRIVATE)
        return prefREM?.getBoolean(key, defaultValue) ?: defaultValue
    }

    fun saveIntPreference(context: Context, key: String, value: Int = 0) {
        initPreference(context)
        editor?.apply {
            putInt(key, value)
            commit()
        }
    }

    fun saveStringPreference(context: Context, key: String, value: String = "") {
        initPreference(context)
        editor?.apply {
            putString(key,value)
            commit()
        }
    }

    fun saveBooleanPreference(context: Context, key: String, value: Boolean = false) {
        initPreference(context)
        editor?.apply {
            putBoolean(key, value)
            commit()
        }
    }

    fun clearPrivacyTerms(context: Context) {
        initPreference(context)
        editor?.apply {
            putBoolean(BuildConfig.privacyPolicyPrefKey, false)
            putBoolean(BuildConfig.termsConditionPrefKey, false)
            commit()
        }
    }
}