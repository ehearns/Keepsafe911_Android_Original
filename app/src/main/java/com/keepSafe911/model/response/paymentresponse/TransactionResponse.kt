package com.keepSafe911.model.response.paymentresponse

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.ArrayList


class TransactionResponse() : Parcelable {

    @SerializedName("responseCode")
    @Expose
    var responseCode: String? = ""
    @SerializedName("authCode")
    @Expose
    var authCode: String? = ""
    @SerializedName("avsResultCode")
    @Expose
    var avsResultCode: String? = ""
    @SerializedName("cvvResultCode")
    @Expose
    var cvvResultCode: String? = ""
    @SerializedName("cavvResultCode")
    @Expose
    var cavvResultCode: String? = ""
    @SerializedName("transId")
    @Expose
    var transId: String? = ""
    @SerializedName("refTransID")
    @Expose
    var refTransID: String? = ""
    @SerializedName("transHash")
    @Expose
    var transHash: String? = ""
    @SerializedName("testRequest")
    @Expose
    var testRequest: String? = ""
    @SerializedName("accountNumber")
    @Expose
    var accountNumber: String? = ""
    @SerializedName("accountType")
    @Expose
    var accountType: String? = ""
    @SerializedName("messages")
    @Expose
    var messages: List<Message_>? = ArrayList()
    @SerializedName("transHashSha2")
    @Expose
    var transHashSha2: String? = ""
    @SerializedName("SupplementalDataQualificationIndicator")
    @Expose
    var supplementalDataQualificationIndicator: Int? = 0


    constructor(parcel: Parcel) : this() {
        cvvResultCode = parcel.readString()
        supplementalDataQualificationIndicator = parcel.readInt()
        transHashSha2 = parcel.readString()
        authCode = parcel.readString()
        cavvResultCode = parcel.readString()
        transId = parcel.readString()
        transHash = parcel.readString()
        accountType = parcel.readString()
        accountNumber = parcel.readString()
        responseCode = parcel.readString()
        avsResultCode = parcel.readString()
        testRequest = parcel.readString()
        refTransID = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(cvvResultCode)
        parcel.writeInt(supplementalDataQualificationIndicator ?: 0)
        parcel.writeString(transHashSha2)
        parcel.writeString(authCode)
        parcel.writeString(cavvResultCode)
        parcel.writeString(transId)
        parcel.writeString(transHash)
        parcel.writeString(accountType)
        parcel.writeString(accountNumber)
        parcel.writeString(responseCode)
        parcel.writeString(avsResultCode)
        parcel.writeString(testRequest)
        parcel.writeString(refTransID)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TransactionResponse> {
        override fun createFromParcel(parcel: Parcel): TransactionResponse {
            return TransactionResponse(parcel)
        }

        override fun newArray(size: Int): Array<TransactionResponse?> {
            return arrayOfNulls(size)
        }
    }

}