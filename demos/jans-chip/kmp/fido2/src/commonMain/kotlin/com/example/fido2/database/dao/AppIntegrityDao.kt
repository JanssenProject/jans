package com.example.fido2.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fido2.model.appIntegrity.AppIntegrityEntity

@Dao
interface AppIntegrityDao {
    @Insert
    suspend fun insert(appIntegrityEntity: AppIntegrityEntity)

    @Update
    suspend fun update(appIntegrityEntity: AppIntegrityEntity)

    @Query("SELECT * FROM APP_INTEGRITY")
    suspend fun getAll(): List<AppIntegrityEntity>

    @Query("DELETE FROM APP_INTEGRITY")
    suspend fun deleteAll()
}