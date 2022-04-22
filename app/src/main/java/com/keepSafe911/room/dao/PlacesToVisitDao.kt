package com.keepSafe911.room.dao


import androidx.room.*
import com.keepSafe911.model.PlacesToVisitModel

@Dao
interface PlacesToVisitDao {

    @Query("SELECT * FROM placesToVisit ORDER BY placesId DESC LIMIT 1")
    fun getLastPlace(): PlacesToVisitModel

    @Query("SELECT * from placesToVisit")
    fun getAllPlaceVisit(): List<PlacesToVisitModel>

    @Query("SELECT COUNT(*) from placesToVisit")
    fun countPlaces(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addPlace(loginObject: PlacesToVisitModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllPlace(loginObject: ArrayList<PlacesToVisitModel>)

    @Query("select * from placesToVisit LIMIT 1")
    fun getFirstPlace(): PlacesToVisitModel

    @Update
    fun updatePlace(loginObject: PlacesToVisitModel)

    @Delete
    fun deletePlace(loginObject: PlacesToVisitModel)

    @Query("DELETE from placesToVisit")
    fun dropPlace()

}