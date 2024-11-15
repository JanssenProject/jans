package com.example.fido2.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fido2.model.AppSettings

@Dao
interface ServerUrlDao {

    @Insert
    suspend fun insert(serverUrl: AppSettings)

    @Update
    suspend fun update(serverUrl: AppSettings)

    @Query("UPDATE APP_SETTINGS SET ServerUrl=:serverUrl WHERE ServerUrl = :serverUrl")
    suspend fun update(serverUrl: String)

    @Query("SELECT * FROM APP_SETTINGS")
    suspend fun getAll(): List<AppSettings>

    @Query("DELETE FROM APP_SETTINGS")
    suspend fun deleteAll()
}