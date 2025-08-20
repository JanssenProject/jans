package io.jans.as.server.service.stat;

import io.jans.as.common.model.stat.Stat;
import io.jans.as.common.model.stat.StatEntry;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.net.InetAddressUtility;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.SimpleBranch;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.agkn.hll.HLL;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class StatService {

    // January - 202001, December - 202012
    private static final int REGWIDTH = 5;
    private static final int LOG_2_M = 15;

    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String ID_TOKEN_KEY = "id_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String UMA_TOKEN_KEY = "uma_token";
    public static final String LOGOUT_STATUS_TOKEN_KEY = "logout_status_jwt";

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    private String nodeId;
    private StatEntry currentEntry;
    private HLL hll;
    private ConcurrentMap<String, Map<String, Long>> tokenCounters;
    private final SimpleDateFormat periodDateFormat = new SimpleDateFormat("yyyyMM");

    private boolean initialized = false;

    @PostConstruct
    public void create() {
        initialized = false;
    }

    public boolean init() {
        try {
            final Set<FeatureFlagType> featureFlags = FeatureFlagType.from(appConfiguration);
            if (!featureFlags.isEmpty() && !featureFlags.contains(FeatureFlagType.STAT)) {
                log.trace("Stat service is not enabled.");
                return false;
            }
            log.info("Initializing Stat Service");

            initNodeId();
            if (StringUtils.isBlank(nodeId)) {
                log.error("Failed to initialize stat service. statNodeId is not set in configuration.");
                return false;
            }
            if (StringUtils.isBlank(getBaseDn())) {
                log.error("Failed to initialize stat service. 'stat' base dn is not set in configuration.");
                return false;
            }

            prepareMonthlyBranch();
            setupCurrentEntry();
            initialized = true;
            log.info("Initialized Stat Service");
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize Stat Service.", e);
            return false;
        }
    }

    public void updateStat() {
        log.trace("updateStat ...  (initialized: {})", initialized);

        if (!initialized) {
            return;
        }

        log.trace("Started updateStat ...");

        prepareMonthlyBranch();
        initNodeId();
        setupCurrentEntry();

        final Stat stat = currentEntry.getStat();
        stat.setTokenCountPerGrantType(tokenCounters);
        stat.setLastUpdatedAt(System.currentTimeMillis());

        synchronized (hll) {
            currentEntry.setUserHllData(Base64.getEncoder().encodeToString(hll.toBytes()));
        }

        log.trace("Updating entry dn {}", currentEntry.getDn());
        entryManager.merge(currentEntry);

        log.trace("Finished updateStat.");
    }

    public String currentMonth() {
        return periodDateFormat.format(new Date());
    }

    public String currentMonthDn() {
        final String baseDn = getBaseDn();
        final String month = currentMonth();
        return String.format("ou=%s,%s", month, baseDn);
    }

    private void setupCurrentEntry() {
        String currentMonth = currentMonth();
        String dn = String.format("jansId=%s,%s", nodeId, currentMonthDn()); // jansId=<id>,ou=yyyyMM,ou=stat,o=gluu
        log.trace("Stat entry dn: {}", dn);

        final boolean sameMonth = currentEntry != null && currentMonth.equals(currentEntry.getStat().getMonth());
        if (sameMonth) {
            log.trace("Same month {}", currentMonth);
            return;
        } else {
            log.trace("Different month {}", currentMonth);
            currentEntry = null; // set current entry to null to force re-fetch it from DB or create new one
        }

        try {
            StatEntry entryFromPersistence = entryManager.find(StatEntry.class, dn);
            if (entryFromPersistence != null && currentMonth.equals(entryFromPersistence.getStat().getMonth())) {
                hll = HLL.fromBytes(Base64.getDecoder().decode(entryFromPersistence.getUserHllData()));
                tokenCounters = new ConcurrentHashMap<>(entryFromPersistence.getStat().getTokenCountPerGrantType());
                currentEntry = entryFromPersistence;
                log.trace("Stat entry {} loaded.", dn);
                if (StringUtils.isBlank(currentEntry.getMonth()) && currentEntry.getStat() != null) {
                    currentEntry.setMonth(currentEntry.getStat().getMonth());
                }
                return;
            } else {
                log.trace("Month does not match. Current month {}, entry month {}, entry dn: {}", currentMonth, entryFromPersistence != null ? entryFromPersistence.getStat().getMonth() : "", dn);
            }
        } catch (EntryPersistenceException e) {
            log.trace("Stat entry is not found in persistence. dn: " + dn, e);
        }

        log.trace("Creating stat entry ...");
        hll = newHll();
        tokenCounters = new ConcurrentHashMap<>();

        currentEntry = new StatEntry();
        currentEntry.setId(nodeId);
        currentEntry.setDn(dn);
        currentEntry.setUserHllData(Base64.getEncoder().encodeToString(hll.toBytes()));

        currentEntry.getStat().setMonth(currentMonth);
        currentEntry.setMonth(currentMonth);
        entryManager.persist(currentEntry);
        log.trace("Created stat entry.");
    }

    public HLL newHll() {
        return new HLL(LOG_2_M, REGWIDTH);
    }

    private void initNodeId() {
        final String currentMonth = currentMonth();
        if (StringUtils.isNotBlank(nodeId) && nodeId.endsWith(currentMonth)) {
            log.trace("NodeId is not blank: {}", nodeId);
            return;
        }

        try {
            nodeId = InetAddressUtility.getMACAddressOrNull() + "_" + currentMonth;
            if (StringUtils.isNotBlank(nodeId)) {
                log.trace("NodeId created: " + nodeId);
                return;
            }

            nodeId = UUID.randomUUID().toString() + "_" + currentMonth;
            log.trace("NodeId created: " + nodeId);
        } catch (Exception e) {
            log.error("Failed to identify nodeId.", e);
            nodeId = UUID.randomUUID().toString() + "_" + currentMonth;
            log.trace("NodeId created: " + nodeId);
        }
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getBaseDn() {
        return staticConfiguration.getBaseDn().getStat();
    }

    private void prepareMonthlyBranch() {
        if (!entryManager.hasBranchesSupport(getBaseDn())) {
            log.trace("Monthly branch creation is skipped. DB does not support branches.");
            return;
        }

        String monthlyDn = currentMonthDn();

        try {
            if (!entryManager.contains(monthlyDn, SimpleBranch.class)) { // Create ou=yyyyMM branch if needed
                createBranch(monthlyDn, currentMonth());
                log.info("Monthly branch is created: {}", monthlyDn);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled())
                log.error("Failed to prepare monthly branch: " + monthlyDn, e);
            throw e;
        }
    }

    public void createBranch(String branchDn, String ou) {
        try {
            SimpleBranch branch = new SimpleBranch();
            branch.setOrganizationalUnitName(ou);
            branch.setDn(branchDn);

            entryManager.persist(branch);
        } catch (EntryPersistenceException ex) {
            // Check if another process added this branch already
            if (!entryManager.contains(branchDn, SimpleBranch.class)) {
                throw ex;
            }
        }
    }

    public void reportActiveUser(String id) {
        if (!initialized) {
            return;
        }

        if (StringUtils.isBlank(id)) {
            return;
        }

        final int hashCode = id.hashCode();
        try {
            setupCurrentEntry();
            synchronized (hll) {
                hll.addRaw(hashCode);
            }
        } catch (Exception e) {
            log.error("Failed to report active user, id: " + id + ", hash: " + hashCode, e);
        }
    }

    public void reportAccessToken(GrantType grantType) {
        reportToken(grantType, ACCESS_TOKEN_KEY);
    }

    public void reportLogoutStatusJwt(GrantType grantType) {
        reportToken(grantType, LOGOUT_STATUS_TOKEN_KEY);
    }

    public void reportIdToken(GrantType grantType) {
        reportToken(grantType, ID_TOKEN_KEY);
    }

    public void reportRefreshToken(GrantType grantType) {
        reportToken(grantType, REFRESH_TOKEN_KEY);
    }

    public void reportUmaToken(GrantType grantType) {
        reportToken(grantType, UMA_TOKEN_KEY);
    }


    private void reportToken(GrantType grantType, String tokenKey) {
        if (!initialized) {
            return;
        }

        if (grantType == null || tokenKey == null) {
            return;
        }
        if (tokenCounters == null) {
            log.error("Stat service is not initialized.");
            return;
        }

        Map<String, Long> tokenMap = tokenCounters.computeIfAbsent(grantType.getValue(), k -> new ConcurrentHashMap<>());

        Long counter = tokenMap.get(tokenKey);

        if (counter == null) {
            counter = 1L;
        } else {
            counter++;
        }

        tokenMap.put(tokenKey, counter);

    }
}