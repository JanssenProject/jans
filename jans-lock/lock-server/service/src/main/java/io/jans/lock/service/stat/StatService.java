package io.jans.lock.service.stat;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.lock.model.Stat;
import io.jans.lock.model.StatEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.net.InetAddressUtility;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.SimpleBranch;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.agkn.hll.HLL;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
@ApplicationScoped
public class StatService {

	private static final String RESULT_ALLOW = "allow";
    private static final String RESULT_DENY = "deny";

    // January - 202001, December - 202012
    private static final int REGWIDTH = 5;
    private static final int LOG_2_M = 15;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    private String nodeId;
    private String monthlyDn;
    private StatEntry currentEntry;
    private HLL userHll, clientHll;
    private ConcurrentMap<String, Map<String, Long>> opearationCounters;
    private final SimpleDateFormat periodDateFormat = new SimpleDateFormat("yyyyMM");

    private boolean initialized = false;
    private final ReentrantLock setupCurrentEntryLock = new ReentrantLock();

    @PostConstruct
    public void create() {
        initialized = false;
    }

    public boolean init() {
        try {
            if (!appConfiguration.isStatEnabled()) {
                log.trace("Stat service is not enabled");
                return false;
            }
            log.info("Initializing Stat Service");

            final Date now = new Date();
            initNodeId(now);
            if (StringUtils.isBlank(nodeId)) {
                log.error("Failed to initialize stat service. statNodeId is not set in configuration");
                return false;
            }
            if (StringUtils.isBlank(getBaseDn())) {
                log.error("Failed to initialize stat service. 'stat' base dn is not set in configuration");
                return false;
            }

            prepareMonthlyBranch(now);
            log.trace("Monthly branch created: {}", monthlyDn);

            setupCurrentEntry(now);
            log.info("Initialized Stat Service");

            initialized = true;
            return true;
        } catch (Exception ex) {
            log.error("Failed to initialize Stat Service", ex);
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

        setupCurrentEntry(now);

        final Stat stat = currentEntry.getStat();
        stat.setOperationsByType(opearationCounters);
        stat.setLastUpdatedAt(now.getTime());

        synchronized (userHll) {
            currentEntry.setUserHllData(Base64.getEncoder().encodeToString(userHll.toBytes()));
        }
        synchronized (clientHll) {
            currentEntry.setClientHllData(Base64.getEncoder().encodeToString(clientHll.toBytes()));
        }
        entryManager.merge(currentEntry);

        log.trace("Finished updateStat");
    }

    private void setupCurrentEntry() {
        setupCurrentEntry(new Date());
    }

	private void setupCurrentEntry(Date now) {
		String dn = String.format("jansId=%s,%s", nodeId, monthlyDn); // jansId=<id>,ou=yyyyMM,ou=lock,ou=stat,o=jans

		final String month = monthString(now);
		initNodeId(now);

		if (currentEntry != null && month.equals(currentEntry.getStat().getMonth())) {
			return;
		}

		setupCurrentEntryLock.lock();
		try {
			// After getting lock check if another thread did initialization already
			if (currentEntry != null && month.equals(currentEntry.getStat().getMonth())) {
				return;
			}

			StatEntry entryFromPersistence = entryManager.find(StatEntry.class, dn);
			if ((entryFromPersistence != null) && month.equals(entryFromPersistence.getStat().getMonth())) {
				userHll = HLL.fromBytes(Base64.getDecoder().decode(entryFromPersistence.getUserHllData()));
				clientHll = HLL.fromBytes(Base64.getDecoder().decode(entryFromPersistence.getClientHllData()));
				opearationCounters = new ConcurrentHashMap<>(entryFromPersistence.getStat().getOperationsByType());
				currentEntry = entryFromPersistence;
				log.trace("Stat entry loaded");

				if (StringUtils.isBlank(currentEntry.getMonth()) && currentEntry.getStat() != null) {
					currentEntry.setMonth(currentEntry.getStat().getMonth());
				}
				return;
			}
		} catch (EntryPersistenceException e) {
			log.trace("Stat entry is not found in persistence");
		} finally {
			setupCurrentEntryLock.unlock();
		}

		log.trace("Creating stat entry ...");
		userHll = newUserHll();
		clientHll = newClientHll();
		opearationCounters = new ConcurrentHashMap<>();
		final String monthString = periodDateFormat.format(new Date());

		currentEntry = new StatEntry();
		currentEntry.setId(nodeId);
		currentEntry.setDn(dn);
		currentEntry.setUserHllData(Base64.getEncoder().encodeToString(userHll.toBytes()));
		currentEntry.setClientHllData(Base64.getEncoder().encodeToString(clientHll.toBytes()));

		currentEntry.getStat().setMonth(monthString);
		currentEntry.setMonth(monthString);
		entryManager.persist(currentEntry);
		
		log.trace("Created stat entry");
	}

	protected HLL newUserHll() {
        return new HLL(LOG_2_M, REGWIDTH);
    }

    protected HLL newClientHll() {
        return new HLL(LOG_2_M, REGWIDTH);
    }

    private void initNodeId(Date now) {
        if (StringUtils.isNotBlank(nodeId)) {
            return;
        }

        try {
            nodeId = InetAddressUtility.getMACAddressOrNull() + "_" + monthString(now);
            if (StringUtils.isNotBlank(nodeId)) {
                return;
            }

            nodeId = UUID.randomUUID().toString() + "_" + monthString(now);
        } catch (Exception e) {
            log.error("Failed to identify nodeId.", e);
            nodeId = UUID.randomUUID().toString() + "_" + monthString(now);
        }
    }

    public String getNodeId() {
        return nodeId;
    }

    public String monthString(Date now) {
        return periodDateFormat.format(now); // yyyyMM
    }

    private void prepareMonthlyBranch(Date now) {
        final String baseDn = getBaseDn();
        final String month = monthString(now); // yyyyMM
        monthlyDn = String.format("ou=%s,%s", month, baseDn); // ou=yyyyMM,ou=lock,ou=stat,o=jans

        if (!entryManager.hasBranchesSupport(baseDn)) {
            return;
        }

        try {
            if (!entryManager.contains(monthlyDn, SimpleBranch.class)) { // Create ou=yyyyMM branch if needed
                createBranch(monthlyDn, month);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled())
                log.error("Failed to prepare monthly branch: " + monthlyDn, e);
            throw e;
        }
    }

    public String getBaseDn() {
        return staticConfiguration.getBaseDn().getStat();
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
            synchronized (userHll) {
                userHll.addRaw(hashCode);
            }
        } catch (Exception e) {
            log.error("Failed to report active user, id: " + id + ", hash: " + hashCode, e);
        }
    }

    public void reportActiveClient(String id) {
        if (!initialized) {
            return;
        }

        if (StringUtils.isBlank(id)) {
            return;
        }

        final int hashCode = id.hashCode();
        try {
            setupCurrentEntry();
            synchronized (clientHll) {
            	clientHll.addRaw(hashCode);
            }
        } catch (Exception e) {
            log.error("Failed to report active client, id: " + id + ", hash: " + hashCode, e);
        }
    }

    public void reportAllow(String operationGroup) {
    	reportOpearation(operationGroup, RESULT_ALLOW);
    }
    
    public void reportDeny(String operationGroup) {
    	reportOpearation(operationGroup, RESULT_DENY);
    }

    public void reportOpearation(String operationGroup, String operationType) {
        if (!initialized) {
            return;
        }

        if (operationGroup == null || operationType == null) {
            return;
        }
        if (opearationCounters == null) {
            log.error("Stat service is not initialized");
            return;
        }

        Map<String, Long> operationMap = opearationCounters.computeIfAbsent(operationGroup, v -> new ConcurrentHashMap<>());

        Long counter = operationMap.get(operationType);

        if (counter == null) {
            counter = 1L;
        } else {
            counter++;
        }

        operationMap.put(operationType, counter);
    }

}
