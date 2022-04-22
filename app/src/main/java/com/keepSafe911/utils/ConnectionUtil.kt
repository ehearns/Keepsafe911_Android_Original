package com.keepSafe911.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class ConnectionUtil {

    /**
     * Checks if the Internet connection is available.
     *
     * @return Returns true if the Internet connection is available. False otherwise.
     * *
     */
    companion object {
        fun isInternetAvailable(ctx: Context): Boolean {
            var result = false
            val connectivityManager =
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = connectivityManager.activeNetwork
                when {
                    networkCapabilities != null -> {
                        val actNw =
                            connectivityManager.getNetworkCapabilities(networkCapabilities)
                        result = if (actNw != null) {
                            when {
                                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    else -> {
                        result = false
                    }
                }
            } else {
                connectivityManager.run {
                    val networkInfo = connectivityManager.activeNetworkInfo
                    if (networkInfo != null) {
                        connectivityManager.activeNetworkInfo?.run {
                            result = when (type) {
                                ConnectivityManager.TYPE_WIFI -> true
                                ConnectivityManager.TYPE_MOBILE -> true
                                ConnectivityManager.TYPE_ETHERNET -> true
                                else -> false
                            }
                        }
                    } else {
                        result = false
                    }
                }
            }
            return result


            // if network is NOT available networkInfo will be null
            // otherwise check if we are connected
/*


            // if network is NOT available networkInfo will be null
            // otherwise check if we are connected
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkInfo = cm.activeNetworkInfo

            // if network is NOT available networkInfo will be null
            // otherwise check if we are connected
            return networkInfo != null && networkInfo.isConnected
*/

        }
    }

}