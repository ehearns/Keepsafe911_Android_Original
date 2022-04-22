package com.keepSafe911.room.convertor

import androidx.room.TypeConverter
import com.keepSafe911.model.LstGeoFenceMember
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class GeoFenceMemberConverter {
    @TypeConverter
    fun fromString(value: String): ArrayList<LstGeoFenceMember>? {
        val listType = object : TypeToken<ArrayList<LstGeoFenceMember>>() {

        }.type
        return Gson().fromJson<ArrayList<LstGeoFenceMember>>(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<LstGeoFenceMember>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}