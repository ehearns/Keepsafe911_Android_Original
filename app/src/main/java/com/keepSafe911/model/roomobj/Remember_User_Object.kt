package com.keepSafe911.model.roomobj

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "remember_User_Object")
class Remember_User_Object {

    @PrimaryKey(autoGenerate = true)
    var user_id: Int = 0

    var user_name: String = ""

    var user_password: String = ""
}