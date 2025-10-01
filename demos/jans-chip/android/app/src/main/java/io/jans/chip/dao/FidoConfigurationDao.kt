package io.jans.chip.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.jans.chip.model.fido.config.FidoConfiguration

@Dao
interface FidoConfigurationDao {
    @Insert
    fun insert(opConfiguration: FidoConfiguration)

    @Update
    fun update(opConfiguration: FidoConfiguration)

    @Query("SELECT * FROM FIDO_CONFIGURATION")
    fun getAll(): List<FidoConfiguration>

    @Query("DELETE FROM FIDO_CONFIGURATION")
    fun deleteAll()
}