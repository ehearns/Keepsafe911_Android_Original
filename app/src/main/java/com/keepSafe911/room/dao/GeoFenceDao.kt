package com.keepSafe911.room.dao

import androidx.room.*
import com.keepSafe911.model.GeoFenceResult
import com.keepSafe911.room.databasetable.GeoFenceNotification

@Dao
interface GeoFenceDao {

    @Query("SELECT * FROM geoFenceTable ORDER BY geoID DESC LIMIT 1")
    fun getLastGeoFence(): GeoFenceResult

    @Query("SELECT * from geoFenceTable")
    fun getAllGeoFenceDetail(): List<GeoFenceResult>

    @Query("SELECT COUNT(*) from geoFenceTable")
    fun countGeoFence(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addGeoFence(loginObject: GeoFenceResult)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllGeoFence(loginObject: ArrayList<GeoFenceResult>)

    @Query("select * from geoFenceTable LIMIT 1")
    fun getFirstGeoFenceEntry(): GeoFenceResult

    @Update
    fun updateGeoFence(loginObject: GeoFenceResult)

    @Delete
    fun deleteGeoFence(loginObject: GeoFenceResult)

    @Query("DELETE from geoFenceTable")
    fun dropGeoFence()

    @Query("UPDATE geoFenceTable SET ex = :status WHERE geoID = :id")
    fun updateGeoFenceData(status: String, id: Int)


    //Goe-Fence Notification Table
    @Query("SELECT * FROM geoFenceNotification ORDER BY notifyID DESC LIMIT 1")
    fun getLastGeoNotify(): GeoFenceNotification

    @Query("SELECT * from geoFenceNotification")
    fun getAllGeoNotify(): List<GeoFenceNotification>

    @Query("SELECT COUNT(*) from geoFenceNotification")
    fun countGeoNotify(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addGeoNotify(loginObject: GeoFenceNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllGeoNotify(loginObject: ArrayList<GeoFenceNotification>)

    @Query("select * from geoFenceNotification LIMIT 1")
    fun getFirstGeoNotifyEntry(): GeoFenceNotification

    @Update
    fun updateGeoNotify(loginObject: GeoFenceNotification)

    @Delete
    fun deleteGeoNotify(loginObject: GeoFenceNotification)

    @Query("DELETE from geoFenceNotification")
    fun dropGeoNotify()

}