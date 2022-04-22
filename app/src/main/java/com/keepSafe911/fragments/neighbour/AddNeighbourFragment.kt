package com.keepSafe911.fragments.neighbour

import addFragment
import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide

import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.model.IncidentType
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.webservices.WebApiClient
import com.kotlinpermissions.KotlinPermissions
import com.yanzhenjie.album.Album
import java.io.File
import java.util.*
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.keepSafe911.BuildConfig
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.response.*
import com.keepSafe911.utils.*
import com.vincent.videocompressor.VideoCompress
import getScreenHeight
import getScreenWidth
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_add_neighbour.*
import kotlinx.android.synthetic.main.toolbar_header.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"

//• = \u2022, ● = \u25CF, ○ = \u25CB, ▪ = \u25AA, ■ = \u25A0, □ = \u25A1, ► = \u25BA
class AddNeighbourFragment : HomeBaseFragment(), View.OnClickListener {

    private val IMAGE = 0
    private val VIDEO = 1
    private val NOFILE = 2

    lateinit var appDatabase: OldMe911Database
    var uploadFile: File? = null
    var videoFile: File? = null
    private var gpstracker: GpsTracker? = null
    private var incidentTypeList: ArrayList<IncidentType> = ArrayList()
    var secondLatitude: Double = 0.0
    var secondLongitude: Double = 0.0
    lateinit var part: MultipartBody.Part
    var neighborRequest: FeedResult = FeedResult()
    var fullName: String = ""

