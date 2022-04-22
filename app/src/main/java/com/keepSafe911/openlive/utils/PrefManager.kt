package com.keepSafe911.openlive.utils

import android.content.Context
import android.content.SharedPreferences
import com.keepSafe911.openlive.Constants

object PrefManager {
    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    }
}