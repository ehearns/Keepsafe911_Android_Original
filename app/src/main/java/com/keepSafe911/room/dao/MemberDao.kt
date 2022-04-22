package com.keepSafe911.room.dao

import androidx.room.*
import com.keepSafe911.model.FamilyMonitorResult

@Dao
interface MemberDao {
    @Query("SELECT * FROM memberTable ORDER BY fmID DESC LIMIT 1")
    fun getLastMember(): FamilyMonitorResult

    @Query("SELECT * from memberTable")
    fun getAllMember(): List<FamilyMonitorResult>


    @Query("SELECT * from memberTable")
    fun getMembers(): FamilyMonitorResult

    @Query("SELECT COUNT(*) from memberTable")
    fun countMember(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addMember(loginObject: FamilyMonitorResult)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllMember(loginObject: ArrayList<FamilyMonitorResult>)

    @Query("SELECT * from memberTable where id = :memberID")
    fun getSingleData(memberID: Int): FamilyMonitorResult

    @Query("select * from memberTable LIMIT 1")
    fun getFirstEntry(): FamilyMonitorResult

    @Update
    fun updateMember(loginObject: FamilyMonitorResult)

    @Delete
    fun deleteMember(loginObject: FamilyMonitorResult)

    @Query("DELETE FROM memberTable")
    fun dropTable()
}