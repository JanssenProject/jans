package org.gluu.oxd.server.persistence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gluu.conf.service.ConfigurationFactory;
import org.gluu.oxd.common.ExpiredObject;
import org.gluu.oxd.common.ExpiredObjectType;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.persistence.configuration.GluuConfiguration;
import org.gluu.oxd.server.persistence.configuration.OxdConfigurationFactory;
import org.gluu.oxd.server.persistence.modal.RpObject;
import org.gluu.oxd.server.service.MigrationService;
import org.gluu.oxd.server.service.Rp;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.persist.model.base.Entry;
import org.gluu.persist.model.base.GluuDummyEntry;
import org.gluu.util.security.StringEncrypter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GluuPersistenceService implements PersistenceService {

    private static final Logger LOG = LoggerFactory.getLogger(GluuPersistenceService.class);
    private OxdServerConfiguration configuration;
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    public GluuPersistenceService(OxdServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void create() {
        LOG.debug("Creating GluuPersistenceService ...");
        try {
            System.setProperty("gluu.base", asGluuConfiguration(this.configuration).getLocation());
            ConfigurationFactory configurationFactory = OxdConfigurationFactory.instance();
            //StringEncrypter stringEncrypter = configurationFactory.getStringEncrypter();

            this.persistenceEntryManager = configurationFactory.getPersistenceEntryManager();

            Entry base = (Entry) this.persistenceEntryManager.find(GluuDummyEntry.class, getBaseDn());
            Preconditions.checkNotNull(base);
        } catch (Exception e) {
            throw new IllegalStateException("Error starting GluuPersistenceService", e);
        }
    }

    public boolean create(Rp rp) {
        try {
            RpObject rpObj = new RpObject(getDnForRp(rp.getOxdId()), rp.getOxdId(), Jackson2.serializeWithoutNulls(rp));
            this.persistenceEntryManager.persist(rpObj);
            LOG.debug("RP created successfully. RP : {} ", rp);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to create RP: {} ", rp, e);
        }
        return false;
    }

    public boolean createExpiredObject(ExpiredObject obj) {
        try {
            if (isExpiredObjectPresent(obj.getKey())) {
                LOG.warn("Expired_object already present. Object : {} ", obj.getKey());
                return true;
            }
            obj.setTypeString(obj.getType().getValue());
            obj.setDn(getDnForExpiredObj(obj.getKey()));
            this.persistenceEntryManager.persist(obj);
            LOG.debug("Expired_object created successfully. Object : {} ", obj.getKey());
            return true;
        } catch (Exception e) {
            LOG.error("Failed to create ExpiredObject: {} ", obj.getKey(), e);
        }
        return false;
    }

    public boolean update(Rp rp) {
        try {
            RpObject rpObj = new RpObject(getDnForRp(rp.getOxdId()), rp.getOxdId(), Jackson2.serializeWithoutNulls(rp));
            this.persistenceEntryManager.merge(rpObj);
            LOG.debug("RP updated successfully. RP : {} ", rpObj);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to update RP: {} ", rp, e);
        }
        return false;
    }

    public Rp getRp(String oxdId) {
        try {
            RpObject rpFromGluuPersistance = getRpObject(oxdId, new String[0]);

            Rp rp = MigrationService.parseRp(rpFromGluuPersistance.getData());
            if (rp != null) {
                LOG.debug("Found RP id: {}, RP : {} ", oxdId, rp);
                return rp;
            }
            LOG.error("Failed to fetch RP by id: {} ", oxdId);
            return null;
        } catch (Exception e) {
            LOG.error("Failed to update oxdId: {} ", oxdId, e);
        }
        return null;
    }

    private RpObject getRpObject(String oxdId, String... returnAttributes) {
        return (RpObject) this.persistenceEntryManager.find(getDnForRp(oxdId), RpObject.class, returnAttributes);
    }

    public ExpiredObject getExpiredObject(String key) {
        try {
            ExpiredObject expiredObject = getExpiredObject(key, null);
            expiredObject.setType(ExpiredObjectType.fromValue(expiredObject.getTypeString()));
            if (expiredObject != null) {
                LOG.debug("Found ExpiredObject id: {} , ExpiredObject : {} ", key, expiredObject);
                return expiredObject;
            }
            LOG.error("Failed to fetch ExpiredObject by id: {} ", key);
            return null;
        } catch (Exception e) {
            if (((e instanceof EntryPersistenceException)) && (e.getMessage().contains("Failed to find entry"))) {
                LOG.warn("Failed to fetch ExpiredObject by id: {} ", key);
                return null;
            }
            LOG.error("Failed to fetch ExpiredObject by id: {} ", key, e);
        }
        return null;
    }

    private ExpiredObject getExpiredObject(String key, String... returnAttributes) {
        return (ExpiredObject) this.persistenceEntryManager.find(getDnForExpiredObj(key), ExpiredObject.class, returnAttributes);
    }

    public boolean isExpiredObjectPresent(String key) {
        return getExpiredObject(key) != null;
    }

    public boolean removeAllRps() {
        List<RpObject> rps = this.persistenceEntryManager.findEntries(String.format("%s,%s", new Object[]{getRpOu(), getBaseDn()}), RpObject.class, null);
        for (RpObject rp : rps) {
            this.persistenceEntryManager.remove(rp);
        }
        LOG.debug("Removed all Rps successfully. ");
        return true;
    }

    public Set<Rp> getRps() {
        try {
            List<RpObject> rpObjects = this.persistenceEntryManager.findEntries(String.format("%s,%s", new Object[]{getRpOu(), getBaseDn()}), RpObject.class, null);

            Set<Rp> result = new HashSet();
            for (RpObject ele : rpObjects) {
                Rp rp = MigrationService.parseRp(ele.getData());
                if (rp != null) {
                    result.add(rp);
                } else {
                    LOG.error("Failed to parse rp, id: {}, dn: {} ", ele.getId(), ele.getDn());
                }
            }
            return result;
        } catch (Exception e) {
            LOG.error("Failed to fetch rps. Error: {} ", e.getMessage(), e);
        }
        return null;
    }

    public void destroy() {
        this.persistenceEntryManager.destroy();
    }

    public boolean remove(String oxdId) {
        try {
            RpObject rpFromPersistance = getRpObject(oxdId, new String[0]);
            this.persistenceEntryManager.remove(rpFromPersistance);
            LOG.debug("Removed rp successfully. oxdId: {} ", oxdId);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove rp with oxdId: {} ", oxdId, e);
        }
        return false;
    }

    public boolean deleteExpiredObjectsByKey(String key) {
        try {
            ExpiredObject expiredObject = getExpiredObject(key);
            this.persistenceEntryManager.remove(expiredObject);
            LOG.debug("Removed expired_objects successfully: {} ", key);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove expired_objects: {} ", key, e);
        }
        return false;
    }

    public boolean deleteAllExpiredObjects() {
        try {
            List<ExpiredObject> expiredObjects = this.persistenceEntryManager.findEntries(String.format("%s,%s", new Object[]{getExpiredObjOu(), getBaseDn()}), ExpiredObject.class, null);
            for (ExpiredObject ele : expiredObjects) {
                this.persistenceEntryManager.remove(ele);
            }
            LOG.debug("Removed all expired_objects successfully. ");
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove expired_objects. ", e);
        }
        return false;
    }

    public String getDnForRp(String oxdId) {
        return String.format("id=%s,%s,%s", new Object[]{oxdId, getRpOu(), getBaseDn()});
    }

    public String getDnForExpiredObj(String oxdId) {
        return String.format("key=%s,%s,%s", new Object[]{oxdId, getExpiredObjOu(), getBaseDn()});
    }

    private String getBaseDn() {
        return "ou=oxd,o=gluu";
    }

    private String getRpOu() {
        return "ou=rp";
    }

    private String getExpiredObjOu() {
        return "ou=expiredObjects";
    }

    public static GluuConfiguration asGluuConfiguration(OxdServerConfiguration configuration) {
        try {
            JsonNode node = configuration.getStorageConfiguration();
            if (node != null) {
                return Jackson2.createJsonMapper().treeToValue(node, GluuConfiguration.class);
            }
        } catch (Exception e) {
            LOG.error("Failed to parse GluuConfiguration.", e);
        }
        return new GluuConfiguration();
    }
}
