/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.server.persistence.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.ca.common.ExpiredObject;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.Jackson2;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.configuration.ConfigurationFactory;
import io.jans.ca.server.configuration.model.ApiConf;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.persistence.modal.OrganizationBranch;
import io.jans.ca.server.persistence.modal.RpObject;
import io.jans.ca.server.service.MigrationService;
import io.jans.configapi.model.status.StatsData;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

import java.util.*;

import static io.jans.ca.server.configuration.ConfigurationFactory.CONFIGURATION_ENTRY_DN;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class MainPersistenceService implements PersistenceService {

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    Logger logger;

    private StatsData statsData;
    private static final String BASE_DN = "o=jans";
    private static final String OU_CONFIGURATION = "configuration";
    private static final String OU_JANS_CLIENT_API = "jans-client-api";

    private ApiAppConfiguration configuration;

    public ApiConf findConf() {
        final String dn = configurationFactory.getConfigurationDn(CONFIGURATION_ENTRY_DN);
        return persistenceManager.find(dn, ApiConf.class, null);
    }

    public void mergeConfiguration(ApiConf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.merge(conf);
    }

    public void mergeGluuConfiguration(GluuConfiguration conf) {
        persistenceManager.merge(conf);
    }

    public ApiAppConfiguration find() {
        return configurationFactory.getAppConfiguration();
    }

    public GluuConfiguration findGluuConfiguration() {
        String configurationDn = findConf().getStaticConf().getBaseDn().getConfiguration();
        if (StringHelper.isEmpty(configurationDn)) {
            return null;
        }
        return persistenceManager.find(GluuConfiguration.class, configurationDn);
    }

    public String getPersistenceType() {
        return configurationFactory.getBaseConfiguration().getString("persistence.type");
    }

    public StatsData getStatsData() {
        return statsData;
    }

    public void setStatsData(StatsData statsData) {
        this.statsData = statsData;
    }

    public void create() {
        logger.debug("Creating JansPersistence for Api Client...");
        try {
            this.configuration = find();
            prepareBranch();
        } catch (Exception e) {
            throw new IllegalStateException("Error JansPersistence for Api Client", e);
        }
    }

    public void prepareBranch() {

        if (!this.persistenceManager.hasBranchesSupport(BASE_DN)) {
            return;
        }
        //create `o=jans` if not present
        if (!containsBranch(BASE_DN)) {
            addOrganizationBranch(BASE_DN, null);
        }
        //create `ou=configuration,o=jans` if not present
        if (!containsBranch(joinWithComa(ou(OU_CONFIGURATION), BASE_DN))) {
            addBranch(joinWithComa(ou(OU_CONFIGURATION), BASE_DN), OU_CONFIGURATION);
        }
        //create `ou=client-api,ou=configuration,o=jans` if not present
        if (!containsBranch(joinWithComa(ou(OU_JANS_CLIENT_API), ou(OU_CONFIGURATION), BASE_DN))) {
            addBranch(joinWithComa(ou(OU_JANS_CLIENT_API), ou(OU_CONFIGURATION), BASE_DN), OU_JANS_CLIENT_API);
        }
        //create `ou=client-api,o=jans` if not present
        if (!containsBranch(getClientApiDn())) {
            addBranch(getClientApiDn(), "client-api");
        }
        //create `ou=rp,ou=client-api,o=jans` if not present
        if (!containsBranch(joinWithComa(getRpOu(), getClientApiDn()))) {
            addBranch(joinWithComa(getRpOu(), getClientApiDn()), "rp");
        }
        //create `ou=expiredObjects,ou=client-api,o=jans` if not present
        if (!containsBranch(joinWithComa(getExpiredObjOu(), getClientApiDn()))) {
            addBranch(joinWithComa(getExpiredObjOu(), getClientApiDn()), "expiredObjects");
        }
    }

    private String joinWithComa(String... words) {
        String result = "";
        String coma = "";
        for (String word : words) {
            result += coma + word;
            coma = ",";
        }
        return result;
    }

    public boolean containsBranch(String dn) {
        return this.persistenceManager.contains(dn, SimpleBranch.class);
    }

    public void addOrganizationBranch(String dn, String oName) {
        OrganizationBranch branch = new OrganizationBranch();
        branch.setOrganizationName(oName);
        branch.setDn(dn);

        this.persistenceManager.persist(branch);
    }

    public void addBranch(String dn, String ouName) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName(ouName);
        branch.setDn(dn);

        this.persistenceManager.persist(branch);
    }

    public boolean create(Rp rp) {
        try {
            RpObject rpObj = new RpObject(getDnForRp(rp.getRpId()), rp.getRpId(), Jackson2.serializeWithoutNulls(rp));
            this.persistenceManager.persist(rpObj);
            logger.debug("RP created successfully. RP : {} ", rp);
            return true;
        } catch (Exception e) {
            logger.error("Failed to create RP: {} ", rp, e);
        }
        return false;
    }

    public boolean createExpiredObject(ExpiredObject obj) {
        try {
            if (isExpiredObjectPresent(obj.getKey())) {
                logger.warn("Expired_object already present. Object : {} ", obj.getKey());
                return true;
            }
            obj.setTypeString(obj.getType().getValue());
            obj.setDn(getDnForExpiredObj(obj.getKey()));
            this.persistenceManager.persist(obj);
            logger.debug("Expired_object created successfully. Object : {} ", obj.getKey());
            return true;
        } catch (Exception e) {
            logger.error("Failed to create ExpiredObject: {} ", obj.getKey(), e);
        }
        return false;
    }

    public boolean update(Rp rp) {
        try {
            RpObject rpObj = new RpObject(getDnForRp(rp.getRpId()), rp.getRpId(), Jackson2.serializeWithoutNulls(rp));
            this.persistenceManager.merge(rpObj);
            logger.debug("RP updated successfully. RP : {} ", rpObj);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update RP: {} ", rp, e);
        }
        return false;
    }

    public Rp getRp(String rpId) {
        try {
            RpObject rpFromGluuPersistance = getRpObject(rpId, new String[0]);

            Rp rp = MigrationService.parseRp(rpFromGluuPersistance.getData());
            if (rp != null) {
                logger.debug("Found RP id: {}, RP : {} ", rpId, rp);
                return rp;
            }
            logger.error("Failed to fetch RP by id: {} ", rpId);
            return null;
        } catch (Exception e) {
            logger.error("Failed to update rpId: {} ", rpId, e);
        }
        return null;
    }

    private RpObject getRpObject(String rpId, String... returnAttributes) {
        return (RpObject) this.persistenceManager.find(getDnForRp(rpId), RpObject.class, returnAttributes);
    }

    public ExpiredObject getExpiredObject(String key) {
        try {
            ExpiredObject expiredObject = (ExpiredObject) this.persistenceManager.find(getDnForExpiredObj(key), ExpiredObject.class, null);
            if (expiredObject != null) {
                expiredObject.setType(ExpiredObjectType.fromValue(expiredObject.getTypeString()));
                logger.debug("Found ExpiredObject id: {} , ExpiredObject : {} ", key, expiredObject);
                return expiredObject;
            }

            logger.error("Failed to fetch ExpiredObject by id: {} ", key);
            return null;
        } catch (Exception e) {
            if (((e instanceof EntryPersistenceException)) && (e.getMessage().contains("Failed to find entry"))) {
                logger.warn("Failed to fetch ExpiredObject by id: {}. {} ", key, e.getMessage());
                return null;
            }
            logger.error("Failed to fetch ExpiredObject by id: {} ", key, e);
        }
        return null;
    }

    public boolean isExpiredObjectPresent(String key) {
        return getExpiredObject(key) != null;
    }

    public boolean removeAllRps() {
        try {
            this.persistenceManager.remove(joinWithComa(getRpOu(), getClientApiDn()), RpObject.class, null, this.configuration.getPersistenceManagerRemoveCount());
            logger.debug("Removed all Rps successfully. ");
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove all Rps", e);
        }
        return false;
    }

    public Set<Rp> getRps() {
        Set<Rp> result = new HashSet<Rp>();
        try {
            List<RpObject> rpObjects = this.persistenceManager.findEntries(String.format("%s,%s", new Object[]{getRpOu(), getClientApiDn()}), RpObject.class, null);
            for (RpObject ele : rpObjects) {
                Rp rp = MigrationService.parseRp(ele.getData());
                if (rp != null) {
                    result.add(rp);
                } else {
                    logger.error("Failed to parse rp, id: {}, dn: {} ", ele.getId(), ele.getDn());
                }
            }
            return result;
        } catch (Exception e) {
            if ((e instanceof EntryPersistenceException) && (e.getMessage().contains("Failed to find entries"))) {
                logger.warn("Failed to fetch RpObjects. {} ", e.getMessage());
                return new HashSet<Rp>();
            }
            logger.error("Failed to fetch rps. Error: {} ", e.getMessage(), e);
        }
        return result;
    }

    public void destroy() {
        this.persistenceManager.destroy();
    }

    public boolean remove(String rpId) {
        try {
            this.persistenceManager.remove(getDnForRp(rpId), RpObject.class);

            logger.debug("Removed rp successfully. rpId: {} ", rpId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove rp with rpId: {} ", rpId, e);
        }
        return false;
    }

    public boolean deleteExpiredObjectsByKey(String key) {
        try {
            this.persistenceManager.remove(getDnForExpiredObj(key), ExpiredObject.class);
            logger.debug("Removed expired_objects successfully: {} ", key);
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove expired_objects: {} ", key, e);
        }
        return false;
    }

    public boolean deleteAllExpiredObjects() {
        try {
            final Calendar cal = Calendar.getInstance();
            final Date currentTime = cal.getTime();
            Filter exirationDateFilter = Filter.createLessOrEqualFilter("exp", this.persistenceManager.encodeTime(BASE_DN, currentTime));

            this.persistenceManager.remove(String.format("%s,%s", new Object[]{getExpiredObjOu(), getClientApiDn()}), ExpiredObject.class, exirationDateFilter, this.configuration.getPersistenceManagerRemoveCount());
            logger.debug("Removed all expired_objects successfully. ");
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove expired_objects. ", e);
        }
        return false;
    }

    public String getDnForRp(String rpId) {
        return String.format("jansId=%s,%s,%s", new Object[]{rpId, getRpOu(), getClientApiDn()});
    }

    public String getDnForExpiredObj(String rpId) {
        return String.format("rpId=%s,%s,%s", new Object[]{rpId, getExpiredObjOu(), getClientApiDn()});
    }

    public String ou(String ouName) {
        return String.format("ou=%s", ouName);
    }

    private String getClientApiDn() {
        return joinWithComa(ou("client-api"), BASE_DN);
    }

    private String getRpOu() {
        return ou("rp");
    }

    private String getExpiredObjOu() {
        return ou("expiredObjects");
    }

}
