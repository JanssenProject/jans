package org.xdi.oxd.server.service;

import com.google.inject.Inject;
import org.h2.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.rs.protect.Jackson;
import org.xdi.oxd.server.persistence.PersistenceProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/04/2017
 */

public class PersistenceService {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceService.class);

    private PersistenceProvider provider;

    @Inject
    public PersistenceService(PersistenceProvider provider) {
        this.provider = provider;
    }

    public void create() {
        provider.onCreate();

        createSchema();
    }

    private void createSchema() {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement createRpTable = conn.prepareStatement(
                    "create table if not exists rp(id varchar(36) primary key, data varchar(65534))");
            createRpTable.executeUpdate();
            createRpTable.close();

            conn.commit();
            LOG.debug("Schema created successfully.");
        } catch (Exception e) {
            LOG.error("Failed to create schema. Error: " + e.getMessage(), e);
            rollbackSilently(conn);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeSilently(conn);
        }
    }

    public boolean create(Rp rp) {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);
            PreparedStatement query = conn.prepareStatement("insert into rp(id, data) values(?, ?)");
            query.setString(1, rp.getOxdId());
            query.setString(2, Jackson.asJson(rp));
            query.executeUpdate();
            query.close();

            conn.commit();
            LOG.debug("RP created successfully. RP : " + rp);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to create RP: " + rp, e);
            rollbackSilently(conn);
            return false;
        } finally {
            IOUtils.closeSilently(conn);
        }
    }

    public boolean update(Rp rp) {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);
            PreparedStatement query = conn.prepareStatement("update rp set data = ? where id = ?");
            query.setString(1, Jackson.asJson(rp));
            query.setString(2, rp.getOxdId());
            query.executeUpdate();
            query.close();

            conn.commit();
            LOG.debug("RP updated successfully. RP : " + rp);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to update RP: " + rp, e);
            rollbackSilently(conn);
            return false;
        } finally {
            IOUtils.closeSilently(conn);
        }
    }

    public Rp getRp(String oxdId) {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement query = conn.prepareStatement("select id, data from rp where id = ?");
            query.setString(1, oxdId);
            ResultSet rs = query.executeQuery();

            rs.next();
            String data = rs.getString("data");
            query.close();
            conn.commit();

            Rp rp = SiteConfigurationService.parseRp(data);
            if (rp != null) {
                LOG.debug("Found RP id: " + oxdId + ", RP : " + rp);
                return rp;
            } else {
                LOG.error("Failed to fetch RP by id: " + oxdId);
                throw new RuntimeException("No RP with id: " + oxdId);
            }
        } catch (Exception e) {
            LOG.error("Failed to find RP by id: " + oxdId + ". Error: " + e.getMessage(), e);
            rollbackSilently(conn);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeSilently(conn);
        }
    }

    public boolean removeAllRps() {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);
            PreparedStatement query = conn.prepareStatement("delete from rp");
            query.executeUpdate();
            query.close();

            conn.commit();
            LOG.debug("All RPs are removed successfully.");
            return true;
        } catch (Exception e) {
            LOG.error("Failed to drop all RPs", e);
            rollbackSilently(conn);
            return false;
        } finally {
            IOUtils.closeSilently(conn);
        }
    }

    public Set<Rp> getRps() {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement query = conn.prepareStatement("select id, data from rp");
            ResultSet rs = query.executeQuery();

            Set<Rp> result = new HashSet<>();
            while (rs.next()) {
                String id = rs.getString("id");
                String data = rs.getString("data");

                Rp rp = SiteConfigurationService.parseRp(data);
                if (rp != null) {
                    result.add(rp);
                } else {
                    LOG.error("Failed to parse rp, id: " + id);
                }
            }

            query.close();
            conn.commit();
            LOG.error("Loaded " + result.size() + " RPs.");
            return result;
        } catch (Exception e) {
            LOG.error("Failed to fetch rps. Error: " + e.getMessage(), e);
            rollbackSilently(conn);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeSilently(conn);
        }
    }

    public static void rollbackSilently(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOG.error("Failed to rollback transaction, error: " + e.getMessage(), e);
        }
    }

    public void destroy() {
        provider.onDestroy();
    }
}
