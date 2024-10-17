package com.example.fido2.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fido2.model.fido.config.FidoConfiguration

@Dao
interface FidoConfigurationDao {
    @Insert
    suspend fun insert(opConfiguration: FidoConfiguration)

    @Update
    suspend fun update(opConfiguration: FidoConfiguration)

    @Query("SELECT * FROM FIDO_CONFIGURATION")
    suspend fun getAll(): List<FidoConfiguration>

    @Query("DELETE FROM FIDO_CONFIGURATION")
    suspend fun deleteAll()
}