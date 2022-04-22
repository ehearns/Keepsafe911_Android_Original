package com.keepSafe911.room.convertor

import androidx.room.TypeConverter
import com.keepSafe911.model.GeoFenceResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class GeoFenceConvertor {
    @TypeConverter
    fun fromString(value: String): ArrayList<GeoFenceResult>? {
        val listType = object : TypeToken<ArrayList<GeoFenceResult>>() {

        }.type
        return Gson().fromJson<ArrayList<GeoFenceResult>>(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<GeoFenceResult>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}