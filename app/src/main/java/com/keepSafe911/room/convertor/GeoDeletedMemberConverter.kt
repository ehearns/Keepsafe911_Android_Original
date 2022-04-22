package com.keepSafe911.room.convertor

import androidx.room.TypeConverter
import com.keepSafe911.model.LstDeleteGeoFence
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class GeoDeletedMemberConverter {
    @TypeConverter
    fun fromString(value: String): ArrayList<LstDeleteGeoFence>? {
        val listType = object : TypeToken<ArrayList<LstDeleteGeoFence>>() {

        }.type
        return Gson().fromJson<ArrayList<LstDeleteGeoFence>>(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<LstDeleteGeoFence>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}