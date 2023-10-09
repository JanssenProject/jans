package io.jans.chip.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.jans.chip.modal.OIDCClient;
@Dao
public interface OidcClientDao {
    @Insert
    void insert(OIDCClient oidcClient);

    @Update
    void update(OIDCClient oidcClient);

    @Query("SELECT * FROM OIDC_CLIENT")
    List<OIDCClient> getAll();
    @Query("DELETE FROM OIDC_CLIENT")
    void deleteAll();
}