    private var placeVisitLatitude = 0.0
    private var placeVisitLongitude = 0.0
    private var placeDetail = PlacesResult()
    private var sharingFileName: String = ""
    var isFromActivity: Boolean = false
    var videoFileDelete: File? = null
    var isFromPlace: Boolean = false


    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    companion object{

        fun newInstance(
            isFromPlace: Boolean = false, placeDetail: PlacesResult = PlacesResult(), fileName: String = "", isFromActivity: Boolean = false
        ): AddNeighbourFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isFromPlace)
            args.putParcelable(ARG_PARAM2,placeDetail)
            args.putString(ARG_PARAM3, fileName)
            args.putBoolean(ARG_PARAM4, isFromActivity)
            val fragment = AddNeighbourFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFromPlace = it.getBoolean(ARG_PARAM1,false)
            placeDetail = it.getParcelable(ARG_PARAM2) ?: PlacesResult()
            sharingFileName = it.getString(ARG_PARAM3, "")
            isFromActivity = it.getBoolean(ARG_PARAM4,false)
            placeVisitLatitude = placeDetail.latitude ?: 0.0
            placeVisitLongitude = placeDetail.longitude ?: 0.0
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mActivity.requestedOrientation =
            if (mActivity.resources.getBoolean(R.bool.isTablet)) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LocaleUtils.setLocale(
            Locale(
                if (AppPreference
                        .getIntPreference(requireActivity()
                            , BuildConfig.languagePrefKey) == 0
                ) LocaleUtils.LAN_ENGLISH else LocaleUtils.LAN_SPANISH
            )
        )
        LocaleUtils.updateConfig(mActivity, mActivity.resources.configuration)
        /*mActivity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
        return inflater.inflate(R.layout.fragment_add_neighbour, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        fullName = appDatabase.loginDao().getAll().firstName+" "+appDatabase.loginDao().getAll().lastName
        neighborRequest = FeedResult()
        Places.initialize(mActivity, mActivity.resources.getString(R.string.firebase_live_key))
        setHeader()
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etNeighbourTitle.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            edNeighbourDescription.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        }else{
            etNeighbourTitle.imeOptions = EditorInfo.IME_ACTION_NEXT
            edNeighbourDescription.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        gpstracker = GpsTracker(mActivity)
        if (isFromPlace){
            etNeighbourTitle.setText(placeDetail.address)
            edNeighbourDescription.setText(mActivity.resources.getString(R.string.str_place_rating)+placeDetail.rating?.toString())
            if (placeVisitLatitude!=0.0 && placeVisitLongitude!=0.0){
                neighborRequest.location =
                    Utils.getCompleteAddressString(mActivity, placeVisitLatitude, placeVisitLongitude)
                neighborRequest._lat = placeVisitLatitude
                neighborRequest._long = placeVisitLongitude
                tvIncidentAddr.text = neighborRequest.location ?: ""
            }else{
                val latitude = gpstracker?.getLatitude() ?: 0.0
                val longitude = gpstracker?.getLongitude() ?: 0.0
                if (latitude != 0.0 && longitude != 0.0) {
                    neighborRequest.location =
                        Utils.getCompleteAddressString(mActivity, latitude, longitude)
                    neighborRequest._lat = latitude
                    neighborRequest._long = longitude
                    tvIncidentAddr.text = neighborRequest.location ?: ""
                } else {
                    tvIncidentAddr.text = ""
                }
            }
        }else {
            tvIncidentType?.text = ""
            etNeighbourTitle.setText("")
            edNeighbourDescription.setText("")
            val latitude = gpstracker?.getLatitude() ?: 0.0
            val longitude = gpstracker?.getLongitude() ?: 0.0
            if (latitude != 0.0 && longitude != 0.0) {
                neighborRequest.location =
                    Utils.getCompleteAddressString(mActivity, latitude, longitude)
                neighborRequest._lat = latitude
                neighborRequest._long = longitude
                tvIncidentAddr.text = neighborRequest.location ?: ""
            } else {
                tvIncidentAddr.text = ""
            }
        }
        callCategoryApi()
        secondLatitude = gpstracker?.getLatitude() ?: 0.0
        secondLongitude = gpstracker?.getLongitude() ?: 0.0
        btnAddPhotoVideo.setOnClickListener(this)
        rlIncidentAddress.setOnClickListener(this)
        tvIncidentType?.setOnClickListener(this)
        if (sharingFileName.isNotEmpty()) {
            videoFileAdjustment(sharingFileName, 1)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.btnAddPhotoVideo -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                setPermission()
            }
            R.id.rlIncidentAddress -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                rlIncidentAddress.isEnabled = false
                rlIncidentAddress.isClickable = false
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val autocompleteIntent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(mActivity)
//                val builder = PlacePicker.IntentBuilder()
                try {
                    // for fragment
//                    startActivityForResult(builder.build(mActivity), PLACE_PICKER_REQUEST)
                    resultLauncher.launch(autocompleteIntent)
                } catch (e: GooglePlayServicesRepairableException) {
                    e.printStackTrace()
                } catch (e: GooglePlayServicesNotAvailableException) {
                    e.printStackTrace()
                } catch (e: Exception){
                    e.printStackTrace()
                }

            }
            R.id.tvIncidentType -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                openStatePicker(mActivity,incidentTypeList,tvIncidentType)
            }
        }
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        rlIncidentAddress.isEnabled = true
        rlIncidentAddress.isClickable = true
        if (result.resultCode == RESULT_OK) {
            // Get the user's selected place from the Intent.
            val data = result.data
            if (data != null) {
                //                        val place = PlacePicker.getPlace(mActivity, data)
                val place = Autocomplete.getPlaceFromIntent(data)
                val placeName = place.name ?: ""
                val placeAddress = place.address ?: ""
                secondLatitude = place.latLng?.latitude ?: 0.0
                secondLongitude = place.latLng?.longitude ?: 0.0
                if (secondLatitude!=0.0 && secondLongitude!=0.0) {
                    tvIncidentAddr.text = placeName + if (placeAddress.isNotEmpty()) "\n" + placeAddress else ""
                    neighborRequest.location =  placeName + if (placeAddress.isNotEmpty()) "\n" + placeAddress else ""
                    neighborRequest._lat = place.latLng?.latitude ?: 0.0
                    neighborRequest._long = place.latLng?.longitude ?: 0.0
                }else{
                    tvIncidentAddr.text = ""
                }
            }
        }
    }

    private fun callCategoryApi(){
        mActivity.isSpeedAvailable()
        Utils.categoryList(mActivity, object : CommonApiListener {
            override fun categoryList(
                status: Boolean,
                categoryResult: ArrayList<CategoryResult>,
                message: String
            ) {
                if (status) {
                    if (categoryResult.size > 0) {
                        incidentTypeList = ArrayList()
                        for (i in 0 until categoryResult.size) {
                            var color = 0
                            when (categoryResult[i].id ?: 0) {
                                1 -> {
                                    color = R.color.caldroid_yellow
                                }
                                2 -> {
                                    color = R.color.color_red
                                }
                                3 -> {
                                    color = R.color.special_green
                                }
                                4 -> {
                                    color = android.R.color.holo_purple
                                }
                                5 -> {
                                    color = R.color.event_color_04
                                }
                                6 -> {
                                    color = R.color.color_purple
                                }
                                else -> {
                                    color = R.color.Date_bg
                                }
                            }
                            incidentTypeList.add(
                                IncidentType(
                                    categoryResult[i].id ?: 0,
                                    color,
                                    categoryResult[i].name ?: "",
                                    true
                                )
                            )
                        }
                        if (isFromPlace) {
                            tvIncidentType?.text = incidentTypeList[0].incidentText
                            neighborRequest.type = incidentTypeList[0].incidentText
                            neighborRequest.categoryID =
                                incidentTypeList[0].incidentID
                        } else {
                            tvIncidentType?.text = ""
                        }
                    } else {
                        addIncidentTypes()
                    }
                }
            }

            override fun onFailureResult() {
                addIncidentTypes()
            }
        })
    }
    private fun addIncidentTypes() {
        incidentTypeList = ArrayList()
        incidentTypeList.add(IncidentType(1,R.color.caldroid_yellow,mActivity.resources.getString(R.string.str_news),true))
        incidentTypeList.add(IncidentType(2,R.color.caldroid_light_red,mActivity.resources.getString(R.string.str_crime),true))
        incidentTypeList.add(IncidentType(3,R.color.special_green,mActivity.resources.getString(R.string.str_safety),true))
        incidentTypeList.add(IncidentType(4,android.R.color.holo_purple,mActivity.resources.getString(R.string.str_suspicious),true))
        incidentTypeList.add(IncidentType(5,R.color.event_color_04,mActivity.resources.getString(R.string.str_stranger),true))
        incidentTypeList.add(IncidentType(6,R.color.color_purple,mActivity.resources.getString(R.string.str_lost_pet),true))
        if (isFromPlace) {
            tvIncidentType?.text = incidentTypeList[0].incidentText
            neighborRequest.type = incidentTypeList[0].incidentText
            neighborRequest.categoryID = incidentTypeList[0].incidentID
        }else{
            tvIncidentType?.text = ""
        }
    }

    private fun setHeader() {

        tvHeader.text = mActivity.resources.getString(R.string.neighbour_post)
        Utils.setTextGradientColor(tvHeader)
        tvShare.visibility = View.VISIBLE
        iv_home_close.visibility = View.VISIBLE
        iv_home_close.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            deleteVideoFile()
            mActivity.onBackPressed()
        }
        tvShare.setOnClickListener {
            mActivity.hideKeyboard()
            if (checkValidation()) {
                Comman_Methods.avoidDoubleClicks(it)
                neighborRequest.title = etNeighbourTitle.text.toString().trim()
                neighborRequest.feeds = edNeighbourDescription.text.toString().trim()
                callAddNeighborApi()
            }
        }
        mActivity.checkUserActive()
    }

    private fun deleteVideoFile() {
        if (sharingFileName.isEmpty()) {
            videoFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
    }

    private fun callAddNeighborApi() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val idRequestBody: RequestBody =
                (neighborRequest.iD ?: 0).toString().toRequestBody(MEDIA_TYPE_TEXT)
            val memberIDRequestBody =
                appDatabase.loginDao().getAll().memberID.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val titleRequestBody = (neighborRequest.title ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val feedRequestBody = (neighborRequest.feeds ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val locationRequestBody = (neighborRequest.location ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val latRequestBody: RequestBody =
                (neighborRequest._lat ?: 0.0).toString().toRequestBody(MEDIA_TYPE_TEXT)
            val lngRequestBody: RequestBody =
                (neighborRequest._long ?: 0.0).toString().toRequestBody(MEDIA_TYPE_TEXT)
            val typeRequestBody: RequestBody = (neighborRequest.type ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val categoryRequestBody: RequestBody =
                (neighborRequest.categoryID ?: 0).toString().toRequestBody(MEDIA_TYPE_TEXT)
            val createdOnRequestBody: RequestBody =
                Utils.getCurrentTimeStamp().toRequestBody(MEDIA_TYPE_TEXT)

            val fileTypeRequestBody: RequestBody
            part = when {
                uploadFile != null -> {
                    if (neighborRequest.fileType == IMAGE){
                        uploadFile = File(Comman_Methods.compressImage(uploadFile?.absolutePath ?: "", mActivity))
                    }
                    if (uploadFile?.exists() == true) {
                        fileTypeRequestBody =
                            neighborRequest.fileType.toString().toRequestBody(MEDIA_TYPE_TEXT)
                        val imageBody: RequestBody =
                            uploadFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("Image", uploadFile?.name, imageBody)
                    } else {
                        fileTypeRequestBody = NOFILE.toString().toRequestBody(MEDIA_TYPE_TEXT)
                        val imageBody: RequestBody =
                            "".toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("Image", null, imageBody)
                    }
                }
                else -> {
                    fileTypeRequestBody = NOFILE.toString().toRequestBody(MEDIA_TYPE_TEXT)
                    val imageBody: RequestBody =
                        "".toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("Image", null, imageBody)
                }
            }

            val callAddFeedApi = WebApiClient.getInstance(mActivity).webApi_with_MultiPart?.feedPost(
                idRequestBody,titleRequestBody,feedRequestBody,locationRequestBody,latRequestBody,
                lngRequestBody,typeRequestBody,memberIDRequestBody,categoryRequestBody,fileTypeRequestBody,createdOnRequestBody, part
            )

            callAddFeedApi?.enqueue(object : retrofit2.Callback<FeedResponse>{
                override fun onFailure(call: Call<FeedResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<FeedResponse>, response: Response<FeedResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    it.result?.let { feedResult ->
                                        if (feedResult.fileType == VIDEO) {
                                            deleteVideoFile()
                                        }
                                        /*val feedResponseResult: FeedResponseResult = FeedResponseResult()
                                feedResponseResult.isLiked = false
                                feedResponseResult.fileType = feedResult.fileType
                                feedResponseResult.file = if (feedResult.imageUrl!=null) feedResult.imageUrl else ""
                                feedResponseResult._lat = feedResult._lat
                                feedResponseResult._long = feedResult._long
                                feedResponseResult.addedBy = fullName
                                feedResponseResult.categoryID = feedResult.categoryID
                                for (i in 0 until incidentTypeList.size){
                                    if (feedResult.categoryID== incidentTypeList[i].incidentID){
                                        feedResponseResult.categoryName = incidentTypeList[i].incidentText
                                    }
                                }
                                feedResponseResult.commentCount = 0
                                feedResponseResult.createdBy = feedResult.createdBy
                                feedResponseResult.createdOn = feedResult.createdOn
                                feedResponseResult.feeds = feedResult.feeds
                                feedResponseResult.iD = feedResult.iD
                                feedResponseResult.isCommented = false
                                feedResponseResult.isDeleted = feedResult.isDeleted
                                feedResponseResult.likeCount = 0
                                feedResponseResult.location = feedResult.location
                                feedResponseResult.lstOfFeedLikeOrComments = ArrayList()
                                feedResponseResult.title = feedResult.title
                                feedResponseResult.type = feedResult.type
                                uploadObjectList.add(0,feedResponseResult)*/
                                    }
