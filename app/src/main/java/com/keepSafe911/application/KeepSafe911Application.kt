package com.keepSafe911.application

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import androidx.multidex.MultiDexApplication
import com.keepSafe911.openlive.rtc.AgoraEventHandler
import com.keepSafe911.openlive.rtc.EngineConfig
import com.keepSafe911.openlive.rtc.EventHandler
import com.keepSafe911.openlive.stats.StatsManager
import com.keepSafe911.openlive.utils.PrefManager
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.nativecode.ImagePipelineNativeLoader
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.keepSafe911.BuildConfig
import com.keepSafe911.openlive.Constants
import com.yanzhenjie.album.Album
import com.yanzhenjie.album.AlbumConfig
import io.agora.rtc.RtcEngine
import java.util.*


open class KeepSafe911Application: MultiDexApplication() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private var mRtcEngine: RtcEngine? = null
    private val mGlobalConfig: EngineConfig = EngineConfig()
    private val mHandler: AgoraEventHandler = AgoraEventHandler()
    private val mStatsManager: StatsManager = StatsManager()
    override fun onCreate() {
        super.onCreate()

        //Init for Fresco
//        Fresco.initialize(this)
        val builder = ImagePipelineConfig.newBuilder(this)
        var imagePipelineConfig = builder.build()
        Fresco.initialize(this, imagePipelineConfig)
        try {
            ImagePipelineNativeLoader.load()
        } catch (error: Throwable) {
            Fresco.shutDown()
            builder.experiment().setNativeCodeDisabled(true)
            imagePipelineConfig = builder.build()
            Fresco.initialize(this, imagePipelineConfig)
            error.printStackTrace()
        } catch (e: Exception) {
            Fresco.shutDown()
            builder.experiment().setNativeCodeDisabled(true)
            imagePipelineConfig = builder.build()
            Fresco.initialize(this, imagePipelineConfig)
            e.printStackTrace()
        }

        /*val fabric = Fabric.Builder(this)
            .kits(Crashlytics())
            .debuggable(true)
            .build()

        //Init Of Fabric for CrashLytics
        Fabric.with(fabric)*/
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KeepSafe911App::KeepSafe911WakelockTag").apply {
                    acquire()
                }
            }

        Album.initialize(
            AlbumConfig.newBuilder(this)
                .setAlbumLoader(MediaLoader())
                .setLocale(Locale.getDefault())
                .build()
        )

        try {
            mRtcEngine = RtcEngine.create(
                applicationContext,
                BuildConfig.private_app_id,
                mHandler
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initConfig()
    }

    private fun initConfig() {
        val pref: SharedPreferences = PrefManager.getPreferences(applicationContext)
        mGlobalConfig.videoDimenIndex = (
                pref.getInt(
                    Constants.PREF_RESOLUTION_IDX, Constants.DEFAULT_PROFILE_IDX
                )
                )
        val showStats: Boolean = pref.getBoolean(Constants.PREF_ENABLE_STATS, false)
        mGlobalConfig.mShowVideoStats = (showStats)
        mStatsManager.enableStats(showStats)
        mGlobalConfig.mirrorLocalIndex = (pref.getInt(Constants.PREF_MIRROR_LOCAL, 0))
        mGlobalConfig.mirrorRemoteIndex = (pref.getInt(Constants.PREF_MIRROR_REMOTE, 0))
        mGlobalConfig.mirrorEncodeIndex = (pref.getInt(Constants.PREF_MIRROR_ENCODE, 0))
    }

    fun engineConfig(): EngineConfig {
        return mGlobalConfig
    }

    fun rtcEngine(): RtcEngine? {
        return mRtcEngine
    }

    fun statsManager(): StatsManager {
        return mStatsManager
    }

    fun registerEventHandler(handler: EventHandler?) {
        handler?.let {
            mHandler.addHandler(it)
        }
    }

    fun removeEventHandler(handler: EventHandler?) {
        handler?.let {
            mHandler.removeHandler(it)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        wakeLock.release()
        RtcEngine.destroy()
    }
}