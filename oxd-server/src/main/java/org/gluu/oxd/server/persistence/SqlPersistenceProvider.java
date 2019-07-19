package org.gluu.oxd.server.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/04/2017
 */

public interface SqlPersistenceProvider {

    void onCreate();

    void onDestroy();

    Connection getConnection() throws SQLException;
}
