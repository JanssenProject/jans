package io.jans.chip

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import io.jans.chip.model.OPConfiguration
import io.jans.chip.utils.AppConfig
import io.jans.chip.dao.AppIntegrityDao
import io.jans.chip.dao.FidoConfigurationDao
import io.jans.chip.dao.OPConfigurationDao
import io.jans.chip.dao.OidcClientDao
import io.jans.chip.model.OIDCClient
import io.jans.chip.model.appIntegrity.AppIntegrityEntity
import io.jans.chip.model.fido.config.FidoConfiguration


@Database(
    entities = [OIDCClient::class, OPConfiguration::class, AppIntegrityEntity::class, FidoConfiguration::class],
    version = AppConfig.ROOM_DATABASE_VERSION
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun oidcClientDao(): OidcClientDao
    abstract fun opConfigurationDao(): OPConfigurationDao
    abstract fun appIntegrityDao(): AppIntegrityDao
    abstract fun fidoConfigurationDao(): FidoConfigurationDao

    companion object {
        private val LOG_TAG = AppDatabase::class.java.simpleName
        private val LOCK = Any()
        private val DATABASE_NAME: String = AppConfig.SQLITE_DB_NAME
        private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                synchronized(LOCK) {
                    Log.d(
                        LOG_TAG,
                        "Creating new database instance"
                    )
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java, DATABASE_NAME
                    )
                        .allowMainThreadQueries()
                        .fallbackToDestructiveMigration()
                        .build()
                }
                Log.d(LOG_TAG, "Getting the database instance")
            }
            return instance!!
        }
    }

}
