package io.jans.ca.server.persistence.service;

import io.jans.ca.common.ExpiredObject;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.PersistenceConfigKeys;
import io.jans.ca.server.RpServerConfiguration;
import io.jans.ca.server.persistence.modal.OrganizationBranch;
import io.jans.ca.server.persistence.modal.RpObject;
import io.jans.ca.server.persistence.providers.JansPersistenceConfiguration;
import io.jans.ca.server.persistence.providers.PersistenceEntryManagerFactory;
import io.jans.ca.server.service.MigrationService;
import io.jans.ca.server.service.Rp;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JansPersistenceService implements PersistenceService {

    private static final Logger LOG = LoggerFactory.getLogger(JansPersistenceService.class);
    private RpServerConfiguration configuration;
    private PersistenceEntryManager persistenceEntryManager;
    private String persistenceType;
    private String baseDn;

    public JansPersistenceService(RpServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public JansPersistenceService(RpServerConfiguration configuration, String persistenceType) {
        this.configuration = configuration;
        this.persistenceType = persistenceType;
    }

    public void create() {
        LOG.debug("Creating JansPersistenceService ...");
        try {
            JansPersistenceConfiguration jansPersistenceConfiguration = new JansPersistenceConfiguration(configuration);
            Properties props = jansPersistenceConfiguration.getPersistenceProps();
            this.baseDn = props.getProperty(PersistenceConfigKeys.BaseDn.getKeyName());
            if (props.getProperty(PersistenceConfigKeys.PersistenceType.getKeyName()).equalsIgnoreCase("ldap")
                    || props.getProperty(PersistenceConfigKeys.PersistenceType.getKeyName()).equalsIgnoreCase("hybrid")) {

                this.persistenceEntryManager = PersistenceEntryManagerFactory.createLdapPersistenceEntryManager(props);

            } else if (props.getProperty(PersistenceConfigKeys.PersistenceType.getKeyName()).equalsIgnoreCase("couchbase")) {

                this.persistenceEntryManager = PersistenceEntryManagerFactory.createCouchbasePersistenceEntryManager(props);
            }

            if (this.persistenceType != null && !this.persistenceType.equalsIgnoreCase(props.getProperty(PersistenceConfigKeys.PersistenceType.getKeyName()))) {
                LOG.error("The value of the `storage` field in `client-api-server.yml` does not matches with `persistence.type` in `gluu.property` file. \n `storage` value: {} \n `persistence.type` value : {}"
                        , this.persistenceType, this.persistenceEntryManager.getPersistenceType());
                throw new RuntimeException("The value of the `storage` field in `client-api-server.yml` does not matches with `persistence.type` in `gluu.property` file. \n `storage` value: " + this.persistenceType + " \n `persistence.type` value : "
                        + this.persistenceEntryManager.getPersistenceType());
            }
            prepareBranch();
        } catch (Exception e) {
            throw new IllegalStateException("Error starting GluuPersistenceService", e);
        }
    }

    public void prepareBranch() {
        if (!this.persistenceEntryManager.hasBranchesSupport(this.baseDn)) {
            return;
        }
        //create `o=gluu` if not present
        if (!containsBranch(this.baseDn)) {
            addOrganizationBranch(this.baseDn, null);
        }
        //create `ou=configuration,o=gluu` if not present
        if (!containsBranch(String.format("%s,%s", ou("configuration"), this.baseDn))) {
            addBranch(String.format("%s,%s", ou("configuration"), this.baseDn), "configuration");
        }
        //create `ou=rp,ou=configuration,o=gluu` if not present
        if (!containsBranch(String.format("%s,%s,%s", ou("rp"), ou("configuration"), this.baseDn))) {
            addBranch(String.format("%s,%s,%s", ou("rp"), ou("configuration"), this.baseDn), "rp");
        }
        //create `ou=rp,o=gluu` if not present
        if (!containsBranch(getRpDn())) {
            addBranch(getRpDn(), "rp");
        }
        //create `ou=rp,ou=rp,o=gluu` if not present
        if (!containsBranch(String.format("%s,%s", getRpOu(), getRpDn()))) {
            addBranch(String.format("%s,%s", getRpOu(), getRpDn()), "rp");
        }
        //create `ou=expiredObjects,ou=rp,o=gluu` if not present
        if (!containsBranch(String.format("%s,%s", getExpiredObjOu(), getRpDn()))) {
            addBranch(String.format("%s,%s", getExpiredObjOu(), getRpDn()), "expiredObjects");
        }
    }

    public boolean containsBranch(String dn) {
        return this.persistenceEntryManager.contains(dn, SimpleBranch.class);
    }

    public void addOrganizationBranch(String dn, String oName) {
        OrganizationBranch branch = new OrganizationBranch();
        branch.setOrganizationName(oName);
        branch.setDn(dn);

        this.persistenceEntryManager.persist(branch);
    }

    public void addBranch(String dn, String ouName) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName(ouName);
        branch.setDn(dn);

        this.persistenceEntryManager.persist(branch);
    }

    public boolean create(Rp rp) {
        try {
            RpObject rpObj = new RpObject(getDnForRp(rp.getRpId()), rp.getRpId(), Jackson2.serializeWithoutNulls(rp));
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
            RpObject rpObj = new RpObject(getDnForRp(rp.getRpId()), rp.getRpId(), Jackson2.serializeWithoutNulls(rp));
            this.persistenceEntryManager.merge(rpObj);
            LOG.debug("RP updated successfully. RP : {} ", rpObj);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to update RP: {} ", rp, e);
        }
        return false;
    }

    public Rp getRp(String rpId) {
        try {
            RpObject rpFromGluuPersistance = getRpObject(rpId, new String[0]);

            Rp rp = MigrationService.parseRp(rpFromGluuPersistance.getData());
            if (rp != null) {
                LOG.debug("Found RP id: {}, RP : {} ", rpId, rp);
                return rp;
            }
            LOG.error("Failed to fetch RP by id: {} ", rpId);
            return null;
        } catch (Exception e) {
            LOG.error("Failed to update rpId: {} ", rpId, e);
        }
        return null;
    }

    private RpObject getRpObject(String rpId, String... returnAttributes) {
        return (RpObject) this.persistenceEntryManager.find(getDnForRp(rpId), RpObject.class, returnAttributes);
    }

    public ExpiredObject getExpiredObject(String key) {
        try {
            ExpiredObject expiredObject = (ExpiredObject) this.persistenceEntryManager.find(getDnForExpiredObj(key), ExpiredObject.class, null);
            if (expiredObject != null) {
                expiredObject.setType(ExpiredObjectType.fromValue(expiredObject.getTypeString()));
                LOG.debug("Found ExpiredObject id: {} , ExpiredObject : {} ", key, expiredObject);
                return expiredObject;
            }

            LOG.error("Failed to fetch ExpiredObject by id: {} ", key);
            return null;
        } catch (Exception e) {
            if (((e instanceof EntryPersistenceException)) && (e.getMessage().contains("Failed to find entry"))) {
                LOG.warn("Failed to fetch ExpiredObject by id: {}. {} ", key, e.getMessage());
                return null;
            }
            LOG.error("Failed to fetch ExpiredObject by id: {} ", key, e);
        }
        return null;
    }

    public boolean isExpiredObjectPresent(String key) {
        return getExpiredObject(key) != null;
    }

    public boolean removeAllRps() {
        try {
            this.persistenceEntryManager.remove(String.format("%s,%s", new Object[]{getRpOu(), getRpDn()}), RpObject.class, null, this.configuration.getPersistenceManagerRemoveCount());
            LOG.debug("Removed all Rps successfully. ");
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove all Rps", e);
        }
        return false;
    }

    public Set<Rp> getRps() {
        try {
            List<RpObject> rpObjects = this.persistenceEntryManager.findEntries(String.format("%s,%s", new Object[]{getRpOu(), getRpDn()}), RpObject.class, null);

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
            if (((e instanceof EntryPersistenceException)) && (e.getMessage().contains("Failed to find entries"))) {
                LOG.warn("Failed to fetch RpObjects. {} ", e.getMessage());
                return null;
            }
            LOG.error("Failed to fetch rps. Error: {} ", e.getMessage(), e);
        }
        return null;
    }

    public void destroy() {
        this.persistenceEntryManager.destroy();
    }

    public boolean remove(String rpId) {
        try {
            this.persistenceEntryManager.remove(getDnForRp(rpId));

            LOG.debug("Removed rp successfully. rpId: {} ", rpId);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove rp with rpId: {} ", rpId, e);
        }
        return false;
    }

    public boolean deleteExpiredObjectsByKey(String key) {
        try {
            this.persistenceEntryManager.remove(getDnForExpiredObj(key));
            LOG.debug("Removed expired_objects successfully: {} ", key);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove expired_objects: {} ", key, e);
        }
        return false;
    }

    public boolean deleteAllExpiredObjects() {
        try {
            final Calendar cal = Calendar.getInstance();
            final Date currentTime = cal.getTime();
            Filter exirationDateFilter = Filter.createLessOrEqualFilter("exp", this.persistenceEntryManager.encodeTime(baseDn, currentTime));

            this.persistenceEntryManager.remove(String.format("%s,%s", new Object[]{getExpiredObjOu(), getRpDn()}), ExpiredObject.class, exirationDateFilter, this.configuration.getPersistenceManagerRemoveCount());
            LOG.debug("Removed all expired_objects successfully. ");
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove expired_objects. ", e);
        }
        return false;
    }

    public String getDnForRp(String rpId) {
        return String.format("oxId=%s,%s,%s", new Object[]{rpId, getRpOu(), getRpDn()});
    }

    public String getDnForExpiredObj(String rpId) {
        return String.format("oxId=%s,%s,%s", new Object[]{rpId, getExpiredObjOu(), getRpDn()});
    }

    public String ou(String ouName) {
        return String.format("ou=%s", ouName);
    }

    private String getRpDn() {
        return String.format("%s,%s", ou("rp"), this.baseDn);
    }

    private String getRpOu() {
        return ou("rp");
    }

    private String getExpiredObjOu() {
        return ou("expiredObjects");
    }
}
