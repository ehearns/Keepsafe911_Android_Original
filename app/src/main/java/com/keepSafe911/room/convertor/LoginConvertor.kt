package com.keepSafe911.room.convertor

import androidx.room.TypeConverter
import com.keepSafe911.model.roomobj.LoginObject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class LoginConvertor {

    @TypeConverter
    fun fromString(value: String): ArrayList<LoginObject>? {
        val listType = object : TypeToken<ArrayList<LoginObject>>() {

        }.type
        return Gson().fromJson<ArrayList<LoginObject>>(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<LoginObject>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}