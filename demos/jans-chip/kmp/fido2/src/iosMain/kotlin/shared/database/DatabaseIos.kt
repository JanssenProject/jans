package shared.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.fido2.database.AppDatabase
import com.example.fido2.database.instantiateImpl
import platform.Foundation.NSHomeDirectory

fun getAppDatabase(): AppDatabase {
    val dbFile = NSHomeDirectory() + "/super_gluu_fido.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile,
        factory = { AppDatabase::class.instantiateImpl() }
    ).setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}
