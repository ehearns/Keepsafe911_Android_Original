package com.keepSafe911.room.convertor

import androidx.room.TypeConverter
import com.keepSafe911.model.FamilyMonitorResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class MemberConverter {
    @TypeConverter
    fun fromString(value: String): ArrayList<FamilyMonitorResult>? {
        val listType = object : TypeToken<ArrayList<FamilyMonitorResult>>() {

        }.type
        return Gson().fromJson<ArrayList<FamilyMonitorResult>>(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<FamilyMonitorResult>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}