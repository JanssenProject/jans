package io.jans.ca.server.persistence.service;

import com.google.common.base.Strings;
import io.jans.ca.common.ExpiredObject;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.Jackson2;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.persistence.providers.SqlPersistenceProvider;
import io.jans.ca.server.service.MigrationService;
import org.h2.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yuriyz
 */
public class SqlPersistenceServiceImpl implements PersistenceService {

    private static final Logger LOG = LoggerFactory.getLogger(SqlPersistenceServiceImpl.class);

    private SqlPersistenceProvider provider;

    public SqlPersistenceServiceImpl(SqlPersistenceProvider provider) {
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

            Statement stmt = conn.createStatement();

            stmt.addBatch("create table if not exists rp(id varchar(36) primary key, data varchar(50000))");
            stmt.addBatch("create table if not exists expired_objects( obj_key varchar(50), obj_value varchar(50000), type varchar(20), iat TIMESTAMP NULL DEFAULT NULL, exp TIMESTAMP NULL DEFAULT NULL)");

            stmt.executeBatch();

            stmt.close();
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

    public boolean createExpiredObject(ExpiredObject obj) {
        Connection conn = null;
        PreparedStatement query = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);
            query = conn.prepareStatement("insert into expired_objects(obj_key, obj_value, type, iat, exp) values(?, ?, ?, ?, ?)");
            query.setString(1, obj.getKey().trim());
            query.setString(2, obj.getValue().trim());
            query.setString(3, obj.getType().getValue());
            query.setTimestamp(4, new Timestamp(obj.getIat().getTime()));
            query.setTimestamp(5, new Timestamp(obj.getExp().getTime()));
            query.executeUpdate();
            query.close();

            conn.commit();
            LOG.debug("Expired_object created successfully. Object : " + obj.getKey());

            return true;
        } catch (Exception e) {
            LOG.error("Failed to create Expired_object: " + obj.getKey(), e);
            rollbackSilently(conn);
            return false;
        } finally {
            IOUtils.closeSilently(query);
            IOUtils.closeSilently(conn);
        }
    }

    public boolean create(Rp rp) {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);
            PreparedStatement query = conn.prepareStatement("insert into rp(id, data) values(?, ?)");
            query.setString(1, rp.getRpId());
            query.setString(2, Jackson2.serializeWithoutNulls(rp));
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
            query.setString(1, Jackson2.serializeWithoutNulls(rp));
            query.setString(2, rp.getRpId());
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

    public Rp getRp(String rpId) {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement query = conn.prepareStatement("select id, data from rp where id = ?");
            query.setString(1, rpId);
            ResultSet rs = query.executeQuery();

            rs.next();
            String data = rs.getString("data");
            query.close();
            conn.commit();

            Rp rp = MigrationService.parseRp(data);
            if (rp != null) {
                LOG.debug("Found RP id: " + rpId + ", RP : " + rp);
                return rp;
            } else {
                LOG.error("Failed to fetch RP by id: " + rpId);
                return null;
            }
        } catch (Exception e) {
            LOG.error("Failed to find RP by id: " + rpId + ". Error: " + e.getMessage(), e);
            rollbackSilently(conn);
            return null;
        } finally {
            IOUtils.closeSilently(conn);
        }
    }

    public ExpiredObject getExpiredObject(String key) {

        Connection conn = null;
        PreparedStatement query = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);
            query = conn.prepareStatement("select obj_key, obj_value, type, iat, exp from expired_objects where obj_key = ?");
            query.setString(1, key.trim());
            ResultSet rs = query.executeQuery();
            ExpiredObject expiredObject = null;

            rs.next();
            if (!Strings.isNullOrEmpty(rs.getString("obj_key"))) {
                expiredObject = new ExpiredObject(rs.getString("obj_key"), rs.getString("obj_value"), ExpiredObjectType.fromValue(rs.getString("type")), new java.util.Date(rs.getTimestamp("iat").getTime()), new java.util.Date(rs.getTimestamp("exp").getTime()));
            }

            query.close();
            conn.commit();

            if (expiredObject != null) {
                LOG.debug("Found ExpiredObject: " + expiredObject.getKey());
                return expiredObject;
            } else {
                LOG.error("ExpiredObject not found: " + key);
                return expiredObject;
            }
        } catch (Exception e) {
            LOG.error("Failed to find ExpiredObject: " + key + ". Error: " + e.getMessage(), e);
            rollbackSilently(conn);
            return null;
        } finally {
            IOUtils.closeSilently(query);
            IOUtils.closeSilently(conn);
        }
    }

    public boolean isExpiredObjectPresent(String key) {
        return getExpiredObject(key) != null;
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

                Rp rp = MigrationService.parseRp(data);
                if (rp != null) {
                    result.add(rp);
                } else {
                    LOG.error("Failed to parse rp, id: " + id);
                }
            }

            query.close();
            conn.commit();
            LOG.info("Loaded " + result.size() + " RPs.");
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

    @Override
    public boolean remove(String rpId) {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement query = conn.prepareStatement("delete from rp where id = ?");
            query.setString(1, rpId);
            query.executeUpdate();
            query.close();

            conn.commit();
            LOG.debug("Removed rp successfully. rpId: " + rpId);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove rp with rpId: " + rpId, e);
            rollbackSilently(conn);
            return false;
        } finally {
            IOUtils.closeSilently(conn);
        }
    }

    public boolean deleteExpiredObjectsByKey(String key) {
        Connection conn = null;
        PreparedStatement query = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);

            query = conn.prepareStatement("delete from expired_objects where obj_key = ?");
            query.setString(1, key);
            query.executeUpdate();
            query.close();

            conn.commit();
            LOG.debug("Removed expired_objects successfully: " + key);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove expired_objects: " + key, e);
            rollbackSilently(conn);
            return false;
        } finally {
            IOUtils.closeSilently(query);
            IOUtils.closeSilently(conn);
        }
    }

    public boolean deleteAllExpiredObjects() {
        Connection conn = null;
        try {
            conn = provider.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement query = conn.prepareStatement("delete from expired_objects where exp < CURRENT_TIMESTAMP()");
            query.executeUpdate();
            query.close();

            conn.commit();
            LOG.debug("Removed expired_objects successfully. ");
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove expired_objects. ", e);
            rollbackSilently(conn);
            return false;
        } finally {
            IOUtils.closeSilently(conn);
        }
    }
}
