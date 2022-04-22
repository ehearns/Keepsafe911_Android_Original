package com.keepSafe911.room.dao

import androidx.room.*
import com.keepSafe911.model.request.LoginRequest

@Dao
interface LoginEntryDao {
    @Query("SELECT * FROM loginEntry ORDER BY loginEntryID DESC LIMIT 1")
    fun getOneRequest(): LoginRequest

    @Query("SELECT * from loginEntry")
    fun getAllLoginRequestDetail(): List<LoginRequest>

    @Query("SELECT COUNT(*) from loginEntry")
    fun countUser(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addLoginRequest(loginObject: LoginRequest)

    @Query("SELECT * from loginEntry where recordStatus = :recordStatus")
    fun getPingEntry(recordStatus: Int): List<LoginRequest>

    @Query("SELECT * from loginEntry where id = :memberID")
    fun getSingleData(memberID: Int): LoginRequest

    @Query("select * from loginEntry LIMIT 1")
    fun getFirstEntry(): LoginRequest

    @Update
    fun updateLogin(loginObject: LoginRequest)

    @Delete
    fun deleteLogin(loginObject: LoginRequest)

    @Query("DELETE FROM loginEntry")
    fun dropTable()
}