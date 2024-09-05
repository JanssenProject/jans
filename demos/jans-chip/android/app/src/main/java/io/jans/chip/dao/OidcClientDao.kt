package io.jans.chip.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.jans.chip.model.OIDCClient
@Dao
interface OidcClientDao {
    @Insert
    fun insert(oidcClient: OIDCClient)

    @Update
    fun update(oidcClient: OIDCClient)

    @Query("SELECT * FROM OIDC_CLIENT")
    fun getAll(): List<OIDCClient>

    @Query("DELETE FROM OIDC_CLIENT")
    fun deleteAll()
}