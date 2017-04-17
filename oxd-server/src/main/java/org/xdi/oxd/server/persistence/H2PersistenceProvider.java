package org.xdi.oxd.server.persistence;

import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/04/2017
 */

public class H2PersistenceProvider implements PersistenceProvider {

    private JdbcConnectionPool pool = null;

    @Override
    public void onCreate() {
         pool = JdbcConnectionPool.create("jdbc:h2:./oxd_db", "oxd", "oxd");
    }

    @Override
    public void onDestroy() {
        pool.dispose();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }
}
