package com.keepSafe911.room.convertor

import androidx.room.TypeConverter
import com.keepSafe911.model.PlacesToVisitModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList

class PlacesToVisitConvertor {
    @TypeConverter
    fun fromString(value: String): ArrayList<PlacesToVisitModel>? {
        val listType = object : TypeToken<ArrayList<PlacesToVisitModel>>() {

        }.type
        return Gson().fromJson<ArrayList<PlacesToVisitModel>>(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<PlacesToVisitModel>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}