package com.keepSafe911.openlive.activities

import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.TextureView
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.datarecorder.ViewRecorder
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.response.LiveStreamResult
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.openlive.Constants
import com.keepSafe911.openlive.media.RtmTokenBuilder
import com.keepSafe911.openlive.rtc.EventHandler
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import getScreenHeight
import getScreenWidth
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


abstract class RtcBaseActivity : BaseActivity(), EventHandler {
    var liveStreamId: String = ""
    private var apiCalled: Boolean = false
    lateinit var appDatabase: OldMe911Database
    var role: Int = io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE
    var mediaFile: File? = null
    var secondMediaFile: File? = null
    private var videoRecorder: ViewRecorder? = null
    private var secondVideoRecorder: ViewRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(this@RtcBaseActivity)
        Utils.muteRecognizer(this@RtcBaseActivity, true)
        registerRtcEventHandler(this)
        joinChannel()
    }

    private fun configVideo() {
        val configuration = VideoEncoderConfiguration(
            Constants.VIDEO_DIMENSIONS[config().videoDimenIndex],
            VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
            VideoEncoderConfiguration.STANDARD_BITRATE,
            VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        )
        configuration.mirrorMode = Constants.VIDEO_MIRROR_MODES[config().mirrorEncodeIndex]
        rtcEngine()?.setVideoEncoderConfiguration(configuration)
    }

    private fun joinChannel() {
        // Initialize token, extra info here before joining channel
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name and uid that
        // you use to generate this token.
        val channelName = intent.getStringExtra(Constants.KEY_CHANNEL_NAME)
        role = intent.getIntExtra(
            Constants.KEY_CLIENT_ROLE,
            io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE
        )
        liveStreamId = intent.getStringExtra(Constants.KEY_CHANNEL_ID) ?: ""
        val liveStreamResult: LiveStreamResult = intent.getParcelableExtra(
            "liveStreamData") ?: LiveStreamResult()

        config().channelName = channelName
        var token: String? = agora_access_token
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null // default, no token
        }

        // Sets the channel profile of the Agora RtcEngine.
        // The Agora RtcEngine differentiates channel profiles and applies different optimization algorithms accordingly. For example, it prioritizes smoothness and low latency for a video call, and prioritizes video quality for a video broadcast.
        rtcEngine()?.setChannelProfile(io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        rtcEngine()?.enableVideo()
        configVideo()

        // Initialize token, extra info here before joining channel
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name and uid that
        // you use to generate this token.
        val appId = BuildConfig.private_app_id
        val appCertificate = BuildConfig.agora_primary_certificate
        val timestamp = (System.currentTimeMillis() / 1000 + 24 * 3600).toInt()
        /*val rtcTokenBuilder = RtcTokenBuilder()
        val tokenDataRTC = rtcTokenBuilder.buildTokenWithUserAccount(appId, appCertificate, "Test", "", if (config().userRole == io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER) RtcTokenBuilder.Role.Role_Publisher else RtcTokenBuilder.Role.Role_Subscriber, timestamp)
        println("!@@@tokenDataRTC = $tokenDataRTC")*/

        val rtmTokenBuilder = RtmTokenBuilder()
        try {
            val tokenData: String = rtmTokenBuilder.buildToken(
                appId,
                appCertificate,
                config().channelName,
                if (role == io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER) RtmTokenBuilder.Role.Rtm_User else RtmTokenBuilder.Role.Rtm_Audience,
                timestamp
            )
            println("!@@@tokenData = $tokenData")
            rtcEngine()?.joinChannel(tokenData, config().channelName, "", 0)
            /*rtcPublishHelper().publishVideo()
            rtcPublishHelper().publishAudio()*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
        /*rtcEngine().joinChannel(
            token,
            "Test",
            "",
            0
        )*/
    }

    protected fun prepareRtcVideo(
        uid: Int,
        local: Boolean
    ): TextureView {
        // Render local/remote video on a SurfaceView
        val surface: TextureView = RtcEngine.CreateTextureView(applicationContext)
        surface.isOpaque = false
        if (local) {
            rtcEngine()?.setupLocalVideo(
                VideoCanvas(
                    surface,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    0,
                    Constants.VIDEO_MIRROR_MODES[config().mirrorLocalIndex]
                )
            )
        } else {
            rtcEngine()?.setupRemoteVideo(
                VideoCanvas(
                    surface,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    uid,
                    Constants.VIDEO_MIRROR_MODES[config().mirrorRemoteIndex]
                )
            )
        }
        return surface
    }

    protected fun removeRtcVideo(uid: Int, local: Boolean) {
        if (local) {
            if (role == io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER) {
                stopRecord()
                stopSecondRecord()
                callDeleteLiveStreamApi(liveStreamId.toInt())
            }
            rtcEngine()?.setupLocalVideo(null)
        } else {
            if (role == io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE) {
                Comman_Methods.isCustomPopUpShow(this@RtcBaseActivity,
                    message = resources.getString(R.string.str_stream_end),
                    negativeButtonText = resources.getString(R.string.str_go_back),
                    singleNegative = true,
                    positiveButtonListener = object : PositiveButtonListener{
                        override fun okClickListener() {}

                        override fun cancelClickLister() {
                            callDeleteLiveStreamApi(liveStreamId.toInt())
                        }
                    })
            }
            rtcEngine()?.setupRemoteVideo(VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecord()
        stopSecondRecord()
        removeRtcEventHandler(this)
        rtcEngine()?.leaveChannel()
    }

    private fun callDeleteLiveStreamApi(id: Int) {
        try {
            val dataRole = role == io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE
            Utils.deleteLiveStreamApi(this@RtcBaseActivity, id, object : CommonApiListener {
                override fun commonResponse(
                    status: Boolean,
                    message: String,
                    responseMessage: String,
                    result: String
                ) {
                    try {
                        if (dataRole) {
                            finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, dataRole)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRecord(surfaceView: TextureView) {
        val loginObject: LoginObject = appDatabase.loginDao().getAll()
        val liveStreamDuration = loginObject.liveStreamDuration ?: 15
        val liveStreamMilli = liveStreamDuration * 60000
        val root = Utils.getStorageRootPath(this@RtcBaseActivity)
        if (!root.exists()) {
            root.mkdir()
        }
        val subFolder = File(root, "/LiveStream/")
        if (!subFolder.exists()) {
            subFolder.mkdir()
        }
        val timeStamp =
            SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date())
        mediaFile = File(
            subFolder.path + File.separator +
                    "VID_" + timeStamp + ".mp4"
        )
        val width = getScreenWidth()
        val height = getScreenHeight()

        videoRecorder = ViewRecorder()
        videoRecorder?.apply {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                    setVideoSource(MediaRecorder.VideoSource.DEFAULT)
                } else {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                }
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoFrameRate(5) // 5fps
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setMaxDuration(liveStreamMilli)
                setVideoEncodingBitRate(2000 * 1000)
                setOutputFile(mediaFile?.absolutePath ?: "")
                setOnErrorListener(mOnErrorListener)
                setRecordedView(surfaceView)
                setOnInfoListener { mr, what, extra ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecord()
//                        startSecondRecord(surfaceView)
                    }
                }
                try {
                    prepare()
                    start()
                } catch (e: Exception) {
                    Log.e("", "startRecord failed", e)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
        }
        Log.d("", "startRecord successfully!")
    }

    fun startSecondRecord(surfaceView: TextureView) {
        val loginObject: LoginObject = appDatabase.loginDao().getAll()
        val liveStreamDuration = loginObject.liveStreamDuration ?: 15
        val liveStreamMilli = (liveStreamDuration * 60000).toLong()
        val root = Utils.getStorageRootPath(this@RtcBaseActivity)
        if (!root.exists()) {
            root.mkdir()
        }
        val subFolder = File(root, "/LiveStream/")
        if (!subFolder.exists()) {
            subFolder.mkdir()
        }
        val timeStamp =
            SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date())
        secondMediaFile = File(
            subFolder.path + File.separator +
                    "VID_" + timeStamp + ".mp4"
        )
        val width = getScreenWidth()
        val height = getScreenHeight()
        val maxDuration = liveStreamMilli - 15000

        secondVideoRecorder = ViewRecorder()
        secondVideoRecorder?.apply {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                    setVideoSource(MediaRecorder.VideoSource.DEFAULT)
                } else {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                }
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoFrameRate(5) // 5fps
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setMaxDuration(maxDuration.toInt())
                setVideoEncodingBitRate(2000 * 1000)
                setOutputFile(secondMediaFile?.absolutePath ?: "")
                setOnErrorListener(mOnSecondErrorListener)
                setRecordedView(surfaceView)
                setOnInfoListener { mr, what, extra ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopSecondRecord()
                    }
                }
                try {
                    prepare()
                    start()
                } catch (e: Exception) {
                    Log.e("", "startSecondRecord failed", e)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
        }
        Log.d("", "startSecondRecord successfully!")
    }

    private val mOnErrorListener =
        MediaRecorder.OnErrorListener { mr, what, extra ->
            Log.e(
                "",
                "MediaRecorder error: type = $what, code = $extra"
            )
            videoRecorder?.apply {
                reset()
                release()
            }
        }

    private val mOnSecondErrorListener =
        MediaRecorder.OnErrorListener { mr, what, extra ->
            Log.e(
                "",
                "SecondMediaRecorder error: type = $what, code = $extra"
            )
            secondVideoRecorder?.apply {
                reset()
                release()
            }
        }

    fun stopRecord() {
        try {
            videoRecorder?.apply {
                stopMediaRecorders(this@apply)
            }
            if (!apiCalled) {
                if (role == io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER) {
                    apiCalled = true
//                    callSendMailEmergency()
                }
            }
            videoRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("", "stopRecord successfully!")
    }

    fun stopSecondRecord() {
        try {
            secondVideoRecorder?.apply {
                stopMediaRecorders(this@apply)
            }
            secondVideoRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("", "stopSecondRecord successfully!")
    }

    private fun stopMediaRecorders(viewRecorder: ViewRecorder) {
        try {
            viewRecorder.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            viewRecorder.reset()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            viewRecorder.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun callSendMailEmergency() {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val gpsTracker = GpsTracker(applicationContext)
        val loginObject: LoginObject = appDatabase.loginDao().getAll()
        executor.execute {
            var currentAddress = ""
            var emergencyLat = 0.0
            var emergencyLng = 0.0

            if (gpsTracker.CheckForLoCation()) {
                emergencyLat = gpsTracker.getLatitude()
                emergencyLng = gpsTracker.getLongitude()

                currentAddress =
                    if (emergencyLat != 0.0 && emergencyLng != 0.0) {
                        Utils.getCompleteAddressString(
                            applicationContext,
                            emergencyLat,
                            emergencyLng
                        )
                    } else {
                        ""
                    }
            }

            if (ConnectionUtil.isInternetAvailable(applicationContext)) {
                val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
                val memberID =
                    if (loginObject.isAdmin) "0".toRequestBody(MEDIA_TYPE_TEXT) else loginObject.memberID.toString()
                        .toRequestBody(
                            MEDIA_TYPE_TEXT
                        )
                val adminId =
                    if (loginObject.isAdmin) loginObject.memberID.toString().toRequestBody(
                        MEDIA_TYPE_TEXT
                    ) else loginObject.adminID.toString().toRequestBody(MEDIA_TYPE_TEXT)
                val address = currentAddress.toRequestBody(MEDIA_TYPE_TEXT)
                val isVideo = "true".toRequestBody(MEDIA_TYPE_TEXT)
                val isAdmin =
                    if (loginObject.isAdmin) "true".toRequestBody(MEDIA_TYPE_TEXT) else "false".toRequestBody(
                        MEDIA_TYPE_TEXT
                    )
                val loginByApp = "2".toRequestBody(MEDIA_TYPE_TEXT)
                val lat =
                    DecimalFormat("#.######", decimalSymbols).format(emergencyLat).toRequestBody(
                        MEDIA_TYPE_TEXT
                    )
                val lng =
                    DecimalFormat("#.######", decimalSymbols).format(emergencyLng).toRequestBody(
                        MEDIA_TYPE_TEXT
                    )
                val imageBody: RequestBody
                val part: MultipartBody.Part = when {
                    mediaFile != null -> {
                        if (mediaFile?.exists() == true) {
                            imageBody =
                                mediaFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("File", mediaFile?.name, imageBody)
                        } else {
                            imageBody =
                                nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("File", null, imageBody)
                        }
                    }
                    else -> {
                        imageBody =
                            nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("File", null, imageBody)
                    }
                }
                val memberListCall = WebApiClient.getInstance(applicationContext)
                    .webApi_without?.sendEmailForEmergency(
                        memberID,
                        adminId,
                        address,
                        isVideo,
                        isAdmin,
                        lat,
                        lng,
                        loginByApp,
                        part
                    )
                memberListCall?.enqueue(object : retrofit2.Callback<ApiResponse> {
                    override fun onResponse(
                        call: Call<ApiResponse>,
                        response: Response<ApiResponse>
                    ) {}

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                })
            }
        }
    }
}