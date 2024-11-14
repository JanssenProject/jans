package com.example.fido2.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fido2.database.dao.AppIntegrityDao
import com.example.fido2.database.dao.FidoConfigurationDao
import com.example.fido2.database.dao.OPConfigurationDao
import com.example.fido2.database.dao.OidcClientDao
import com.example.fido2.database.dao.ServerUrlDao
import com.example.fido2.model.*
import com.example.fido2.model.appIntegrity.AppIntegrityEntity
import com.example.fido2.model.fido.config.FidoConfiguration
import com.example.fido2.utils.AppConfig

@Database(
  entities = [OIDCClient::class, OPConfiguration::class, FidoConfiguration::class, AppIntegrityEntity::class, AppSettings::class],
  version = AppConfig.ROOM_DATABASE_VERSION
)
abstract class AppDatabase : RoomDatabase(), DB {
  abstract fun oidcClientDao(): OidcClientDao
  abstract fun opConfigurationDao(): OPConfigurationDao
  abstract fun appIntegrityDao(): AppIntegrityDao
  abstract fun fidoConfigurationDao(): FidoConfigurationDao
  abstract fun serverUrlDao(): ServerUrlDao

  override fun clearAllTables() {
    super.clearAllTables()
  }
}

// FIXME: Added a hack to resolve below issue:
// Class 'AppDatabase_Impl' is not abstract and does not implement abstract base class member 'clearAllTables'.
interface DB {
  fun clearAllTables() {}
}
