package com.keepSafe911.fragments.homefragment.profile

import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.response.CommonValidationResponse
import com.keepSafe911.model.response.voicerecognition.ManageVoiceRecognitionModel
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_voice_recognition.*
import kotlinx.android.synthetic.main.raw_voice_recognition.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class VoiceRecognitionFragment : HomeBaseFragment(), View.OnClickListener {

    private var param1: String? = ""
    private var param2: String? = ""
    lateinit var appDatabase: OldMe911Database
    lateinit var voiceDialog: Dialog
    lateinit var recognitionAdapter: VoiceRecognitionAdapter
    var voiceRecognitionList: ArrayList<ManageVoiceRecognitionModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1, "")
            param2 = it.getString(ARG_PARAM2, "")
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
        return inflater.inflate(R.layout.fragment_voice_recognition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        ivAddVoiceRecognition.setOnClickListener(this)

        getVoiceRecognitionList()

        val addVoiceParam = ivAddVoiceRecognition.layoutParams as ViewGroup.MarginLayoutParams
        val height = if (Comman_Methods.hasNavBar(mActivity)) {
            if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                Utils.calculateNoOfRows(mActivity, 1.2)
            } else {
                Utils.calculateNoOfRows(mActivity, 1.1)
            }
        } else {
            if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                Utils.calculateNoOfRows(mActivity, 1.25)
            } else {
                Utils.calculateNoOfRows(mActivity, 1.15)
            }
        }
        addVoiceParam.setMargins(0, height, 0, 20)
    }

    private fun getVoiceRecognitionList() {
        mActivity.isSpeedAvailable()

        Utils.voiceRecognitionListing(mActivity, object : CommonApiListener {
            override fun voiceRecognitionResponse(
                status: Boolean,
                voiceList: ArrayList<ManageVoiceRecognitionModel>,
                message: String,
                responseMessage: String
            ) {
                voiceRecognitionList = voiceList
                setVoiceAdapter()
            }
        })
    }

    private fun setVoiceAdapter() {
        if (voiceRecognitionList.size > 0) {
            rvRecognitionList.visibility = View.VISIBLE
            tvRecognitionListNoData.visibility = View.GONE
        } else {
            rvRecognitionList.visibility = View.GONE
            tvRecognitionListNoData.visibility = View.VISIBLE
        }
        recognitionAdapter = VoiceRecognitionAdapter(mActivity, voiceRecognitionList)
        rvRecognitionList.adapter = recognitionAdapter
        recognitionAdapter.notifyDataSetChanged()
    }

    private fun setHeader() {
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.disableDrawer()
        tvHeader.setPadding(0, 0, 50, 0)
        tvHeader.text = mActivity.resources.getString(R.string.str_voice_recognition).uppercase()
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        rvRecognitionList.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
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
        fun newInstance(param1: String, param2: String) =
            VoiceRecognitionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.ivAddVoiceRecognition -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                addUpdatePhraseData(false, ManageVoiceRecognitionModel())
            }
        }
    }

    private fun addUpdatePhraseData(
        isUpdate: Boolean,
        manageVoiceRecognitionModel: ManageVoiceRecognitionModel
    ) {
        val inflater = mActivity.layoutInflater
        val dialogLayout = inflater.inflate(R.layout.popup_voice_recognition, null)

        if (this::voiceDialog.isInitialized) {
            if (voiceDialog.isShowing) {
                voiceDialog.dismiss()
            }
        }

        val tvVoiceTitle: TextView = dialogLayout.findViewById(R.id.tvVoiceTitle)
        val tvVoiceCancel: TextView = dialogLayout.findViewById(R.id.tvVoiceCancel)
        val tvVoiceAddUpdate: TextView = dialogLayout.findViewById(R.id.tvVoiceAddUpdate)
        val etPhrase: EditText = dialogLayout.findViewById(R.id.etPhrase)
        etPhrase.setText(manageVoiceRecognitionModel.voiceText.toString())

        val mDialog = AlertDialog.Builder(mActivity)
        mDialog.setView(dialogLayout)
        voiceDialog = mDialog.create()


        tvVoiceTitle.text = if (isUpdate) mActivity.resources.getString(R.string.str_update_phrase) else mActivity.resources.getString(R.string.str_add_phrase)
        tvVoiceAddUpdate.text = if (isUpdate) mActivity.resources.getString(R.string.str_update) else mActivity.resources.getString(R.string.add)
        tvVoiceCancel.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            voiceDialog.dismiss()
        }
        tvVoiceAddUpdate.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            callManageVoiceRecognitionApi(etPhrase.text.toString().trim(), manageVoiceRecognitionModel.id ?: 0)
            voiceDialog.dismiss()
        }
        voiceDialog.setCancelable(false)
        voiceDialog.show()
    }

    private fun callManageVoiceRecognitionApi(phrase: String, id: Int = 0) {
        val loginUser = appDatabase.loginDao().getAll()
        val userId = loginUser.memberID ?: 0
        val createdOn = Utils.getCurrentTimeStamp() ?: ""
        val jsonObject = JsonObject()
        jsonObject.addProperty("Id", id)
        jsonObject.addProperty("UserId", userId)
        jsonObject.addProperty("VoiceText", phrase)
        jsonObject.addProperty("CreatedOn", createdOn)
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            val managevoiceRecoCall =
                WebApiClient.getInstance(mActivity).webApi_without?.manageVoiceRecognition(
                    jsonObject
                )

            managevoiceRecoCall?.enqueue(object : Callback<CommonValidationResponse> {
                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    if (response.code() == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                val gson: Gson = GsonBuilder().create()
                                val responseTypeToken: TypeToken<ManageVoiceRecognitionModel> =
                                    object :
                                        TypeToken<ManageVoiceRecognitionModel>() {}
                                val responseData: ManageVoiceRecognitionModel? =
                                    gson.fromJson(
                                        gson.toJson(it.result),
                                        responseTypeToken.type
                                    )
                                if (responseData != null) {
                                    if (id > 0) {
                                        for (v in 0 until voiceRecognitionList.size) {
                                            val voiceData = voiceRecognitionList[v]
                                            if (voiceData.id == responseData.id) {
                                                voiceRecognitionList[v] = responseData
                                            }
                                        }
                                    } else {
                                        voiceRecognitionList.add(responseData)
                                    }

                                    if (id > 0) {
                                        appDatabase.loginDao().updatePhrases(responseData)
                                    } else {
                                        appDatabase.loginDao().addPhrases(responseData)
                                    }
                                }
                            }
                            setVoiceAdapter()
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

    private fun callVoiceRecoDeleteApi(voiceModel: ManageVoiceRecognitionModel) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val callDeleteVoiceRecoListResponse =
                WebApiClient.getInstance(mActivity).webApi_without?.deleteVoiceRecognition(
                    voiceModel.id ?: 0
                )

            callDeleteVoiceRecoListResponse?.enqueue(object :
                Callback<CommonValidationResponse> {
                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body().let {
                                if (it?.status == true) {
                                    voiceRecognitionList.remove(voiceModel)
                                    appDatabase.loginDao().deletePhrase(voiceModel)
                                    setVoiceAdapter()
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

    inner class VoiceRecognitionAdapter(
        val activity: HomeActivity,
        val voiceRecoList: ArrayList<ManageVoiceRecognitionModel>
    ) :
        RecyclerView.Adapter<VoiceRecognitionAdapter.VoiceRecognitionHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceRecognitionHolder {
            return VoiceRecognitionHolder(
                LayoutInflater.from(activity).inflate(R.layout.raw_voice_recognition, parent, false)
            )
        }

        override fun onBindViewHolder(holder: VoiceRecognitionHolder, position: Int) {
            holder.tvVoiceText.text = voiceRecoList[position].voiceText ?: ""
            holder.ivDeleteVoice.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                voiceDeleteDialog(voiceRecoList[position])
            }
            holder.ivEditVoice.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                addUpdatePhraseData(true, voiceRecoList[position])
            }
        }

        private fun voiceDeleteDialog(voiceModel: ManageVoiceRecognitionModel) {
            Comman_Methods.isCustomPopUpShow(activity,
                title = activity.resources.getString(R.string.str_delete_phrase),
                message = activity.resources.getString(R.string.wish_phrase),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        callVoiceRecoDeleteApi(voiceModel)
                    }
                })
        }

        override fun getItemCount(): Int {
            return voiceRecoList.size
        }

        inner class VoiceRecognitionHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvVoiceText: TextView = view.tvVoiceText
            var ivEditVoice: ImageView = view.ivEditVoice
            var ivDeleteVoice: ImageView = view.ivDeleteVoice
        }
    }
}