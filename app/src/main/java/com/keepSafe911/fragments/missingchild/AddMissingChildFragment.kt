package com.keepSafe911.fragments.missingchild

import ValidationUtil.Companion.isRequiredField
import addFragment
import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.HomeActivity
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.PhoneCountryCode
import com.keepSafe911.model.response.CommonValidationResponse
import com.keepSafe911.model.response.findmissingchild.MatchResult
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import com.yanzhenjie.album.Album
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_add_missing_child.*
import kotlinx.android.synthetic.main.fragment_add_missing_child.tvCountrySelected
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
import retrofit2.Response
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"

class AddMissingChildFragment : HomeBaseFragment(), View.OnClickListener {

    private var param1: String? = ""
    private var param2: String? = ""
    private var param3: String? = ""
    private var param4: Boolean = false
    lateinit var appDatabase: OldMe911Database
    var imageFile: File? = null
    private lateinit var datePickerDialog: DatePickerDialog
    private var dateFormatter: SimpleDateFormat? = null
    private var dateParse: Date? = null
    private var missingCity: String = ""
    private var missingState: String = ""
    private var countryCode: String = ""
    private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
    lateinit var dialog: Dialog

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1, "")
            param2 = it.getString(ARG_PARAM2, "")
            param3 = it.getString(ARG_PARAM3, "")
            param4 = it.getBoolean(ARG_PARAM4, false)
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
        return inflater.inflate(R.layout.fragment_add_missing_child, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        storeCountryCode()
        Places.initialize(mActivity, mActivity.resources.getString(R.string.firebase_live_key))
        img_missing_photoUpload.setOnClickListener(this)
        btn_add_missing_data.setOnClickListener(this)
        etMissingBirthDate.setOnClickListener(this)
        etMissingSince.setOnClickListener(this)
        etMissingFrom.setOnClickListener(this)
        tvCountrySelected.setOnClickListener(this)
        countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
        tvCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
        tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
            phoneCountryCodes[UNITED__CODE_POSITION].flag,
            0,
            0,
            0
        )
        etHeight.filters = arrayOf(
            InputFilterMinMax(
                0.0,
                Double.MAX_VALUE
            ),
            DecimalDigitsInputFilter(5,2, etHeight)
        )
        etWeight.filters = arrayOf(
            InputFilterMinMax(
                0.0,
                Double.MAX_VALUE
            ),
            DecimalDigitsInputFilter(5,2, etWeight)
        )
        rbPhysicalYes.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                flPhysicalAttributes.visibility = View.VISIBLE
            } else {
                flPhysicalAttributes.visibility = View.GONE
            }
        }
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etMissingFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etMissingLastName.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etMissingAge.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etMissingSituation.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etMissingPhone.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etHairColor.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etEyeColor.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etHeight.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etWeight.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etComplexion.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etPhysicalAttributes.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etMissingFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etMissingLastName.imeOptions = EditorInfo.IME_ACTION_DONE
            etMissingAge.imeOptions = EditorInfo.IME_ACTION_DONE
            etMissingSituation.imeOptions = EditorInfo.IME_ACTION_NEXT
            etMissingPhone.imeOptions = EditorInfo.IME_ACTION_NEXT
            etHairColor.imeOptions = EditorInfo.IME_ACTION_NEXT
            etEyeColor.imeOptions = EditorInfo.IME_ACTION_NEXT
            etHeight.imeOptions = EditorInfo.IME_ACTION_NEXT
            etWeight.imeOptions = EditorInfo.IME_ACTION_NEXT
            etComplexion.imeOptions = EditorInfo.IME_ACTION_DONE
            etPhysicalAttributes.imeOptions = EditorInfo.IME_ACTION_DONE
        }
    }

    private fun setHeader() {
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.disableDrawer()
        tvHeader.setPadding(0, 0, 50, 0)
        tvHeader.text = mActivity.resources.getString(R.string.str_add_missing_child).uppercase()
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String, param3: String, isFrom: Boolean) =
            AddMissingChildFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                    putString(ARG_PARAM3, param3)
                    putBoolean(ARG_PARAM4, isFrom)
                }
            }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.img_missing_photoUpload -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                setPermission()
            }
            R.id.tvCountrySelected -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                storeCountryCode()
                openUploadCountryDialog(phoneCountryCodes)
            }
            R.id.btn_add_missing_data -> {
                mActivity.hideKeyboard()
                if (checkMissingChildDataValidation()) {
                    Comman_Methods.avoidDoubleClicks(v)

                    val formatter = SimpleDateFormat("MM-dd-yyyy")
                    var diagStartDate = ""
                    try {
                        var date1: Date? = null
                        if (etMissingSince.text.toString().isNotEmpty()) {
                            date1 = formatter.parse(etMissingSince.text.toString())
                        }
                        val target = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val missingChildData = MatchResult()
                    missingChildData.id = 0
                    missingChildData.age = etMissingAge.text.toString().trim().toInt()
                    missingChildData.caseNumber = ""
                    missingChildData.dateMissing = if (diagStartDate != "") diagStartDate else Utils.getCurrentTimeStamp()
                    missingChildData.firstName = etMissingFirstName.text.toString().trim()
                    missingChildData.lastName = etMissingLastName.text.toString().trim()
                    missingChildData.matchScore = 0.0
                    missingChildData.missingCity = missingCity
                    missingChildData.missingState = missingState
                    missingChildData.imageUrl = (imageFile?.absolutePath ?: "").toString()
                    missingChildData.imageName = ""
                    missingChildData.lastSeenSituation = etMissingSituation.text.toString().trim()
                    missingChildData.contactNumber = if (etMissingPhone.text.toString().trim().isNotEmpty()) countryCode + etMissingPhone.text.toString().trim() else ""
                    missingChildData.hairColor = etHairColor.text.toString().trim()
                    missingChildData.eyeColor = etEyeColor.text.toString().trim()
                    missingChildData.height = String.format("%.2f", etHeight.text.toString().toDouble()).toDouble()
                    missingChildData.weight = String.format("%.2f", etWeight.text.toString().toDouble()).toDouble()
                    missingChildData.complexion = etComplexion.text.toString().trim()

                    missingChildData.isWearLenses = rbGlassYes.isChecked
                    missingChildData.isBracesOnTeeth = rbTeethYes.isChecked
                    missingChildData.isPhysicalAttributes = rbPhysicalYes.isChecked
                    missingChildData.physicalAttributes = etPhysicalAttributes.text.toString().trim()

//                    mActivity.addFragment(MissingChildPaymentFragment.newInstance("", "", missingChildData), true, true, AnimationType.fadeInfadeOut)
                    callAddMissingChildApi(missingChildData)
                }
            }
            R.id.etMissingBirthDate -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                setDateTimeField(etMissingBirthDate, 1)
            }
            R.id.etMissingSince -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                setDateTimeField(etMissingSince, 2)
            }
            R.id.etMissingFrom -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                val fields = listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS_COMPONENTS
                )
                val autocompleteIntent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN,
                    fields
                )
                    .build(mActivity)
                try {
                    resultLauncher.launch(autocompleteIntent)
                } catch (e: GooglePlayServicesRepairableException) {
                    e.printStackTrace()
                } catch (e: GooglePlayServicesNotAvailableException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Get the user's selected place from the Intent.
            val data = result.data
            if (data != null) {
                val place = Autocomplete.getPlaceFromIntent(data)
                val placeName = place.name ?: ""
                val placeAddresses = place.address ?: ""
                val secondLatitude = place.latLng?.latitude ?: 0.0
                val secondLongitude = place.latLng?.longitude ?: 0.0
                if (secondLatitude != 0.0 && secondLongitude != 0.0) {
                    place.addressComponents?.let { addressComponents ->
//                                administrative_area_level_1
//                                locality or administrative_area_level_3
                        for (i in 0 until addressComponents.asList().size) {
                            val componentData = addressComponents.asList()[i]
                            if (componentData.types[0].contains("locality", ignoreCase = true)) {
                                missingCity = componentData.name
                            }
                            if (missingCity == "") {
                                if (componentData.types[0].contains("administrative_area_level_3", ignoreCase = true)) {
                                    missingCity = componentData.name
                                }
                            }
                            if (componentData.types[0].contains("administrative_area_level_1", ignoreCase = true)) {
                                missingState = componentData.shortName ?: ""
                            }
                        }
                    }
                    val placeAddress = if (placeAddresses.isNotEmpty()) {
                        if (placeAddresses.contains(placeName, ignoreCase = true)) {
                            placeAddresses
                        } else {
                            "$placeName, $placeAddresses"
                        }
                    } else {
                        placeName
                    }
                    etMissingFrom.setText(placeAddress)
                } else {
                    etMissingFrom.setText("")
                }
            }
        }
    }

    private fun setDateTimeField(editText: EditText, value: Int) {
        dateFormatter = SimpleDateFormat("MM-dd-yyyy", Locale.US)
        val newCalendar = Calendar.getInstance()
        if (editText.text.toString().trim().isNotEmpty()){
            try {
                newCalendar.time = dateFormatter?.parse(editText.text.toString().trim())!!
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        datePickerDialog = DatePickerDialog(
            mActivity,
            { view, year, monthOfYear, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, monthOfYear, dayOfMonth)
                editText.setText(dateFormatter?.format(newDate.time))
                if (value == 1) {
                    val currentDate = Calendar.getInstance()
                    currentDate.time = Date()
                    var diff: Int = currentDate.get(Calendar.YEAR) - newDate.get(Calendar.YEAR)
                    if (newDate.get(Calendar.MONTH) > currentDate.get(Calendar.MONTH) ||
                        newDate.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH) && newDate.get(
                            Calendar.DATE
                        ) > currentDate.get(Calendar.DATE)
                    ) {
                        diff--
                    }
                    if (etMissingBirthDate.text.toString().trim().isNotEmpty()) {
                        etMissingAge.isFocusableInTouchMode = false
                        if (diff > 0) {
                            etMissingAge.setText("" + diff)
                        } else {
                            etMissingAge.setText("0")
                        }
                    } else {
                        etMissingAge.isFocusableInTouchMode = true
                        etMissingAge.setText("")
                    }
                }
                try {
                    dateParse = dateFormatter?.parse(dateFormatter?.format(newDate.time))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            },
            newCalendar.get(Calendar.YEAR),
            newCalendar.get(Calendar.MONTH),
            newCalendar.get(Calendar.DAY_OF_MONTH)
        )
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            try {
                if (etMissingSince.text.toString().trim().isNotEmpty() && value == 1) {
                    datePickerDialog.datePicker.maxDate = dateFormatter?.parse(
                        etMissingSince.text.toString().trim()
                    )?.time ?: System.currentTimeMillis() - 1000
                } else if (value == 2 && etMissingBirthDate.text.toString().trim().isNotEmpty()) {
                    datePickerDialog.datePicker.minDate = dateFormatter?.parse(
                        etMissingBirthDate.text.toString().trim()
                    )?.time ?: System.currentTimeMillis() - 1000
                    datePickerDialog.datePicker.maxDate = System.currentTimeMillis() - 1000
                } else {
                    datePickerDialog.datePicker.maxDate = System.currentTimeMillis() - 1000
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        datePickerDialog.show()
    }

    private fun callAddMissingChildApi(missingChildData: MatchResult) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val imageBody: RequestBody
            val loginId = appDatabase.loginDao().getAll().memberID

            val missingId = (missingChildData.id ?: 0).toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingFirstName = (missingChildData.firstName ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingLastName = (missingChildData.lastName ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingCity = (missingChildData.missingCity ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingState = (missingChildData.missingState ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingDateMiss = (missingChildData.dateMissing ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingAge = missingChildData.age.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingToken = "".toRequestBody(MEDIA_TYPE_TEXT)
            val missingPayId = "".toRequestBody(MEDIA_TYPE_TEXT)
            val purchasePassword = "".toRequestBody(MEDIA_TYPE_TEXT)
            val accountNumber = "".toRequestBody(MEDIA_TYPE_TEXT)
            val deviceType = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val purchaseAmount = "0".toRequestBody(MEDIA_TYPE_TEXT)
            val lastSeenSituation = (missingChildData.lastSeenSituation ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val contactNumber = (missingChildData.contactNumber ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val hairColor = (missingChildData.hairColor ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val eyeColor = (missingChildData.eyeColor ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val height = String.format("%.2f",(missingChildData.height ?: 0.0)).toRequestBody(MEDIA_TYPE_TEXT)
            val weight = String.format("%.2f", (missingChildData.weight ?: 0.0)).toRequestBody(MEDIA_TYPE_TEXT)
            val complexion = (missingChildData.complexion ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val isWearLenses = if (missingChildData.isWearLenses == true) "true".toRequestBody(MEDIA_TYPE_TEXT) else "false".toRequestBody(MEDIA_TYPE_TEXT)
            val isBracesOnTeeth = if (missingChildData.isBracesOnTeeth == true) "true".toRequestBody(MEDIA_TYPE_TEXT) else "false".toRequestBody(MEDIA_TYPE_TEXT)
            val isPhysicalAttributes = if (missingChildData.isPhysicalAttributes == true) "true".toRequestBody(MEDIA_TYPE_TEXT) else "false".toRequestBody(MEDIA_TYPE_TEXT)
            val physicalAttributes = (missingChildData.physicalAttributes ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val userId = loginId.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val paymentDate = Utils.getCurrentTimeStamp().toRequestBody(MEDIA_TYPE_TEXT)
            val missingPart: MultipartBody.Part = when {
                imageFile != null -> {
                    if (imageFile?.exists() == true) {
                        imageBody = imageFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ImageName", imageFile?.name, imageBody)
                    } else {
                        imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ImageName", null, imageBody)
                    }
                }
                else -> {
                    imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("ImageName", null, imageBody)
                }
            }

            val callAddMissingChildDataApi = WebApiClient.getInstance(mActivity)
                .webApi_with_MultiPart?.addMissingChild(
                    missingId,
                    missingFirstName,
                    missingLastName,
                    missingCity,
                    missingState,
                    missingDateMiss,
                    missingAge,
                    missingToken,
                    missingPayId,
                    purchasePassword,
                    purchaseAmount,
                    paymentDate,
                    accountNumber,
                    deviceType,
                    userId,
                    lastSeenSituation,
                    contactNumber,
                    hairColor,
                    eyeColor,
                    height,
                    weight,
                    complexion,
                    isWearLenses,
                    isBracesOnTeeth,
                    isPhysicalAttributes,
                    physicalAttributes,
                    missingPart)

            callAddMissingChildDataApi?.enqueue(object: retrofit2.Callback<CommonValidationResponse>{
                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    val statusCode: Int = response.code()

                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                mActivity.showMessage(it.responseMessage ?: "")
                                if (it.status == true) {
//                                    mActivity.onBackPressed()
                                    val gson: Gson = GsonBuilder().create()
                                    val responseTypeToken: TypeToken<MatchResult> =
                                        object :
                                            TypeToken<MatchResult>() {}
                                    val responseData: MatchResult? =
                                        gson.fromJson(
                                            gson.toJson(it.result),
                                            responseTypeToken.type
                                        )
                                    val missingChildResult = responseData ?: MatchResult()

                                    mActivity.addFragment(MissingChildTaskFragment.newInstance(
                                        true, missingChildResult.id ?: 0,
                                        missingChildResult.lstChildTaskResponse ?: ArrayList(), missingChildResult.userId ?: 0),
                                        true, true,
                                        animationType = AnimationType.fadeInfadeOut)
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }

                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun checkMissingChildDataValidation(): Boolean {
        return when {
            !isRequiredField(etMissingFirstName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_first))
                false
            }
            !isRequiredField(etMissingLastName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_last))
                false
            }
            ((etMissingBirthDate.text.toString().isEmpty() && etMissingSince.text.toString().trim().isEmpty() && etMissingAge.text.toString().trim().isEmpty()) || (etMissingBirthDate.text.toString().isEmpty() && etMissingFrom.text.toString().trim().isEmpty() && etMissingAge.text.toString().trim().isEmpty())) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_birth))
                false
            }
            !isRequiredField(etMissingSince.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_date))
                false
            }
            !isRequiredField(etMissingFrom.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_place))
                false
            }
            etMissingBirthDate.text.toString().isEmpty() && etMissingAge.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_age))
                false
            }
            !isRequiredField(etMissingSituation.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_situation))
                false
            }
            !isRequiredField(etMissingPhone.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_phone))
                false
            }
            !isRequiredField(etHairColor.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_hair))
                false
            }
            !isRequiredField(etEyeColor.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_eye))
                false
            }
            !isRequiredField(etHeight.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_height))
                false
            }
            etHeight.text.toString().toDouble() <= 0.0 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.valid_missing_height))
                false
            }
            !isRequiredField(etWeight.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_weight))
                false
            }
            etWeight.text.toString().toDouble() <= 0.0 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.valid_missing_weight))
                false
            }
            !isRequiredField(etComplexion.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_complex))
                false
            }
            rbPhysicalYes.isChecked && !isRequiredField(etPhysicalAttributes.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_physical))
                false
            }
            else -> true
        }
    }

    private fun storeCountryCode() {
        phoneCountryCodes = ArrayList()
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

    private fun openUploadCountryDialog(phoneCountryCodes: ArrayList<PhoneCountryCode>) {
        val inflater = layoutInflater
        val dialogLayout1 = inflater.inflate(R.layout.search_country, null)
        val mDialog = AlertDialog.Builder(activity)
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
                when (duplicatePhoneCountryCodes[i].code) {
                    "CA" -> tvCountrySelected.text = "+1"
                    else -> tvCountrySelected.text = duplicatePhoneCountryCodes[i].countryCode
                }
                countryCode = duplicatePhoneCountryCodes[i].countryCode
                tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(duplicatePhoneCountryCodes[i].flag, 0, 0, 0)
            }
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
                                        sdv_missing_profile.loadFrescoImageFromFile(
                                            mActivity,
                                            imageFile,
                                            1
                                        )
                                        imageFile =
                                            File(
                                                Comman_Methods.compressImage(
                                                    imageFile?.absolutePath ?: "",
                                                    mActivity
                                                )
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
}