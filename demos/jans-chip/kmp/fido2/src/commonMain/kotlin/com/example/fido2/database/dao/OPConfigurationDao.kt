package com.example.fido2.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fido2.model.OPConfiguration

@Dao
interface OPConfigurationDao {
    @Insert
    suspend fun insert(opConfiguration: OPConfiguration)

    @Update
    suspend fun update(opConfiguration: OPConfiguration)

    @Query("SELECT * FROM OP_CONFIGURATION")
    suspend fun getAll(): List<OPConfiguration>

    @Query("DELETE FROM OP_CONFIGURATION")
    suspend fun deleteAll()
}
