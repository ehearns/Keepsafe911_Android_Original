package com.keepSafe911.utils

import android.content.Context
import androidx.preference.PreferenceManager

class LocationUtil
{

    companion object {
        val KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates"

        /**
         * Returns true if requesting location updates, otherwise returns false.
         *
         * @param context The [Context].
         */
        fun requestingLocationUpdates(context: Context?): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false)
        }

        /**
         * Stores the location updates state in SharedPreferences.
         * @param requestingLocationUpdates The location updates state.
         */
        fun setRequestingLocationUpdates(
            context: Context?,
            requestingLocationUpdates: Boolean
        ) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply()
        }
    }
}