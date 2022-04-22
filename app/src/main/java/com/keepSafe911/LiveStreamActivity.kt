package com.keepSafe911

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.keepSafe911.geofenceservice.LocationUpdatesService
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.response.LiveStreamResult
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.openlive.Constants.KEY_CHANNEL_ID
import com.keepSafe911.openlive.Constants.KEY_CHANNEL_NAME
import com.keepSafe911.openlive.Constants.KEY_CLIENT_ROLE
import com.keepSafe911.openlive.activities.RtcBaseActivity
import com.keepSafe911.openlive.stats.LocalStatsData
import com.keepSafe911.openlive.stats.RemoteStatsData
import com.keepSafe911.openlive.stats.StatsData
import com.keepSafe911.utils.*
import hideKeyboard
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.video.BeautyOptions
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.android.synthetic.main.activity_live_stream.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class LiveStreamActivity : RtcBaseActivity(), View.OnClickListener {

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private var mVideoDimension: VideoEncoderConfiguration.VideoDimensions? = null
    private var mService: LocationUpdatesService? = null
    private var mBound = false
    lateinit var mServiceIntent: Intent
    var userRole = Constants.CLIENT_ROLE_AUDIENCE
    var liveStreamResult: LiveStreamResult = LiveStreamResult()
    var minimumDurationTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_stream)
        hideKeyboard()
        ivLeaveChannel.setOnClickListener(this)
        ivSwitchCamera.setOnClickListener(this)
        ivAudio.setOnClickListener(this)
        initUI()
        initData()
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: LocationUpdatesService.LocalBinder =
                service as LocationUpdatesService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            if (mBound) {
                unbindService(mServiceConnection)
                mBound = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            bindService(
                Intent(this, LocationUpdatesService::class.java),
                mServiceConnection,
                Context.BIND_AUTO_CREATE
            )
            Handler(Looper.getMainLooper()).postDelayed({
                mService?.requestLocationUpdates()
            }, 3000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }

    private fun initUI() {
        val channelName = intent.getStringExtra(KEY_CHANNEL_NAME) ?: ""
        userRole = intent.getIntExtra(
            KEY_CLIENT_ROLE,
            Constants.CLIENT_ROLE_AUDIENCE
        )
        val liveStreamId = intent.getStringExtra(KEY_CHANNEL_ID) ?: ""
        val isFromNotification = intent.getBooleanExtra(
            "isFromNotification",
            false
        )
        liveStreamResult = intent.getParcelableExtra(
            "liveStreamData") ?: LiveStreamResult()

        setDataInView(liveStreamResult)

        if (isFromNotification) {
            finish()
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Utils.moveToLiveStream(this@LiveStreamActivity, false, channelName, liveStreamId, userRole, liveStreamResult)
                /*intent.putExtra(KEY_CLIENT_ROLE, role)
                intent.putExtra(KEY_CHANNEL_NAME, channelName)
                intent.putExtra(KEY_CHANNEL_ID, liveStreamId)
                intent.putExtra("liveStreamData", liveStreamResult)
                intent.putExtra("isFromNotification", false)*/
                startActivity(intent)
            }, 1500)
        }
        config().channelName = channelName
        val isBroadcaster = userRole == Constants.CLIENT_ROLE_BROADCASTER
        uiVisibility()

        ivAudio?.isActivated = isBroadcaster
        rtcEngine()?.setBeautyEffectOptions(
            true,
            BeautyOptions()
        )
        live_video_grid_layout?.setStatsManager(statsManager())
        config().userRole = userRole
        rtcEngine()?.setClientRole(userRole)
        if (isBroadcaster) startBroadcast()
        if (!isFromNotification) {
            Handler(Looper.getMainLooper()).postDelayed({
                Utils.liveStreamUserListing(this@LiveStreamActivity, object : CommonApiListener {
                    override fun liveStreamListResponse(
                        status: Boolean,
                        userList: ArrayList<LiveStreamResult>,
                        message: String,
                        responseMessage: String
                    ) {
                        var count = 0
                        for (i in userList.indices) {
                            val user = userList[i]
                            if (user.id != liveStreamId.toInt()) {
                                if (user.channelName != channelName) {
                                    count += 1
                                }
                            } else if (user.id == liveStreamId.toInt()) {
                                if ((liveStreamResult.id ?: 0) <= 0) {
                                    liveStreamResult = user
                                    setDataInView(liveStreamResult)
                                }
                            }
                        }
                        if (count == userList.size) {
                            Comman_Methods.isCustomPopUpShow(this@LiveStreamActivity,
                                message = resources.getString(R.string.str_stream_end),
                                negativeButtonText = resources.getString(R.string.str_go_back),
                                singleNegative = true,
                                positiveButtonListener = object : PositiveButtonListener{
                                    override fun cancelClickLister() {
                                        finish()
                                    }
                                })
                        }
                    }
                })
            }, 1000)
        }
        if (userRole == Constants.CLIENT_ROLE_BROADCASTER) {
            val loginObject: LoginObject = appDatabase.loginDao().getAll()
            val liveStreamDuration = loginObject.liveStreamDuration ?: 15
            val liveStreamMilli = (liveStreamDuration * 60000).toLong()
            minimumDurationTimer = Timer(liveStreamMinimumDurationKey, true)
            // schedule a single event till 30 minutes
            minimumDurationTimer?.schedule(liveStreamMilli) {
                returnDataToActivity()
            }
        }
    }

    private fun uiVisibility() {
        val isBroadcaster = userRole == Constants.CLIENT_ROLE_BROADCASTER
        if (isBroadcaster) {
            ivSwitchCamera.visibility = View.VISIBLE
            ivAudio.visibility = View.VISIBLE
            live_name_space_layout.visibility = View.GONE
        } else {
            ivSwitchCamera.visibility = View.GONE
            ivAudio.visibility = View.GONE
            live_name_space_layout.visibility = View.VISIBLE
        }
    }

    private fun setDataInView(liveStreamResult: LiveStreamResult) {
        if ((liveStreamResult.id ?: 0) > 0) {
            uiVisibility()
            val memberId = liveStreamResult.memberId ?: 0
            val adminId = liveStreamResult.adminId ?: 0
            val userId = if (memberId > 0) memberId else adminId
            val profileUrl = if (memberId > 0) (liveStreamResult.memberProfileUrl
                ?: "") else (liveStreamResult.adminProfileUrl ?: "")
            val userName = if (memberId > 0) (liveStreamResult.memberName
                ?: "") else (liveStreamResult.adminName ?: "")

            live_name_board_icon.loadFrescoImage(this@LiveStreamActivity, profileUrl, 1)
            live_room_name.text = userName
        } else {
            live_name_space_layout.visibility = View.GONE
        }
    }

    private fun initData() {
        mVideoDimension = com.keepSafe911.openlive.Constants.VIDEO_DIMENSIONS[config().videoDimenIndex]
    }

    private fun startBroadcast() {
        rtcEngine()?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        val surface: TextureView = prepareRtcVideo(0, true)
        live_video_grid_layout?.addUserVideoSurface(0, surface, true)
        ivAudio?.isActivated = true
        startRecord(surface)
    }

    private fun stopBroadcast() {
        try {
            rtcEngine()?.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
            removeRtcVideo(0, true)
            live_video_grid_layout?.removeUserVideo(0, true)
            ivAudio?.isActivated = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        // Do nothing at the moment
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        // Do nothing at the moment
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        runOnUiThread { removeRemoteUser(uid) }
    }

    override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
        runOnUiThread { renderRemoteUser(uid) }
    }

    private fun renderRemoteUser(uid: Int) {
        val surface: TextureView = prepareRtcVideo(uid, false)
        live_video_grid_layout?.addUserVideoSurface(uid, surface, false)
    }

    private fun removeRemoteUser(uid: Int) {
        removeRtcVideo(uid, false)
        live_video_grid_layout?.removeUserVideo(uid, false)
    }

    override fun onLocalVideoStats(stats: IRtcEngineEventHandler.LocalVideoStats?) {
        if (!statsManager().isEnabled) return
        val data: LocalStatsData = statsManager().getStatsData(0) as LocalStatsData? ?: return
        data.width = (mVideoDimension?.width ?: 0)
        data.height = (mVideoDimension?.height ?: 0)
        data.framerate = (stats?.sentFrameRate ?: 0)
    }

    override fun onRtcStats(stats: IRtcEngineEventHandler.RtcStats?) {
        if (!statsManager().isEnabled) return
        val data: LocalStatsData = statsManager().getStatsData(0) as LocalStatsData? ?: return
        data.lastMileDelay = (stats?.lastmileDelay ?: 0)
        data.videoSendBitrate = (stats?.txVideoKBitRate ?: 0)
        data.videoRecvBitrate = (stats?.rxVideoKBitRate ?: 0)
        data.audioSendBitrate = (stats?.txAudioKBitRate ?: 0)
        data.audioRecvBitrate = (stats?.rxAudioKBitRate ?: 0)
        data.cpuApp = (stats?.cpuAppUsage ?: 0.0)
        data.cpuTotal = (stats?.cpuAppUsage ?: 0.0)
        data.sendLoss = (stats?.txPacketLossRate ?: 0)
        data.recvLoss = (stats?.rxPacketLossRate ?: 0)
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
        if (!statsManager().isEnabled) return
        val data: StatsData = statsManager().getStatsData(uid) ?: return
        data.sendQuality = (statsManager().qualityToString(txQuality))
        data.recvQuality = (statsManager().qualityToString(rxQuality))
    }

    override fun onRemoteVideoStats(stats: IRtcEngineEventHandler.RemoteVideoStats?) {
        if (!statsManager().isEnabled) return
        val data: RemoteStatsData =
            statsManager().getStatsData(stats?.uid ?: 0) as RemoteStatsData? ?: return
        data.width = (stats?.width ?: 0)
        data.height = (stats?.height ?: 0)
        data.framerate = (stats?.rendererOutputFrameRate ?: 0)
        data.videoDelay = (stats?.delay ?: 0)
    }

    override fun onRemoteAudioStats(stats: IRtcEngineEventHandler.RemoteAudioStats?) {
        if (!statsManager().isEnabled) return
        val data: RemoteStatsData =
            statsManager().getStatsData(stats?.uid ?: 0) as RemoteStatsData? ?: return
        data.audioNetDelay = (stats?.networkTransportDelay ?: 0)
        data.audioNetJitter = (stats?.jitterBufferDelay ?: 0)
        data.audioLoss = (stats?.audioLossRate ?: 0)
        data.audioQuality = (statsManager().qualityToString(stats?.quality ?: 0))
    }

    override fun finish() {
        super.finish()
        if (this@LiveStreamActivity::mServiceIntent.isInitialized) {
            stopService(mServiceIntent)
        }
        mService?.removeLocationUpdates()
        minimumDurationTimer?.cancel()
        stopBroadcast()
        statsManager().clearAllData()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivAudio -> {
                Comman_Methods.avoidDoubleClicks(v)
                rtcEngine()?.muteLocalAudioStream(v.isActivated)
                v.isActivated = !v.isActivated
            }
            R.id.ivSwitchCamera -> {
                Comman_Methods.avoidDoubleClicks(v)
                rtcEngine()?.switchCamera()
            }
            R.id.ivLeaveChannel -> {
                Comman_Methods.avoidDoubleClicks(v)
                onBackPressed()
            }
        }
    }

    override fun onBackPressed() {
        deleteLiveStreamDialog()
    }

    private fun deleteLiveStreamDialog() {
        var messageData = ""
        var positiveName = ""
        if (userRole == Constants.CLIENT_ROLE_BROADCASTER) {
            messageData = resources.getString(R.string.end_stream_message)
            positiveName = resources.getString(R.string.str_end_video)
        } else {
            messageData = resources.getString(R.string.leave_stream_message)
            positiveName = resources.getString(R.string.str_leave_video)
        }
        Comman_Methods.isCustomPopUpShow(this@LiveStreamActivity,
            message = messageData, positiveButtonText = positiveName,
            negativeButtonText = resources.getString(R.string.cancel),
            positiveButtonListener = object : PositiveButtonListener {
                override fun okClickListener() {
                    returnDataToActivity()
                }
            })
    }

    private fun returnDataToActivity() {
        if (userRole == Constants.CLIENT_ROLE_BROADCASTER) {
            try {
                val intent = Intent()
                mediaFile?.let {
                    if (it.exists()) {
                        intent.putExtra("videoFile", it.absolutePath)
                    }
                }
                /*secondMediaFile?.let {
                    if (it.exists()) {
                        intent.putExtra("secondVideoFile", it.absolutePath)
                    }
                }*/
                intent.putExtra(KEY_CHANNEL_ID, liveStreamId)
                setResult(RESULT_OK, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}