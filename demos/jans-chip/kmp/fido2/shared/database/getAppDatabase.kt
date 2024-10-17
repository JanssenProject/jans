package com.example.fido2.shared.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.fido2.database.AppDatabase

fun getAppDatabase(context: Context): AppDatabase {
    val dbFile = context.getDatabasePath("super_gluu_fido.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext, name = dbFile.absolutePath
    ).setDriver(BundledSQLiteDriver()).fallbackToDestructiveMigration(true).build()
}