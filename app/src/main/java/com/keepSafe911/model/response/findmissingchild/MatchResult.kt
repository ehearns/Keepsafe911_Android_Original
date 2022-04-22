package com.keepSafe911.model.response.findmissingchild

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MatchResult(): Parcelable {
    @SerializedName("id")
    @Expose
    var id: Int? = 0

    @SerializedName("FirstName")
    @Expose
    var firstName: String? = ""

    @SerializedName("LastName")
    @Expose
    var lastName: String? = ""

    @SerializedName("MissingCity")
    @Expose
    var missingCity: String? = ""

    @SerializedName("MissingState")
    @Expose
    var missingState: String? = ""

    @SerializedName("Age")
    @Expose
    var age: Int = 0

    @SerializedName("DateMissing")
    @Expose
    var dateMissing: String? = ""

    @SerializedName("CaseNumber")
    @Expose
    var caseNumber: String? = ""

    @SerializedName("ImageName")
    @Expose
    var imageName: String? = ""

    @SerializedName("ImageUrl")
    @Expose
    var imageUrl: String? = ""

    @SerializedName("matchScore")
    @Expose
    var matchScore: Double? = 0.0

    @SerializedName("Amount")
    @Expose
    var amount: Double? = 0.0

    @SerializedName("PaymentDate")
    @Expose
    var paymentDate: String? = ""

    @SerializedName("AccountNumber")
    @Expose
    var accountNumber: String? = ""

    @SerializedName("LastSeenSituation")
    @Expose
    var lastSeenSituation: String? = ""

    @SerializedName("ContactNumber")
    @Expose
    var contactNumber: String? = ""

    @SerializedName("HairColor")
    @Expose
    var hairColor: String? = ""

    @SerializedName("EyeColor")
    @Expose
    var eyeColor: String? = ""

    @SerializedName("Height")
    @Expose
    var height: Double? = 0.0

    @SerializedName("Weight")
    @Expose
    var weight: Double? = 0.0

    @SerializedName("Complexion")
    @Expose
    var complexion: String? = ""

    @SerializedName("IsWearLenses")
    @Expose
    var isWearLenses: Boolean? = false

    @SerializedName("IsbracesOnTeeth")
    @Expose
    var isBracesOnTeeth: Boolean? = false

    @SerializedName("IsPhysicalAttributes")
    @Expose
    var isPhysicalAttributes: Boolean? = false

    @SerializedName("PhysicalAttributes")
    @Expose
    var physicalAttributes: String? = ""

    @SerializedName("UserId")
    @Expose
    var userId: Int? = 0

    @SerializedName("AllTaskCompleted")
    @Expose
    var allTaskCompleted: Boolean? = false

    @SerializedName("lstChildTaskResponse")
    @Expose
    var lstChildTaskResponse: ArrayList<MissingChildTaskModel>? = ArrayList()

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        firstName = parcel.readString()
        lastName = parcel.readString()
        missingCity = parcel.readString()
        missingState = parcel.readString()
        age = parcel.readInt()
        dateMissing = parcel.readString()
        caseNumber = parcel.readString()
        imageName = parcel.readString()
        imageUrl = parcel.readString()
        matchScore = parcel.readValue(Double::class.java.classLoader) as? Double
        amount = parcel.readValue(Double::class.java.classLoader) as? Double
        paymentDate = parcel.readString()
        accountNumber = parcel.readString()
        lastSeenSituation = parcel.readString()
        contactNumber = parcel.readString()
        hairColor = parcel.readString()
        eyeColor = parcel.readString()
        height = parcel.readValue(Double::class.java.classLoader) as? Double
        weight = parcel.readValue(Double::class.java.classLoader) as? Double
        complexion = parcel.readString()
        isWearLenses = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        isBracesOnTeeth = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        isPhysicalAttributes = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        physicalAttributes = parcel.readString()
        userId = parcel.readValue(Int::class.java.classLoader) as? Int
        allTaskCompleted = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        lstChildTaskResponse = parcel.createTypedArrayList(MissingChildTaskModel.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(firstName)
        parcel.writeString(lastName)
        parcel.writeString(missingCity)
        parcel.writeString(missingState)
        parcel.writeInt(age)
        parcel.writeString(dateMissing)
        parcel.writeString(caseNumber)
        parcel.writeString(imageName)
        parcel.writeString(imageUrl)
        parcel.writeValue(matchScore)
        parcel.writeValue(amount)
        parcel.writeString(paymentDate)
        parcel.writeString(accountNumber)
        parcel.writeString(lastSeenSituation)
        parcel.writeString(contactNumber)
        parcel.writeString(hairColor)
        parcel.writeString(eyeColor)
        parcel.writeValue(height)
        parcel.writeValue(weight)
        parcel.writeString(complexion)
        parcel.writeValue(isWearLenses)
        parcel.writeValue(isBracesOnTeeth)
        parcel.writeValue(isPhysicalAttributes)
        parcel.writeString(physicalAttributes)
        parcel.writeValue(userId)
        parcel.writeValue(allTaskCompleted)
        parcel.writeTypedList(lstChildTaskResponse)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MatchResult> {
        override fun createFromParcel(parcel: Parcel): MatchResult {
            return MatchResult(parcel)
        }

        override fun newArray(size: Int): Array<MatchResult?> {
            return arrayOfNulls(size)
        }
    }
}