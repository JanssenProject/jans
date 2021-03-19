package io.jans.as.server.service.stat;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.as.common.model.stat.Stat;
import io.jans.as.common.model.stat.StatEntry;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.net.InetAddressUtility;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.SimpleBranch;
import net.agkn.hll.HLL;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class StatService {

    // January - 202001, December - 202012
    private static final SimpleDateFormat PERIOD_DATE_FORMAT = new SimpleDateFormat("yyyyMM");
    private static final int regwidth = 5;
    private static final int log2m = 15;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    private String nodeId;
    private String monthlyDn;
    private StatEntry currentEntry;
    private HLL hll;
    private ConcurrentMap<String, Map<String, Long>> tokenCounters;

    private boolean initialized = false;

    @PostConstruct
    public void create() {
        initialized = false;
    }

    public boolean init() {
        try {
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

            final Date now = new Date();
            prepareMonthlyBranch(now);
            if (StringUtils.isBlank(monthlyDn)) {
                log.error("Failed to initialize stat service. Failed to prepare monthly branch.");
                return false;
            }
            log.trace("Monthly branch created: " + monthlyDn);

            setupCurrentEntry(now);
            log.info("Initialized Stat Service");
            initialized = true;
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize Stat Service.", e);
            return false;
        }
    }

    public void updateStat() {
        if (!initialized) {
            return;
        }

        log.trace("Started updateStat ...");

        Date now = new Date();
        prepareMonthlyBranch(now);
        if (StringUtils.isBlank(monthlyDn)) {
            log.error("Failed to update stat. Unable to prepare monthly branch.");
            return;
        }

        setupCurrentEntry(now);

        final Stat stat = currentEntry.getStat();
        stat.setTokenCountPerGrantType(tokenCounters);
        stat.setLastUpdatedAt(now.getTime());

        currentEntry.setUserHllData(new String(hll.toBytes(), StandardCharsets.UTF_8));
        entryManager.merge(currentEntry);

        log.trace("Finished updateStat.");
    }

    private void setupCurrentEntry() {
        setupCurrentEntry(new Date());
    }

    private void setupCurrentEntry(Date now) {
        final String month = PERIOD_DATE_FORMAT.format(now);
        String dn = String.format("jansId=%s,%s", nodeId, monthlyDn); // jansId=<id>,ou=yyyyMM,ou=stat,o=gluu

        if (currentEntry != null && month.equals(currentEntry.getStat().getMonth())) {
            return;
        }

        try {
            StatEntry entryFromPersistence = entryManager.find(StatEntry.class, dn);
            if (entryFromPersistence != null && month.equals(entryFromPersistence.getStat().getMonth())) {
                hll = HLL.fromBytes(entryFromPersistence.getUserHllData().getBytes(StandardCharsets.UTF_8));
                tokenCounters = new ConcurrentHashMap<>(entryFromPersistence.getStat().getTokenCountPerGrantType());
                currentEntry = entryFromPersistence;
                log.trace("Stat entry loaded.");
                return;
            }
        } catch (EntryPersistenceException e) {
            log.trace("Stat entry is not found in persistence.");
        }

        if (currentEntry == null) {
            log.trace("Creating stat entry ...");
            hll = new HLL(log2m, regwidth);
            tokenCounters = new ConcurrentHashMap<>();

            currentEntry = new StatEntry();
            currentEntry.setId(nodeId);
            currentEntry.setDn(dn);
            currentEntry.setUserHllData(new String(hll.toBytes(), StandardCharsets.UTF_8));
            currentEntry.getStat().setMonth(PERIOD_DATE_FORMAT.format(new Date()));
            entryManager.persist(currentEntry);
            log.trace("Created stat entry.");
        }
    }

    private void initNodeId() {
        if (StringUtils.isNotBlank(nodeId)) {
            return;
        }

        try {
            nodeId = InetAddressUtility.getMACAddressOrNull();
            if (StringUtils.isNotBlank(nodeId)) {
                return;
            }

            nodeId = UUID.randomUUID().toString();
        } catch (Exception e) {
            log.error("Failed to identify nodeId.", e);
            nodeId = UUID.randomUUID().toString();
        }
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getBaseDn() {
        return staticConfiguration.getBaseDn().getStat();
    }

    private void prepareMonthlyBranch(Date now) {
        final String baseDn = getBaseDn();
        if (!entryManager.hasBranchesSupport(baseDn)) {
            return;
        }

        final String month = PERIOD_DATE_FORMAT.format(now); // yyyyMM
        monthlyDn = String.format("ou=%s,%s", month, baseDn); // ou=yyyyMM,ou=stat,o=gluu

        try {
            if (!entryManager.contains(monthlyDn, SimpleBranch.class)) { // Create ou=yyyyMM branch if needed
                createBranch(monthlyDn, month);
            }
        } catch (Exception e) {
            log.error("Failed to prepare monthly branch: " + monthlyDn, e);
            monthlyDn = null;
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
        setupCurrentEntry();
        hll.addRaw(id.hashCode());
    }

    public void reportAccessToken(GrantType grantType) {
        reportToken(grantType, "access_token");
    }

    public void reportIdToken(GrantType grantType) {
        reportToken(grantType, "id_token");
    }

    public void reportRefreshToken(GrantType grantType) {
        reportToken(grantType, "refresh_token");
    }

    public void reportUmaToken(GrantType grantType) {
        reportToken(grantType, "uma_token");
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