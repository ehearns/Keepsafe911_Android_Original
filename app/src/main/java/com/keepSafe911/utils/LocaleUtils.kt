package com.keepSafe911.utils

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.ContextThemeWrapper
import java.lang.Exception
import java.util.*

object LocaleUtils {

    val LAN_SPANISH = "es"
    val LAN_PORTUGUESE = "pt"
    val LAN_ENGLISH = "en"
    private var sLocale: Locale? = null

    fun setLocale(locale: Locale) {
        sLocale = locale
        if (sLocale != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Locale.setDefault(Locale.Category.DISPLAY, sLocale)
            }else{
                Locale.setDefault(sLocale)
            }
        }
    }

    fun updateConfig(wrapper: ContextThemeWrapper) {
        if (sLocale != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val configuration = Configuration()
            configuration.setLocale(sLocale)
            wrapper.applyOverrideConfiguration(configuration)
        }
    }

    fun updateConfig(app: Application, configuration: Configuration) {
        if (sLocale != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //Wrapping the configuration to avoid Activity endless loop
            try {
                val config = Configuration(configuration)
                config.locale = sLocale
                config.setLocale(sLocale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localList: LocaleList = LocaleList(sLocale)
                    LocaleList.setDefault(localList)
                    config.setLocales(localList)
                }
                val res = app.baseContext.resources
                res.updateConfiguration(config, res.displayMetrics)
                app.createConfigurationContext(config)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    fun updateConfig(app: Context, configuration: Configuration) {
        if (sLocale != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //Wrapping the configuration to avoid Activity endless loop
            try {
                val config = Configuration(configuration)
                config.locale = sLocale
                config.setLocale(sLocale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localList: LocaleList = LocaleList(sLocale)
                    LocaleList.setDefault(localList)
                    config.setLocales(localList)
                }
                val res = app.resources
                res.updateConfiguration(config, res.displayMetrics)
                app.createConfigurationContext(config)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}