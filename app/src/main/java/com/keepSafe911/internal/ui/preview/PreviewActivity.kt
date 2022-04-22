package com.keepSafe911.internal.ui.preview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.keepSafe911.R
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.internal.configuration.AnncaConfiguration
import com.keepSafe911.internal.configuration.AnncaConfiguration.MediaAction
import com.keepSafe911.internal.ui.BaseAnncaActivity
import com.keepSafe911.internal.ui.view.AspectFrameLayout
import com.keepSafe911.internal.utils.AnncaImageLoader
import com.keepSafe911.internal.utils.Utils
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.Comman_Methods
import com.keepSafe911.utils.ConnectionUtil
import com.keepSafe911.utils.MEDIA_TYPE_TEXT
import com.keepSafe911.utils.nullData
import com.keepSafe911.webservices.WebApiClient
import kotlinx.android.synthetic.main.activity_preview.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Created by memfis on 7/6/16.
 */
class PreviewActivity : AppCompatActivity(),
    View.OnClickListener {
    private var mediaAction = 0
    private var previewFilePath: String? = null
    private var sendEmail: Boolean? = false
    private var surfaceView: SurfaceView? = null
    private var photoPreviewContainer: FrameLayout? = null
    private var imagePreview: ImageView? = null
    private var buttonPanel: ViewGroup? = null
    private var videoPreviewContainer: AspectFrameLayout? = null
    private var cropMediaAction: View? = null
    private var ratioChanger: TextView? = null
    private var mediaController: MediaController? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlaybackPosition = 0
    private var isVideoPlaying = true
    private var currentRatioIndex = 0
    lateinit var ratios: FloatArray
    lateinit var ratioLabels: Array<String>
    var uploadFile: File? = null
    lateinit var appDatabase: OldMe911Database
    var gpsTracker: GpsTracker? = null

    private val surfaceCallbacks: SurfaceHolder.Callback =
        object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                showVideoPreview(holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        }
    private val MediaPlayerControlImpl: MediaController.MediaPlayerControl =
        object : MediaController.MediaPlayerControl {
            override fun start() {
                mediaPlayer?.start()
            }

            override fun pause() {
                mediaPlayer?.pause()
            }

            override fun getDuration(): Int {
                return mediaPlayer?.duration ?: 0
            }

            override fun getCurrentPosition(): Int {
                return mediaPlayer?.currentPosition ?: 0
            }

            override fun seekTo(pos: Int) {
                mediaPlayer?.seekTo(pos)
            }

            override fun isPlaying(): Boolean {
                return mediaPlayer?.isPlaying ?: false
            }

            override fun getBufferPercentage(): Int {
                return 0
            }

            override fun canPause(): Boolean {
                return true
            }

            override fun canSeekBackward(): Boolean {
                return true
            }

            override fun canSeekForward(): Boolean {
                return true
            }

            override fun getAudioSessionId(): Int {
                return mediaPlayer?.audioSessionId ?: 0
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        appDatabase = OldMe911Database.getDatabase(this)
        gpsTracker = GpsTracker(this@PreviewActivity)
        val originalRatioLabel =
            getString(R.string.preview_controls_original_ratio_label)
        ratioLabels = arrayOf(originalRatioLabel, "1:1", "4:3", "16:9")
        ratios = floatArrayOf(0f, 1f, 4f / 3f, 16f / 9f)
        surfaceView =
            findViewById<View>(R.id.video_preview) as SurfaceView
        surfaceView?.setOnTouchListener(View.OnTouchListener { v, event ->
            if (mediaController == null) return@OnTouchListener false
            if (mediaController?.isShowing == true) {
                mediaController?.hide()
                showButtonPanel(true)
            } else {
                showButtonPanel(false)
                mediaController?.show()
            }
            false
        })
        videoPreviewContainer =
            findViewById<View>(R.id.previewAspectFrameLayout) as AspectFrameLayout
        photoPreviewContainer =
            findViewById<View>(R.id.photo_preview_container) as FrameLayout
        buttonPanel =
            findViewById<View>(R.id.preview_control_panel) as ViewGroup
        val confirmMediaResult =
            findViewById<View>(R.id.confirm_media_result)
        val reTakeMedia =
            findViewById<View>(R.id.re_take_media)
        val cancelMediaAction =
            findViewById<View>(R.id.cancel_media_action)
        cropMediaAction = findViewById(R.id.crop_image)
        ratioChanger =
            findViewById<View>(R.id.ratio_image) as TextView
        ratioChanger?.setOnClickListener {
            currentRatioIndex = (currentRatioIndex + 1) % ratios.size
            ratioChanger?.text = ratioLabels[currentRatioIndex]
        }
        cropMediaAction?.visibility = View.GONE
        ratioChanger?.visibility = View.GONE
        confirmMediaResult?.visibility = View.GONE
        cropMediaAction?.setOnClickListener { }
        confirmMediaResult.setOnClickListener(this)
        reTakeMedia?.setOnClickListener(this)
        cancelMediaAction?.setOnClickListener(this)
        val args = intent.extras

        mediaAction = args?.getInt(MEDIA_ACTION_ARG) ?: 0
        previewFilePath = args?.getString(FILE_PATH_ARG) ?: ""
        sendEmail = args?.getBoolean(SEND_EMAIL) ?: false

        if (sendEmail == true) {
            videoRecording()
        }

        if (mediaAction == AnncaConfiguration.MEDIA_ACTION_VIDEO) {
            displayVideo(savedInstanceState)
        } else if (mediaAction == AnncaConfiguration.MEDIA_ACTION_PHOTO) {
            displayImage()
        } else {
            val mimeType =
                Utils.getMimeType(previewFilePath)
            if (mimeType.contains(MIME_TYPE_VIDEO)) {
                displayVideo(savedInstanceState)
            } else if (mimeType.contains(MIME_TYPE_IMAGE)) {
                displayImage()
            } else finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveVideoParams(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
        if (mediaController != null) {
            mediaController?.hide()
            mediaController = null
        }
    }

    private fun displayImage() {
        videoPreviewContainer?.visibility = View.GONE
        surfaceView?.visibility = View.GONE
        showImagePreview()
        ratioChanger?.text = ratioLabels[currentRatioIndex]
    }

    private fun showImagePreview() {
        imagePreview = ImageView(this)
        val builder = AnncaImageLoader.Builder(this)
        builder.load(previewFilePath).build().into(imagePreview)
        photoPreviewContainer?.removeAllViews()
        photoPreviewContainer?.addView(imagePreview)
    }

    private fun displayVideo(savedInstanceState: Bundle?) {
        cropMediaAction?.visibility = View.GONE
        ratioChanger?.visibility = View.GONE
        savedInstanceState?.let { loadVideoParams(it) }
        photoPreviewContainer?.visibility = View.GONE
        surfaceView?.holder?.addCallback(surfaceCallbacks)
    }

    private fun showVideoPreview(holder: SurfaceHolder) {
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(previewFilePath)
            mediaPlayer?.setDisplay(holder)
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer?.setOnPreparedListener { mp ->
                mediaController = MediaController(this@PreviewActivity)
                mediaController?.setAnchorView(surfaceView)
                mediaController?.setMediaPlayer(MediaPlayerControlImpl)
                val videoWidth = mp.videoWidth
                val videoHeight = mp.videoHeight
                videoPreviewContainer?.setAspectRatio(videoWidth.toDouble() / videoHeight)
                mediaPlayer?.start()
                mediaPlayer?.seekTo(currentPlaybackPosition)
                if (!isVideoPlaying) mediaPlayer?.pause()
            }
            mediaPlayer?.setOnErrorListener { mp, what, extra ->
                finish()
                true
            }
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error media player playing video.")
            finish()
        }
    }

    private fun saveVideoParams(outState: Bundle) {
        if (mediaPlayer != null) {
            outState.putInt(
                VIDEO_POSITION_ARG,
                mediaPlayer?.currentPosition ?: 0
            )
            outState.putBoolean(
                VIDEO_IS_PLAYED_ARG,
                mediaPlayer?.isPlaying ?: false
            )
        }
    }

    private fun loadVideoParams(savedInstanceState: Bundle) {
        currentPlaybackPosition =
            savedInstanceState.getInt(VIDEO_POSITION_ARG, 0)
        isVideoPlaying =
            savedInstanceState.getBoolean(VIDEO_IS_PLAYED_ARG, true)
    }

    private fun showButtonPanel(show: Boolean) {
        if (show) {
            buttonPanel?.visibility = View.VISIBLE
        } else {
            buttonPanel?.visibility = View.GONE
        }
    }

    override fun onClick(view: View) {
        val resultIntent = Intent()
        when (view.id) {
            R.id.confirm_media_result -> {
                resultIntent.putExtra(
                    RESPONSE_CODE_ARG,
                    BaseAnncaActivity.ACTION_CONFIRM
                ).putExtra(FILE_PATH_ARG, previewFilePath)
            }
            R.id.re_take_media -> {
//                deleteMediaFile()
                resultIntent.putExtra(
                    RESPONSE_CODE_ARG,
                    BaseAnncaActivity.ACTION_RETAKE
                )
            }
            R.id.cancel_media_action -> {
//                deleteMediaFile()
                resultIntent.putExtra(
                    RESPONSE_CODE_ARG,
                    BaseAnncaActivity.ACTION_CANCEL
                )
            }
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
//        deleteMediaFile()
        val resultIntent = Intent()
        resultIntent.putExtra(
            RESPONSE_CODE_ARG,
            BaseAnncaActivity.ACTION_CANCEL
        )
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
//        super.onBackPressed()
    }

    private fun deleteMediaFile(): Boolean {
        val mediaFile = File(previewFilePath)
        return if (mediaFile.exists()) {
            mediaFile.delete()
        } else {
            false
        }
    }

    companion object {
        private const val TAG = "PreviewActivity"
        private const val MEDIA_ACTION_ARG = "media_action_arg"
        private const val FILE_PATH_ARG = "file_path_arg"
        private const val SEND_EMAIL = "send_email"
        private const val RESPONSE_CODE_ARG = "response_code_arg"
        private const val VIDEO_POSITION_ARG = "current_video_position"
        private const val VIDEO_IS_PLAYED_ARG = "is_played"
        private const val MIME_TYPE_VIDEO = "video"
        private const val MIME_TYPE_IMAGE = "image"
        @JvmStatic
        fun newIntent(
            context: Context?,
            @MediaAction mediaAction: Int,
            filePath: String?,
            sendEmail: Boolean
        ): Intent {
            return Intent(context, PreviewActivity::class.java)
                .putExtra(MEDIA_ACTION_ARG, mediaAction)
                .putExtra(FILE_PATH_ARG, filePath)
                .putExtra(SEND_EMAIL, sendEmail)
        }

        @JvmStatic
        fun isResultConfirm(resultIntent: Intent): Boolean {
            return BaseAnncaActivity.ACTION_CONFIRM == resultIntent.getIntExtra(
                RESPONSE_CODE_ARG,
                -1
            )
        }

        @JvmStatic
        fun getMediaFilePatch(resultIntent: Intent): String {
            return resultIntent.getStringExtra(FILE_PATH_ARG) ?: ""
        }

        @JvmStatic
        fun isResultRetake(resultIntent: Intent): Boolean {
            return BaseAnncaActivity.ACTION_RETAKE == resultIntent.getIntExtra(
                RESPONSE_CODE_ARG,
                -1
            )
        }

        @JvmStatic
        fun isResultCancel(resultIntent: Intent): Boolean {
            return BaseAnncaActivity.ACTION_CANCEL == resultIntent.getIntExtra(
                RESPONSE_CODE_ARG,
                -1
            )
        }
    }

    private fun videoRecording() {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()

        executor.execute {
            try {
                if (File(previewFilePath).exists()) {
                    uploadFile = File(previewFilePath)
                    callSendMailEmergency()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
    private fun callSendMailEmergency() {
        if (ConnectionUtil.isInternetAvailable(this)) {
            val loginObject: LoginObject = appDatabase.loginDao().getAll()
            val latitude = gpsTracker?.getLatitude() ?: 0.0
            val longitude = gpsTracker?.getLongitude() ?: 0.0
            val currentAddress: String =
                if (latitude != 0.0 && longitude != 0.0) {
                    com.keepSafe911.utils.Utils.getCompleteAddressString(this, latitude, longitude)
                } else {
                    val loginAddress = loginObject.locationAddress ?: ""
                    if (loginAddress!="") loginAddress else
                        com.keepSafe911.utils.Utils.getCompleteAddressString(this, loginObject.latitude, loginObject.longitude)
                }

            val emergencyLat: Double = if (latitude != 0.0) {
                latitude
            } else {
                loginObject.latitude
            }

            val emergencyLng: Double = if (longitude != 0.0) {
                longitude
            } else {
                loginObject.longitude
            }

            val memberID = if (loginObject.isAdmin) "0".toRequestBody(MEDIA_TYPE_TEXT) else loginObject.memberID.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val adminId = if (loginObject.isAdmin) loginObject.memberID.toString().toRequestBody(MEDIA_TYPE_TEXT) else loginObject.adminID.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val address = currentAddress.toRequestBody(MEDIA_TYPE_TEXT)
            val isVideo = "true".toRequestBody(MEDIA_TYPE_TEXT)
            val isAdmin = if (loginObject.isAdmin) "true".toRequestBody(MEDIA_TYPE_TEXT) else "false".toRequestBody(MEDIA_TYPE_TEXT)
            val loginByApp = "2".toRequestBody(MEDIA_TYPE_TEXT)
            val lat = emergencyLat.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val lng = emergencyLng.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val imageBody: RequestBody
            val part: MultipartBody.Part = when {
                uploadFile != null -> {
                    if (uploadFile?.exists() == true) {
                        imageBody = uploadFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("File", uploadFile?.name, imageBody)
                    } else {
                        imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("File", null, imageBody)
                    }
                }
                else -> {
                    imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("File", null, imageBody)
                }
            }
            val memberListCall = WebApiClient.getInstance(this)
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
                ) {
                    val statusCode: Int = response.code()

                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status) {
                                    if (uploadFile != null) {
                                        if (uploadFile?.exists() == true) {
                                            uploadFile?.delete()
                                        }
                                    }
                                    showMessage(it.result)
                                } else {
                                    showMessage(resources.getString(R.string.oops_error_message))
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showMessage(resources.getString(R.string.oops_error_message))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }
            })
        } else {
            showMessage(this.resources.getString(R.string.no_internet))
        }
    }

    fun showMessage(message: String) {
        if (previewCoordinator != null) {
            if (message != "") {
                val snack = Snackbar.make(previewCoordinator, message, Snackbar.LENGTH_SHORT)
                snack.show()
            }
        }
    }
}