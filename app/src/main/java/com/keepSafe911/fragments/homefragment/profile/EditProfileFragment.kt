package com.keepSafe911.fragments.homefragment.profile

import ValidationUtil.Companion.isRequiredField
import ValidationUtil.Companion.isValidEmail
import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.PhoneCountryCode
import com.keepSafe911.model.response.UpdateProfileResponse
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.compressImage
import com.keepSafe911.utils.Comman_Methods.Companion.createDir
import com.keepSafe911.utils.Comman_Methods.Companion.createSignatureDir
import com.keepSafe911.utils.Comman_Methods.Companion.deleteDir
import com.keepSafe911.utils.Comman_Methods.Companion.stringToURL
import com.keepSafe911.webservices.WebApiClient
import com.yanzhenjie.album.Album
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import kotlinx.android.synthetic.main.raw_country_search.view.*
import kotlinx.android.synthetic.main.search_country.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class EditProfileFragment : HomeBaseFragment(), View.OnClickListener {


    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    lateinit var appDatabase: OldMe911Database
    var imageFile: File? = null
    private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
    lateinit var part: MultipartBody.Part
    lateinit var dialog: Dialog
    private var countryCode: String = ""
    private var updateImage: String = ""
    private lateinit var removeProfileDialog: BottomSheetDialog


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
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.disableDrawer()

        val loginObject = appDatabase.loginDao().getAll()
        val image_url = loginObject.profilePath ?: ""

        try {
            val image_name = image_url.split("Family/")
            updateImage = if (image_url != "") if (image_name[1]!="") image_url.split("Family/")[1] else "" else ""
            val file_path =
                "" + mActivity.cacheDir + CACHE_FOLDER_NAME + updateImage
            val file = File(file_path)

            if (file.exists()) {
                sdvUserImage.setImageURI(Uri.fromFile(file).toString())
            } else {
                createDir(mActivity)
                downLoadTask(stringToURL(image_url))
            }
        }catch (e: Exception){
            e.printStackTrace()
        }

        storeCountryCode()
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etEditFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etEditLastName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etEditPhoneNumber.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etEditEmail.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etEditFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etEditLastName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etEditPhoneNumber.imeOptions = EditorInfo.IME_ACTION_NEXT
            etEditEmail.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        etEditUserName.setText(loginObject.userName ?: "")
        etEditEmail.setText(loginObject.email ?: "")
        etEditFirstName.setText(loginObject.firstName ?: "")
        val referralCode = loginObject.ReferralCode ?: ""
        if (referralCode == ""){
            btnCopy.visibility = View.GONE
        }else{
            tvRefferal.text = referralCode
            btnCopy.visibility = View.VISIBLE
        }

        etEditLastName.setText(loginObject.lastName ?: "")
        val mobileNumber = loginObject.mobile ?: ""
        if (mobileNumber.length > 10) {
            etEditPhoneNumber.setText(mobileNumber.takeLast(10))
            countryCode = mobileNumber.dropLast(10)
        } else {
            countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
            etEditPhoneNumber.setText(mobileNumber)
        }

        setHeader()

        btnUpdateProfile.setOnClickListener(this)
        ivEditProfile.setOnClickListener(this)
        tvEditCountrySelected.setOnClickListener(this)
        btnCopy.setOnClickListener(this)

        if (countryCode != "") {
            for (i in 0 until phoneCountryCodes.size) {
                if (countryCode == phoneCountryCodes[i].countryCode) {
                    when (phoneCountryCodes[i].code) {
                        "CA" -> tvEditCountrySelected.text = "+1"
                        else -> tvEditCountrySelected.text = phoneCountryCodes[i].countryCode
                    }
                    tvEditCountrySelected.setCompoundDrawablesWithIntrinsicBounds(phoneCountryCodes[i].flag, 0, 0, 0)
                    return
                } else {
                    tvEditCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                    tvEditCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                        phoneCountryCodes[UNITED__CODE_POSITION].flag,
                        0,
                        0,
                        0
                    )
                }
            }
        } else {
            countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
            tvEditCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
            tvEditCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                phoneCountryCodes[UNITED__CODE_POSITION].flag,
                0,
                0,
                0
            )
        }


    }

    private fun storeCountryCode() {
        phoneCountryCodes = java.util.ArrayList()
        val stringBuffer = StringBuffer()
        var bufferedReader: BufferedReader? = null
        try {
            bufferedReader = BufferedReader(InputStreamReader(mActivity.assets.open("Countrylistwithdialcode.json")))
            var temp: String? = ""
            while (run {
                    temp = bufferedReader.readLine()
                    temp
                } != null)
                stringBuffer.append(temp)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bufferedReader?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        val myjsonString = stringBuffer.toString()
        try {
            val jsonObjMain = JSONObject(myjsonString)
            val jsonArray = jsonObjMain.getJSONArray("CountryDialcode")
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val countryName = jsonObject.getString("name")
                val dialCode = jsonObject.getString("dial_code")
                val countryCode = jsonObject.getString("code")
                phoneCountryCodes.add(
                    PhoneCountryCode(
                        countryName,
                        dialCode,
                        countryCode,
                        Utils.countryFlagWithCode(mActivity)[i].flag,
                        false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setHeader() {
        mActivity.disableDrawer()
        tvHeader.text = mActivity.resources.getString(R.string.edit_profile)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnUpdateProfile -> {
                mActivity.hideKeyboard()
                if (checkValidation()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    callUpdateProfile(updateImage)
//                    callVerifyEmailAPI()
                }
            }
            R.id.ivEditProfile -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (updateImage != "") {
                    pictureOption()
                } else {
                    setPermission()
                }
            }
            R.id.tvEditCountrySelected -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                storeCountryCode()
                openUploadCountryDialog(phoneCountryCodes)
            }
            R.id.btnCopy ->{
                btnCopy.text = resources.getString(R.string.text_copied)
                val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                val promoUrl = loginupdate.PromocodeUrl ?: ""
                setClipboard(promoUrl)
            }
        }
    }

    private fun setClipboard(text: String) {
        val clipboard = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            val clipboard =
                mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
            clipboard.text = text
        } else {
            val clipboard = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
        }*/
    }

    private fun pictureOption(){
        val view = LayoutInflater.from(mActivity)
            .inflate(R.layout.popup_picture_option_layout, mActivity.window.decorView.rootView as ViewGroup, false)
        if (this::removeProfileDialog.isInitialized){
            if (removeProfileDialog.isShowing){
                removeProfileDialog.dismiss()
            }
        }
        removeProfileDialog = BottomSheetDialog(mActivity)
        removeProfileDialog.setContentView(view)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.isHideable = false
        removeProfileDialog.setOnShowListener {
            mBehavior.peekHeight = view.height
        }
        val ivPictureCancel: ImageView? = removeProfileDialog.findViewById(R.id.ivPictureCancel)
        val fabAlbumOption: FloatingActionButton? = removeProfileDialog.findViewById(R.id.fabAlbumOption)
        val fabRemoveOption: FloatingActionButton? = removeProfileDialog.findViewById(R.id.fabRemoveOption)
        val tvAlbumOptionName: TextView? = removeProfileDialog.findViewById(R.id.tvAlbumOptionName)
        val tvRemoveOptionName: TextView? = removeProfileDialog.findViewById(R.id.tvRemoveOptionName)
        ivPictureCancel?.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            removeProfileDialog.dismiss()
        }
        fabRemoveOption?.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            callRemoveProfileImage(appDatabase.loginDao().getAll().memberID)
            removeProfileDialog.dismiss()
        }
        tvRemoveOptionName?.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            callRemoveProfileImage(appDatabase.loginDao().getAll().memberID)
            removeProfileDialog.dismiss()
        }
        fabAlbumOption?.setOnClickListener {
            mActivity.hideKeyboard()
            removeProfileDialog.dismiss()
            Comman_Methods.avoidDoubleClicks(fabAlbumOption)
            setPermission()
        }
        tvAlbumOptionName?.setOnClickListener {
            mActivity.hideKeyboard()
            removeProfileDialog.dismiss()
            Comman_Methods.avoidDoubleClicks(tvAlbumOptionName)
            setPermission()
        }
        removeProfileDialog.show()
    }

    private fun callRemoveProfileImage(memberId: Int) {
        mActivity.isSpeedAvailable()
        Utils.removeUserProfileImageApi(mActivity, memberId, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                if (status) {
//                    gotoPaymentScreen()
                    imageFile = null
                    updateImage = ""
                    sdvUserImage.loadFrescoImageFromFile(mActivity, File(""), 1)
                    val loginUpdate: LoginObject = appDatabase.loginDao().getAll()
                    loginUpdate.profilePath =
                        "https://apiyfsn.azurewebsites.net/Uploads/Family/"
                    appDatabase.loginDao().updateLogin(loginUpdate)


                    mActivity.setProfile(appDatabase.loginDao().getAll())
                    val loginData = appDatabase.loginDao().getAll()
                    if (appDatabase.memberDao().getAllMember().isNotEmpty()) {
                        for (element in appDatabase.memberDao().getAllMember()) {
                            if (element.iD == loginData.memberID) {
                                element.image = loginData.profilePath
                                appDatabase.memberDao().updateMember(element)
                            }
                        }
                    }

                    Comman_Methods.isProgressHide()
//                            mActivity.onBackPressed()

                    if (imageFile == null) {
                        val file =
                            File("" + mActivity.cacheDir + CACHE_FOLDER_NAME + USER_IMAGE_NAME)
                        sdvUserImage.setImageURI(Uri.fromFile(file).toString())
                    } else {
                        if (imageFile != null) {
                            if (imageFile?.exists() == true) {
                                imageFile?.delete()
                                if (imageFile?.exists() == true) {
                                    imageFile?.canonicalFile?.delete()
                                }
                            }
                        }
                        val fileImage =
                            File("" + mActivity.cacheDir + CACHE_FOLDER_NAME)
                        if (fileImage.exists()) {
                            deleteDir(fileImage)
                        }
                        createDir(mActivity)
                        downLoadTask(stringToURL(appDatabase.loginDao().getAll().profilePath ?: ""))
                    }
                } else {
                    Comman_Methods.isProgressHide()
                }
            }
        }, true)
    }

    private fun callVerifyEmailAPI() {
        mActivity.isSpeedAvailable()
        val main = JsonObject()
        main.addProperty("id",appDatabase.loginDao().getAll().memberID)
        main.addProperty("Email", etEditEmail.text.toString().trim())
        main.addProperty("Username", etEditUserName.text.toString().trim())
        main.addProperty("Mobile", if (etEditPhoneNumber.text.toString().trim().isNotEmpty()) countryCode + etEditPhoneNumber.text.toString().trim() else "")

        Utils.verifyUserData(mActivity, main, object : CommonApiListener {
            override fun commonResponse(status: Boolean, message: String, responseMessage: String, result: String) {
                if (status) {
                    callUpdateProfile(updateImage)
                }
            }
        })
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
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
        Album.image(mActivity) // Image and video mix options.
            .singleChoice() // Multi-Mode, Single-Mode: singleChoice().
            .columnCount(3) // The number of columns in the page list.
            .camera(true) // Whether the camera appears in the Item.
            .onResult { result ->
                if (result != null) {
                    if (result.size > 0) {
                        println("result[0].mimeType = ${result[0].mimeType}")
                        if (result[0].mimeType.contains("image")) {
                            if (result[0].path != null) {
                                println("result[0].path = ${result[0].path}")
                                if (result[0].path != "") {
                                    imageFile = File(result[0].path)
                                    if (imageFile?.exists() == true) {
                                        updateImage = imageFile?.name ?: ""
                                        sdvUserImage.loadFrescoImageFromFile(
                                            mActivity,
                                            imageFile,
                                            1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .onCancel { }
            .start()
    }

    private fun checkValidation(): Boolean {
        return when {
            etEditFirstName.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_first))
                false
            }
            etEditLastName.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_last))
                false
            }
            etEditEmail.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_email))
                false
            }
            !isValidEmail(etEditEmail.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.val_email))
                false
            }
            /*etEditPhoneNumber.text.toString().trim().isEmpty() -> {
                mActivity.showMwssage(mActivity.resources.getString(R.string.blank_phone))
                false
            }*/
            isRequiredField(etEditPhoneNumber.text.toString()) && etEditPhoneNumber.text.toString().trim().length != 10 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.phone_length))
                false
            }
            else -> true
        }
    }

    private fun callUpdateProfile(image_name: String) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val memberIDRequestBody =
                appDatabase.loginDao().getAll().memberID.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val firstNameRequestBody =
                etEditFirstName.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val lastNameRequestBody =
                etEditLastName.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val emailRequestBody =
                etEditEmail.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val phoneNumberRequestBody =
                (if(etEditPhoneNumber.text.toString().trim().isNotEmpty()) countryCode + etEditPhoneNumber.text.toString().trim() else "").toRequestBody(
                    MEDIA_TYPE_TEXT
                )
            val imageNameRequestBody: RequestBody

            part = when {
                imageFile != null -> {
                    imageFile = File(compressImage(imageFile?.absolutePath ?: "", mActivity))
                    if (imageFile?.exists() == true) {
                        imageNameRequestBody = "".toRequestBody(MEDIA_TYPE_TEXT)
                        val imageBody: RequestBody = imageFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("Image", imageFile?.name, imageBody)
                    } else {
                        imageNameRequestBody = image_name.toRequestBody(MEDIA_TYPE_TEXT)
                        val imageBody: RequestBody =
                            "".toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("Image", null, imageBody)
                    }
                }
                else -> {
                    imageNameRequestBody = image_name.toRequestBody(MEDIA_TYPE_TEXT)
                    val imageBody: RequestBody =
                        "".toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("Image", null, imageBody)
                }
            }


            val callUpdateProfileApi = WebApiClient.getInstance(mActivity).webApi_with_MultiPart?.updateProfile(
                memberIDRequestBody, firstNameRequestBody,
                lastNameRequestBody, phoneNumberRequestBody, emailRequestBody, imageNameRequestBody, part
            )
            callUpdateProfileApi?.enqueue(object : Callback<UpdateProfileResponse> {
                override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<UpdateProfileResponse>, response: Response<UpdateProfileResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {

                            response.body()?.let {
                                if (it.isStatus) {
                                    val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                                    loginupdate.firstName = etEditFirstName.text.toString().trim()
                                    loginupdate.lastName = etEditLastName.text.toString().trim()
                                    loginupdate.mobile =
                                        if (etEditPhoneNumber.text.toString().trim().isNotEmpty()) countryCode + etEditPhoneNumber.text.toString().trim() else ""
                                    loginupdate.email = etEditEmail.text.toString().trim()

                                    if (it.result != null) {
                                        loginupdate.profilePath =
                                            if (it.result?.url != null) it.result?.url else ""
                                    }
                                    appDatabase.loginDao().updateLogin(loginupdate)

                                    mActivity.setProfile(appDatabase.loginDao().getAll())
                                    val loginData = appDatabase.loginDao().getAll()
                                    if (appDatabase.memberDao().getAllMember().isNotEmpty()) {
                                        for (memberData in appDatabase.memberDao().getAllMember()) {
                                            if (memberData.iD == loginData.memberID) {
                                                memberData.image = loginData.profilePath
                                                appDatabase.memberDao().updateMember(memberData)
                                            }
                                        }
                                    }

                                    Comman_Methods.isProgressHide()
//                            mActivity.onBackPressed()

                                    if (imageFile == null) {
                                        val file =
                                            File("" + mActivity.cacheDir + CACHE_FOLDER_NAME + USER_IMAGE_NAME)
                                        sdvUserImage.setImageURI(Uri.fromFile(file).toString())
                                    } else {
                                        if (imageFile != null) {
                                            if (imageFile?.exists() == true) {
                                                imageFile?.delete()
                                                if (imageFile?.exists() == true) {
                                                    imageFile?.canonicalFile?.delete()
                                                }
                                            }
                                        }
                                        val fileImage =
                                            File("" + mActivity.cacheDir + CACHE_FOLDER_NAME)
                                        if (fileImage.exists()) {
                                            deleteDir(fileImage)
                                        }
                                        createDir(mActivity)
                                        downLoadTask(stringToURL(appDatabase.loginDao().getAll().profilePath ?: ""))
                                    }
                                    mActivity.onBackPressed()
                                    mActivity.showMessage(it.responseMessage ?: "")
                                } else {
                                    Comman_Methods.isProgressHide()
                                    mActivity.showMessage(it.responseMessage ?: "")
                                }
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

    private fun downLoadTask(url: URL?) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            var downloadBitmap: Bitmap? = null
            var connection: HttpURLConnection? = null

            try {
                connection = url?.openConnection() as HttpURLConnection
                connection.connect()
                val inputStream = connection.inputStream
                val bufferedInputStream = BufferedInputStream(inputStream)
                downloadBitmap = BitmapFactory.decodeStream(bufferedInputStream)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Disconnect the http url connection
                connection?.disconnect()
            }
            handler.post {
                if (downloadBitmap != null) {
                    // Display the downloaded image into ImageView
                    // sdv_profile_image.setImageBitmap(downloadBitmap);


                    // Save bitmap to internal storage
                    try {

                        val degrees = 0f
                        val matrix = Matrix()
                        matrix.setRotate(degrees)
                        val bOutput = Bitmap.createBitmap(downloadBitmap, 0, 0, downloadBitmap.width, downloadBitmap.height, matrix, true)
                        val imageInternalUri = saveImageToInternalStorage(bOutput)

                        val file = File(imageInternalUri.toString())
                        if (file != null) {
                            val uri: Uri = Uri.fromFile(file)

                            if (uri != null) {

                                sdvUserImage.setImageURI(uri.toString())
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    // Notify user that an error occurred while downloading image
                }
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        var fileName = "ProfileImage.jpg"
        var file: File? = null
        if (appDatabase.loginDao().getAll() != null) {
            val loginObject = appDatabase.loginDao().getAll()
            val profilePath = loginObject.profilePath ?: ""
            if (profilePath.split("Family/")[1] != "") {
                fileName = profilePath.split("Family/")[1]
            }
            file = createSignatureDir(mActivity, fileName)
            try {
                // Initialize a new OutputStream
                var stream: OutputStream? = null

                // If the output file exists, it can be replaced or appended to it
                stream = FileOutputStream(file)

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)


                // Flushes the stream
                stream.flush()

                // Closes the stream
                stream.close()


                Comman_Methods.isProgressHide()
//            mActivity.setUpdatedImage()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Parse the gallery image url to uri

        // Return the saved image Uri
        return Uri.parse(if (file != null) file.absolutePath else "")
    }

    private fun openUploadCountryDialog(phoneCountryCodes: ArrayList<PhoneCountryCode>) {
        val inflater = layoutInflater
        val dialogLayout1 = inflater.inflate(R.layout.search_country, null)
        val mDialog = android.app.AlertDialog.Builder(activity)
        mDialog.setView(dialogLayout1)

        if (this::dialog.isInitialized) {
            if (dialog != null) {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }

        dialog = mDialog.create()
        dialog.window?.attributes?.windowAnimations = R.style.animationForDialog

        dialogLayout1.tvSearchCountryClose.setOnClickListener { dialog.dismiss() }
        dialogLayout1.rvCountryNameCodeFlag.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        val adapter = CountrySelectionAdapter(mActivity, phoneCountryCodes)
        dialogLayout1.rvCountryNameCodeFlag.adapter = adapter
        dialogLayout1.tvSearchCountryHeader.text = mActivity.resources.getString(R.string.select_country)
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            dialogLayout1.etSearchCountry.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            dialogLayout1.etSearchCountry.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        dialogLayout1.etSearchCountry.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }

        })
        dialog.setCancelable(false)
        dialog.show()
    }

    inner class CountrySelectionAdapter() : RecyclerView.Adapter<CountrySelectionAdapter.CountrySelectionHolder>(),
        Filterable {

        lateinit var context: HomeActivity
        private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
        private var duplicatePhoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
        private var adapterPosition = -1

        constructor(context: HomeActivity, phoneCountryCodes: ArrayList<PhoneCountryCode>) : this() {
            this.context = context
            this.phoneCountryCodes = phoneCountryCodes
            this.duplicatePhoneCountryCodes = ArrayList()
            this.duplicatePhoneCountryCodes.addAll(phoneCountryCodes)
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): CountrySelectionHolder {
            return CountrySelectionHolder(LayoutInflater.from(context).inflate(R.layout.raw_country_search, p0, false))
        }

        override fun getItemCount(): Int {
            return duplicatePhoneCountryCodes.size
        }

        override fun onBindViewHolder(p0: CountrySelectionHolder, p1: Int) {
            when (duplicatePhoneCountryCodes[p1].code) {
                "CA" -> p0.tvCountryDetails.text =
                    duplicatePhoneCountryCodes[p1].countryName + " (+1)"
                else -> p0.tvCountryDetails.text =
                    duplicatePhoneCountryCodes[p1].countryName + " (" + duplicatePhoneCountryCodes[p1].countryCode + ")"
            }
            p0.tvCountryDetails.setCompoundDrawablesWithIntrinsicBounds(duplicatePhoneCountryCodes[p1].flag, 0, 0, 0)
            p0.tvCountryDetails.setOnClickListener {
                context.hideKeyboard()
                adapterPosition = p0.bindingAdapterPosition
                notifyDataSetChanged()
            }
            if (adapterPosition == p1) {
                duplicatePhoneCountryCodes[p1].isSelected = true
                dialog.dismiss()
            } else {
                duplicatePhoneCountryCodes[p1].isSelected = false
            }
            displaySelectedCountry(duplicatePhoneCountryCodes)
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence): FilterResults {
                    val charString = charSequence.toString()
                    duplicatePhoneCountryCodes = if (charString.isEmpty()) {
                        phoneCountryCodes
                    } else {
                        val filterList = ArrayList<PhoneCountryCode>()
                        for (row in phoneCountryCodes) {
                            if (row.countryName.lowercase().contains(charString.lowercase()) || row.countryCode.lowercase().contains(
                                    charString.lowercase()
                                )
                            ) {
                                filterList.add(row)
                            }
                        }
                        filterList
                    }
                    val filterResults = FilterResults()
                    filterResults.values = duplicatePhoneCountryCodes
                    return filterResults
                }

                override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                    if (filterResults.values != null) {
                        duplicatePhoneCountryCodes = filterResults.values as ArrayList<PhoneCountryCode>
                    }
                    notifyDataSetChanged()
                }
            }
        }

        inner class CountrySelectionHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvCountryDetails: TextView = view.tvCountryDetails
        }
    }

    private fun displaySelectedCountry(duplicatePhoneCountryCodes: ArrayList<PhoneCountryCode>) {
        for (i in 0 until duplicatePhoneCountryCodes.size) {
            if (duplicatePhoneCountryCodes[i].isSelected) {
                when {
                    duplicatePhoneCountryCodes[i].code == "CA" -> tvEditCountrySelected.text = "+1"
                    else -> tvEditCountrySelected.text = duplicatePhoneCountryCodes[i].countryCode
                }
                countryCode = duplicatePhoneCountryCodes[i].countryCode
                tvEditCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                    duplicatePhoneCountryCodes[i].flag,
                    0,
                    0,
                    0
                )
            }
        }
    }
}