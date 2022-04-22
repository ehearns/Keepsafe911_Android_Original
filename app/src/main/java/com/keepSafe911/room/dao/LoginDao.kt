package com.keepSafe911.room.dao

import androidx.room.*
import com.keepSafe911.model.response.voicerecognition.ManageVoiceRecognitionModel
import com.keepSafe911.model.roomobj.FingerSucess_User_Object
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.model.roomobj.Remember_User_Object

@Dao
interface LoginDao {

    @Query("SELECT * FROM loginTable ORDER BY ID DESC LIMIT 1")
    fun getAll(): LoginObject

    @Query("SELECT * FROM loginTable ORDER BY ID DESC LIMIT 10")
    fun lastSomeRecords(): List<LoginObject>

    @Query("SELECT * from loginTable")
    fun getAllLoginDetail(): List<LoginObject>

    @Query("SELECT COUNT(*) from loginTable")
    fun countUser(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addLogin(loginObject: LoginObject)

    @Query("SELECT * from loginTable where recordStatus = :recordStatus")
    fun getPingEntry(recordStatus: Int): List<LoginObject>

    @Query("SELECT * from loginTable where memberID = :memberID")
    fun getSingleData(memberID: Int): LoginObject

    @Query("select * from loginTable LIMIT 1")
    fun getFirstEntry(): LoginObject

    @Update
    fun updateLogin(loginObject: LoginObject)

    @Delete
    fun deleteLogin(loginObject: LoginObject)

    @Query("DELETE from loginTable")
    fun dropLogin()


    /**
     * Remember_User_Object
     */
    //Set data into remember_User_Object From Here
    @Query("SELECT * FROM remember_User_Object")
    fun getRemember(): Remember_User_Object

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addRemember(rem_obj: Remember_User_Object)

    @Update
    fun updateRemember(rem_obj: Remember_User_Object)

    @Delete
    fun deleteRemember(rem_obj: Remember_User_Object)


    /**
     * FingerSucess_User_Object
     */
    //Set data into remember_User_Object From Here
    @Query("SELECT * FROM finger_User_Object")
    fun getfingerLoginData(): FingerSucess_User_Object

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFingerLogin(fing_obj: FingerSucess_User_Object)

    @Update
    fun updateFingerLogin(fing_obj: FingerSucess_User_Object)

    @Query("DELETE from finger_User_Object")
    fun dropFinger()

    //Pharases queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllPhrases(addPhrase: List<ManageVoiceRecognitionModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addPhrases(addPhrase: ManageVoiceRecognitionModel)

    @Update
    fun updatePhrases(updatePhrase: ManageVoiceRecognitionModel)

    @Query("SELECT * from phrasesTable")
    fun getAllPhrases(): List<ManageVoiceRecognitionModel>

    @Delete
    fun deletePhrase(deletePhrase: ManageVoiceRecognitionModel)

    @Query("DELETE from phrasesTable")
    fun dropPhrases()
}