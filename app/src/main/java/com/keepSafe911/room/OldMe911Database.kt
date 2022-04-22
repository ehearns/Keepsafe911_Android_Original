package com.keepSafe911.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.roomobj.FingerSucess_User_Object
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.model.roomobj.Remember_User_Object
import com.keepSafe911.room.convertor.*
import com.keepSafe911.room.dao.*
import com.keepSafe911.model.*
import com.keepSafe911.model.response.voicerecognition.ManageVoiceRecognitionModel
import com.keepSafe911.room.databasetable.GeoFenceNotification

@Database(
    entities = [LoginObject::class,
        LoginRequest::class,
        Remember_User_Object::class,
        GeoFenceResult::class,
        FamilyMonitorResult::class,
        LstDeleteGeoFence::class,
        LstGeoFenceMember::class,
        GeoFenceNotification::class,
        FingerSucess_User_Object::class,
        PlacesToVisitModel::class,
        ManageVoiceRecognitionModel::class],
    version = 5, exportSchema = false
)
@TypeConverters(
    LoginConvertor::class,
    GeoFenceConvertor::class,
    MemberConverter::class,
    GeoFenceMemberConverter::class,
    GeoDeletedMemberConverter::class,
    PlacesToVisitConvertor::class
)

abstract class OldMe911Database : RoomDatabase() {

    abstract fun loginDao(): LoginDao
    abstract fun loginRequestDao(): LoginEntryDao
    abstract fun geoFenceDao(): GeoFenceDao
    abstract fun memberDao(): MemberDao
    abstract fun placeToVisitDao(): PlacesToVisitDao

    companion object {
        @Volatile
        var instance: OldMe911Database? = null

        fun getDatabase(context: Context): OldMe911Database {
            if (instance == null) {
                instance = Room.databaseBuilder(context.applicationContext, OldMe911Database::class.java, "KeepSafe911")
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
            }
            return instance!!
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loginTable ADD COLUMN IsChildMissing INTEGER")
                database.execSQL("ALTER TABLE loginTable ADD COLUMN ClientMobileNumber TEXT")
                database.execSQL("ALTER TABLE loginTable ADD COLUMN ClientImageUrl TEXT")

                database.execSQL("ALTER TABLE memberTable ADD COLUMN IsChildMissing INTEGER")
            }
        }

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loginTable ADD COLUMN CurrentSubscriptionEndDate TEXT")
                database.execSQL("ALTER TABLE loginTable ADD COLUMN LiveStreamDuration INTEGER")
            }
        }

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loginTable ADD COLUMN AdminName TEXT")
            }
        }

        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loginTable ADD COLUMN IsAdminLoggedIn INTEGER")
            }
        }

        fun destroyInstance() {
            instance = null
        }
    }
}