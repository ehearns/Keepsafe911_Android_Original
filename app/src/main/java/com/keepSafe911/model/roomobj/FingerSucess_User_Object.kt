package com.keepSafe911.model.roomobj

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "finger_User_Object")
class FingerSucess_User_Object {

    @PrimaryKey(autoGenerate = true)
    var user_id: Int = 0

    var user_name: String = ""

    var user_password: String = ""

    var touch_id: Boolean = false
}