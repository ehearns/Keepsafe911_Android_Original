package com.keepSafe911.model.response.paymentresponse

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
class GoogleTransactionData() : Parcelable{
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(responseCode)
        dest?.writeString(authCode)
        dest?.writeString(avsResultCode)
        dest?.writeString(cvvResultCode)
        dest?.writeString(cavvResultCode)
        dest?.writeString(transId)
        dest?.writeString(refTransID)
        dest?.writeString(transHash)
        dest?.writeString(accountNumber)
        dest?.writeString(accountType)
        dest?.writeTypedList(messages)
        dest?.writeTypedList(userFields)
        dest?.writeString(transHashSha2)
    }

    override fun describeContents(): Int {
        return 0
    }

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
    @SerializedName("accountNumber")
    @Expose
    var accountNumber: String? = ""
    @SerializedName("accountType")
    @Expose
    var accountType: String? = ""
    @SerializedName("messages")
    @Expose
    var messages: ArrayList<Message>? = ArrayList()
    @SerializedName("userFields")
    @Expose
    var userFields: ArrayList<UserField>? = ArrayList()
    @SerializedName("transHashSha2")
    @Expose
    var transHashSha2: String? = ""

    constructor(parcel: Parcel) : this() {
        responseCode = parcel.readString()
        authCode = parcel.readString()
        avsResultCode = parcel.readString()
        cvvResultCode = parcel.readString()
        cavvResultCode = parcel.readString()
        transId = parcel.readString()
        refTransID = parcel.readString()
        transHash = parcel.readString()
        accountNumber = parcel.readString()
        accountType = parcel.readString()
        messages = parcel.createTypedArrayList(Message)
        userFields = parcel.createTypedArrayList(UserField)
        transHashSha2 = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<GoogleTransactionData> {
        override fun createFromParcel(parcel: Parcel): GoogleTransactionData {
            return GoogleTransactionData(parcel)
        }

        override fun newArray(size: Int): Array<GoogleTransactionData?> {
            return arrayOfNulls(size)
        }
    }

}