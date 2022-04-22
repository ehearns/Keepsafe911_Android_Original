package com.keepSafe911.model.response.findmissingchild

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MissingChildTaskModel(): Parcelable {

    @SerializedName("Id")
    @Expose
    var id: Int? = 0

    @SerializedName("TaskId")
    @Expose
    var taskId: Int? = 0

    @SerializedName("Question")
    @Expose
    var question: String? = ""

    @SerializedName("Type")
    @Expose
    var type: Int? = 0

    @SerializedName("Hint")
    @Expose
    var hint: String? = ""

    @SerializedName("AnswerInText")
    @Expose
    var answerInText: String? = ""

    @SerializedName("AnswerInBoolean")
    @Expose
    var answerInBoolean: Boolean? = false

    @SerializedName("AssignBy")
    @Expose
    var assignBy: Int? = 0

    @SerializedName("AssignByName")
    @Expose
    var assignByName: String? = ""

    @SerializedName("AssignByProfileUrl")
    @Expose
    var assignByProfileUrl: String? = ""

    @SerializedName("AssignTo")
    @Expose
    var assignTo: Int? = 0

    @SerializedName("AssignToName")
    @Expose
    var assignToName: String? = ""

    @SerializedName("AssignToProfileUrl")
    @Expose
    var assignToProfileUrl: String? = ""

    @SerializedName("ChildReferenceId")
    @Expose
    var childReferenceId: Int? = 0

    @SerializedName("ChildName")
    @Expose
    var childName: String? = ""

    @SerializedName("Status")
    @Expose
    var status: Int? = 0

    @SerializedName("CreatedOn")
    @Expose
    var createdOn: String? = ""

    var isShared: Boolean = false
    var isOpened: Boolean = false
    var isAssignTask: Boolean = false
    var ownDisabled: Boolean = false
    var reAssigned: Boolean = false

    var reAssignedTo: Int? = 0
    var reAssignName: String? = ""
    var reAssignToProfileUrl: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        taskId = parcel.readValue(Int::class.java.classLoader) as? Int
        question = parcel.readString()
        type = parcel.readValue(Int::class.java.classLoader) as? Int
        hint = parcel.readString()
        answerInText = parcel.readString()
        answerInBoolean = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        assignBy = parcel.readValue(Int::class.java.classLoader) as? Int
        assignByName = parcel.readString()
        assignByProfileUrl = parcel.readString()
        assignTo = parcel.readValue(Int::class.java.classLoader) as? Int
        assignToName = parcel.readString()
        assignToProfileUrl = parcel.readString()
        childReferenceId = parcel.readValue(Int::class.java.classLoader) as? Int
        childName = parcel.readString()
        status = parcel.readValue(Int::class.java.classLoader) as? Int
        createdOn = parcel.readString()
        isShared = parcel.readByte() != 0.toByte()
        isOpened = parcel.readByte() != 0.toByte()
        isAssignTask = parcel.readByte() != 0.toByte()
        ownDisabled = parcel.readByte() != 0.toByte()
        reAssigned = parcel.readByte() != 0.toByte()
        reAssignedTo = parcel.readValue(Int::class.java.classLoader) as? Int
        reAssignName = parcel.readString()
        reAssignToProfileUrl = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(taskId)
        parcel.writeString(question)
        parcel.writeValue(type)
        parcel.writeString(hint)
        parcel.writeString(answerInText)
        parcel.writeValue(answerInBoolean)
        parcel.writeValue(assignBy)
        parcel.writeString(assignByName)
        parcel.writeString(assignByProfileUrl)
        parcel.writeValue(assignTo)
        parcel.writeString(assignToName)
        parcel.writeString(assignToProfileUrl)
        parcel.writeValue(childReferenceId)
        parcel.writeString(childName)
        parcel.writeValue(status)
        parcel.writeString(createdOn)
        parcel.writeByte(if (isShared) 1 else 0)
        parcel.writeByte(if (isOpened) 1 else 0)
        parcel.writeByte(if (isAssignTask) 1 else 0)
        parcel.writeByte(if (ownDisabled) 1 else 0)
        parcel.writeByte(if (reAssigned) 1 else 0)
        parcel.writeValue(reAssignedTo)
        parcel.writeString(reAssignName)
        parcel.writeString(reAssignToProfileUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MissingChildTaskModel> {
        override fun createFromParcel(parcel: Parcel): MissingChildTaskModel {
            return MissingChildTaskModel(parcel)
        }

        override fun newArray(size: Int): Array<MissingChildTaskModel?> {
            return arrayOfNulls(size)
        }
    }
}