//                                    mActivity.onBackPressed()
                                    mActivity.addFragment(
                                        NeighbourFragment(),
                                        true,
                                        true,
                                        animationType = AnimationType.bottomtotop
                                    )
                                }
                                mActivity.showMessage(it.responseMessage ?: "")
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun setPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 2) {
                        Utils.muteRecognizer(mActivity, true)
                        openCamera()
                    }
                }
                .onDenied {
                    setPermission()
                }
                .onForeverDenied {
                    mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                }
                .ask()
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 3) {
                        Utils.muteRecognizer(mActivity, true)
                        openCamera()
                    }
                }
                .onDenied {
                    setPermission()
                }
                .onForeverDenied {
                    mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                }
                .ask()
        }
    }

    private fun openCamera() {
        Album.album(mActivity) // Image and video mix options.
            .singleChoice() // Multi-Mode, Single-Mode: singleChoice().
            .columnCount(3) // The number of columns in the page list.
            .camera(false) // Whether the camera appears in the Item.
            .cameraVideoQuality(1) // Video quality, [0, 1].
            .cameraVideoLimitDuration(Long.MAX_VALUE) // The longest duration of the video is in milliseconds.
            .cameraVideoLimitBytes(Long.MAX_VALUE) // Maximum size of the video, in bytes.
            .filterDuration { attributes -> attributes!! > 60001 }
            .filterSize { filter -> filter > 71 * 1024 * 1024 }
            .afterFilterVisibility(false)
            .onResult { result ->
                Utils.muteRecognizer(mActivity)
                println("result[0].path = ${result[0].path}")
                println("result[0].mimeType = ${result[0].mimeType}")
                if (result[0].mimeType.contains("video")) {
                    videoFileAdjustment(result[0].path, 0)
                } else if (result[0].mimeType.contains("image")) {
                    neighborRequest.fileType = IMAGE
                    flNeighbourFile.visibility = View.VISIBLE
                    Glide.with(mActivity).load(result[0].path)
                        .into(ivNeighbourImageFile)
                    uploadFile = File(result[0].path)
                }
//                        btnAddPhotoVideo.text = mActivity.resources.getString(R.string.update_photo_video)
            }
            .onCancel { }
            .start()
    }

    private fun videoFileAdjustment(fileName: String, type : Int) {
        neighborRequest.fileType = VIDEO
        uploadFile = File(fileName)
        val width = if (type > 0) mActivity.getScreenWidth(false) else 0
        val height = if (type > 0) mActivity.getScreenHeight(false) else 0
        try {
            val root = Utils.getStorageRootPath(mActivity)
            if (!root.exists()) {
                root.mkdir()
            }
            videoFile = File(root, File(fileName).name)
            if (videoFile?.exists() == false) {
                val videoCompress = VideoCompress.compressVideoLow(fileName,
                    videoFile?.path ?: "", height, width,
                    object : VideoCompress.CompressListener {
                        override fun onStart() {
                            Comman_Methods.isProgressShow(mActivity)
                        }

                        override fun onSuccess() {
                            Comman_Methods.isProgressHide()
                            uploadFile = videoFile
                            videoFileDelete = videoFile
                        }

                        override fun onFail() {
                            Comman_Methods.isProgressHide()
                        }

                        override fun onProgress(percent: Float) {
                            Comman_Methods.isProgressShow(mActivity)
                        }

                    })
            } else {
                uploadFile = videoFile
                videoFileDelete = videoFile
            }
            /*// Two Types is there TYPE_MEDIACODEC and TYPE_FFMPEG
        GiraffeCompressor.create(TYPE_MEDIACODEC)
            .input(File(result[0].path))
            .output(videoFile)
            .bitRate(3000000)
            .resizeFactor(1.0F)
            .ready()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object: Subscriber<GiraffeCompressor.Result>(){
                override fun onNext(t: GiraffeCompressor.Result?) {
                    val msg = String.format("compress completed \ntake time:%s \nout put file:%s", t?.costTime, t?.output)
                    System.out.println(msg)
                    System.out.println(t?.output)
                    System.out.println(File(t?.output).length()/1024)
                }

                override fun onCompleted() {
                    Comman_Methods.isProgressHide()
                    uploadFile = videoFile

                }

                override fun onError(e: Throwable?) {
                    e?.printStackTrace()
                    flNeighbourFile.visibility = View.GONE
                    Comman_Methods.isProgressHide()
                }
            })*/
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val thumbSize = Size(100, 100)
                ThumbnailUtils.createVideoThumbnail(
                    File(fileName),
                    thumbSize, CancellationSignal()
                )
            } else {
                ThumbnailUtils.createVideoThumbnail(
                    fileName,
                    MediaStore.Video.Thumbnails.MICRO_KIND
                )
            }
            if (bitmap != null) {
                flNeighbourFile.visibility = View.VISIBLE
                Glide.with(mActivity).load(bitmap)
                    .into(ivNeighbourImageFile)
            } else {
                flNeighbourFile.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            flNeighbourFile.visibility = View.GONE
        }
    }

    private fun checkValidation(): Boolean{
        return when {
            etNeighbourTitle.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_post_title))
                false
            }
            edNeighbourDescription.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_post_desc))
                false
            }
            tvIncidentAddr.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_incident_loc))
                false
            }
            tvIncidentType?.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_incident_type))
                false
            }
            /*uploadFile==null -> {
                mActivity.showMwssage(mActivity.resources.getString(R.string.blank_post_file))
                false
            }*/
            else -> true
        }
    }

    private fun openStatePicker(
        context: Context,
        country: ArrayList<IncidentType>,
        editText: TextView
    ) {
        val dialogLayout = layoutInflater.inflate(R.layout.popup_state_picker, null)
        val btnDone = dialogLayout.findViewById<Button>(R.id.btnDone)
        val statePicker = dialogLayout.findViewById<NumberPicker>(R.id.statePicker)
        statePicker.minValue = 0
        statePicker.maxValue = country.size - 1
        statePicker.wrapSelectorWheel = false
        statePicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        val incidentTypeString = ArrayList<String>()
        for (i in 0 until country.size){
            incidentTypeString.add(country[i].incidentText)
        }
        statePicker.displayedValues = incidentTypeString.toTypedArray()
        if (editText.text.toString().trim() != "") {
            statePicker.value = incidentTypeString.indexOf(editText.text.toString().trim())
        }
        val bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setContentView(dialogLayout)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(dialogLayout.parent as View)
        mBehavior.isHideable = false
        bottomSheetDialog.setOnShowListener {
            mBehavior.peekHeight = dialogLayout.height
        }
        statePicker.setOnValueChangedListener { numberPicker, i, i1 -> editText.text = country[i1].incidentText }
        btnDone.setOnClickListener { v ->
            editText.text = country[statePicker.value].incidentText
            neighborRequest.type = country[statePicker.value].incidentText
            neighborRequest.categoryID = country[statePicker.value].incidentID
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.show()
    }
}
