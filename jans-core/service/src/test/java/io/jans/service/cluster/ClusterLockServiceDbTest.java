/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.cluster;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.model.cluster.ClusterLock;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;
import io.jans.service.test.MutableClock;
import io.jans.service.test.cluster.ClaimRace;
import io.jans.service.test.cluster.FixedClusterLockService;
import io.jans.service.test.orm.InMemoryEntryManager;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;

/**
 * {@link ClusterLockService} against a real SQL database: the 8-thread claim race, the entity mapping,
 * heartbeat, release, the state row and purge.
 *
 * <p>Skipped unless the system property {@value #PROFILE_PROPERTY} names a profile directory of
 * {@code jans-orm/integration-test} (the directory that holds {@code conf/jans-sql.properties},
 * {@code conf/salt} and, for TLS, the certificate the properties refer to). The database must have the
 * {@code jansClusterLock} table. Every test uses a random task name, and all entries of those tasks are
 * removed in {@link #destroyEntryManager()}, also after a failure.</p>
 *
 * <pre>
 * mvn -o test -pl service -Dtest=ClusterLockServiceDbTest \
 *     -Djans.test.orm.profile=/path/to/jans-orm/integration-test/profiles/&lt;host&gt;
 * </pre>
 */
public class ClusterLockServiceDbTest {

    public static final String PROFILE_PROPERTY = "jans.test.orm.profile";

    private static final String BASE_DN = "ou=node,o=jans";
    private static final String TASK_PREFIX = "t02-test-";
    private static final String SQL_PREFIX = "sql#";
    private static final String SSL_FACTORY_ARG = "connection.driver-property.sslfactoryarg";
    private static final String FILE_URL_PREFIX = "file:";
    private static final int THREADS = 8;
    private static final Duration LEASE = Duration.ofMinutes(5);

    private PersistenceEntryManager entryManager;
    private final Set<String> tasks = new LinkedHashSet<>();

    @BeforeClass
    public void createEntryManager() throws Exception {
        String profile = System.getProperty(PROFILE_PROPERTY);
        if (StringUtils.isBlank(profile)) {
            throw new SkipException("DB profile is not set; run with -D" + PROFILE_PROPERTY + "=<profile directory>");
        }

        File confDir = new File(profile, "conf");
        Properties sqlProperties = load(new File(confDir, "jans-sql.properties"));
        String encodeSalt = load(new File(confDir, "salt")).getProperty("encodeSalt");
        if (StringUtils.isBlank(encodeSalt)) {
            throw new IllegalStateException("Profile salt file has no encodeSalt");
        }

        Properties decrypted = PropertiesDecrypter.decryptAllProperties(StringEncrypter.instance(encodeSalt.trim()), sqlProperties);
        String sslFactoryArg = decrypted.getProperty(SSL_FACTORY_ARG);
        if ((sslFactoryArg != null) && sslFactoryArg.startsWith(FILE_URL_PREFIX)) {
            File certificate = new File(sslFactoryArg.substring(FILE_URL_PREFIX.length()));
            if (!certificate.isAbsolute()) {
                decrypted.setProperty(SSL_FACTORY_ARG, FILE_URL_PREFIX + new File(confDir, certificate.getPath()).getAbsolutePath());
            }
        }

        Properties connectionProperties = new Properties();
        for (String name : decrypted.stringPropertyNames()) {
            connectionProperties.setProperty(SQL_PREFIX + name, decrypted.getProperty(name));
        }

        SqlEntryManagerFactory factory = new SqlEntryManagerFactory();
        factory.create();
        entryManager = factory.createEntryManager(connectionProperties);
    }

    @AfterClass(alwaysRun = true)
    public void destroyEntryManager() {
        if (entryManager == null) {
            return;
        }

        try {
            for (String task : tasks) {
                entryManager.remove(BASE_DN, ClusterLock.class, Filter.createEqualityFilter("jansType", task), 1000);
                List<ClusterLock> left = entryManager.findEntries(BASE_DN, ClusterLock.class, Filter.createEqualityFilter("jansType", task));
                for (ClusterLock entry : left) {
                    entryManager.remove(entry.getDn(), ClusterLock.class);
                }
            }
        } finally {
            entryManager.destroy();
        }
    }

    @Test
    public void exactlyOneOfEightThreadsWinsOnDatabase() throws Exception {
        String task = newTask();
        String slot = "1";

        List<FixedClusterLockService> services = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            services.add(new FixedClusterLockService(entryManager, BASE_DN, "db-key-" + i, "db-node-" + i, null, null));
        }

        List<ClusterLock> winners = ClaimRace.run(services, task, slot, LEASE);

        assertEquals(winners.size(), 1, "Claim winners; 0 means every insert failed (is the jansClusterLock table missing?)");
        List<ClusterLock> rows = findRows(task);
        assertEquals(rows.size(), 1, "Rows in jansClusterLock for the slot");
        assertEquals(rows.get(0).getDn(), services.get(0).buildClaimDn(task, slot));
        assertEquals(rows.get(0).getLockKey(), winners.get(0).getLockKey());
    }

    @Test
    public void entityMappingRoundTrip() {
        String task = newTask();
        MutableClock clock = newClock();
        FixedClusterLockService node = new FixedClusterLockService(entryManager, BASE_DN, "map-key", "map-node", 7, clock);

        ClusterLock claim = node.tryClaim(task, "1", LEASE).get();
        ClusterLock stored = entryManager.find(ClusterLock.class, claim.getDn());

        assertEquals(stored.getDn(), claim.getDn());
        assertEquals(stored.getId(), task + "_1");
        assertEquals(stored.getTask(), task);
        assertEquals(stored.getLockKey(), claim.getLockKey());
        assertEquals(stored.getNodeIdentifier(), "map-node");
        assertEquals(stored.getClusterNodeId(), Integer.valueOf(7));
        assertEquals(stored.getCreationDate().toInstant(), clock.instant());
        assertEquals(stored.getLastUpdate().toInstant(), clock.instant());
        assertEquals(stored.getExpirationDate().toInstant(), clock.instant().plus(LEASE));
        assertEquals(stored.getDeletable(), Boolean.TRUE);
        assertEquals(stored.getStatus(), ClusterLock.STATUS_RUNNING);
        assertNull(stored.getData());
        assertNull(stored.getTtl(), "ttl has no column, so it is not read back");
        assertTrue(node.isActive(stored, clock.instant()));

        // jansId is ignoreDuringUpdate: a merge must not change it
        stored.setId("changed-id");
        stored.setData("{\"merged\":true}");
        entryManager.merge(stored);

        ClusterLock merged = entryManager.find(ClusterLock.class, claim.getDn());
        assertEquals(merged.getId(), task + "_1");
        assertEquals(merged.getData(), "{\"merged\":true}");
    }

    @Test
    public void heartbeatByOwnerAndStranger() {
        String task = newTask();
        MutableClock clock = newClock();
        FixedClusterLockService owner = new FixedClusterLockService(entryManager, "hb-owner", clock);
        FixedClusterLockService stranger = new FixedClusterLockService(entryManager, "hb-stranger", clock);
        ClusterLock claim = owner.tryClaim(task, "1", LEASE).get();

        Instant later = clock.advance(Duration.ofSeconds(60));
        assertTrue(owner.heartbeat(claim));

        ClusterLock stored = entryManager.find(ClusterLock.class, claim.getDn());
        assertEquals(stored.getLastUpdate().toInstant(), later);
        assertEquals(stored.getExpirationDate().toInstant(), later.plus(LEASE));
        assertEquals(stored.getCreationDate().toInstant(), later.minusSeconds(60));
        Map<String, Object> before = InMemoryEntryManager.describe(stored);

        clock.advance(Duration.ofSeconds(60));
        assertFalse(stranger.heartbeat(claim));
        assertFalse(stranger.heartbeat(stranger.find(task, "1").get()));

        assertEquals(InMemoryEntryManager.describe(entryManager.find(ClusterLock.class, claim.getDn())), before);
    }

    @Test
    public void releaseByOwnerAndStranger() {
        String task = newTask();
        MutableClock clock = newClock();
        FixedClusterLockService owner = new FixedClusterLockService(entryManager, "rel-owner", clock);
        FixedClusterLockService stranger = new FixedClusterLockService(entryManager, "rel-stranger", clock);
        ClusterLock claim = owner.tryClaim(task, "1", LEASE).get();

        assertFalse(stranger.release(claim, ClusterLock.STATUS_SKIPPED, null));
        assertEquals(entryManager.find(ClusterLock.class, claim.getDn()).getStatus(), ClusterLock.STATUS_RUNNING);

        Instant later = clock.advance(Duration.ofSeconds(30));
        assertTrue(owner.release(claim, ClusterLock.STATUS_DONE, "{\"rows\":3}"));

        ClusterLock stored = entryManager.find(ClusterLock.class, claim.getDn());
        assertEquals(stored.getStatus(), ClusterLock.STATUS_DONE);
        assertEquals(stored.getData(), "{\"rows\":3}");
        assertEquals(stored.getLastUpdate().toInstant(), later);
        assertEquals(stored.getExpirationDate().toInstant(), later.plus(ClusterLockService.RELEASED_RETENTION));
        assertEquals(stored.getDeletable(), Boolean.TRUE);
        assertFalse(owner.isActive(stored, later));
        assertFalse(owner.heartbeat(claim));
    }

    @Test
    public void expiredSqlAcquisitionCannotChangeReplacementInSameSlot() {
        String task = newTask();
        MutableClock clock = newClock();
        FixedClusterLockService owner = new FixedClusterLockService(entryManager, "same-service", clock);
        ClusterLock old = owner.tryClaim(task, "1", LEASE).get();
        clock.advance(LEASE);
        assertFalse(owner.heartbeat(old));
        assertEquals(owner.purgeExpired(task, 1), 1);
        ClusterLock current = owner.tryClaim(task, "1", LEASE).get();
        assertFalse(old.getLockKey().equals(current.getLockKey()));
        assertFalse(owner.heartbeat(old));
        assertFalse(owner.release(old, ClusterLock.STATUS_DONE, null));
        assertFalse(owner.saveState(old, state -> "{}"));
        assertTrue(owner.heartbeat(current));
        assertTrue(owner.saveState(current, state -> "{}"));
        assertTrue(owner.release(current, ClusterLock.STATUS_DONE, null));
        assertEquals(entryManager.find(ClusterLock.class, current.getDn()).getLockKey(), current.getLockKey());
    }

    @Test
    public void stateRowRoundTrip() {
        String task = newTask();
        MutableClock clock = newClock();
        FixedClusterLockService nodeA = new FixedClusterLockService(entryManager, "state-a", clock);
        FixedClusterLockService nodeB = new FixedClusterLockService(entryManager, "state-b", clock);

        assertEquals(nodeA.loadState(task), Optional.empty());

        ClusterLock first = nodeA.tryClaim(task, "1", LEASE).get();
        assertTrue(nodeA.saveState(first, current -> "{\"HOURLY\":\"first\"}"));
        ClusterLock state = entryManager.find(ClusterLock.class, nodeA.buildStateDn(task));
        assertEquals(state.getStatus(), ClusterLock.STATUS_STATE);
        assertEquals(state.getDeletable(), Boolean.FALSE);
        assertNull(state.getExpirationDate());
        assertEquals(state.getLockKey(), first.getLockKey());

        clock.advance(Duration.ofMinutes(5));
        ClusterLock second = nodeB.tryClaim(task, "2", LEASE).get();
        assertTrue(nodeB.saveState(second, current -> "{\"HOURLY\":\"second\"}"));

        assertEquals(nodeA.loadState(task), Optional.of("{\"HOURLY\":\"second\"}"));
        state = entryManager.find(ClusterLock.class, nodeA.buildStateDn(task));
        assertEquals(state.getLockKey(), second.getLockKey());
        assertEquals(state.getDeletable(), Boolean.FALSE);
        assertNull(state.getExpirationDate());
        assertEquals(state.getLastUpdate().toInstant(), clock.instant());
        assertEquals(state.getCreationDate().toInstant(), clock.instant().minus(Duration.ofMinutes(5)));
        assertEquals(findRows(task).size(), 3);
    }

    @Test
    public void purgeRemovesOnlyExpiredRowsOfTask() {
        String task = newTask();
        String otherTask = newTask();
        MutableClock clock = newClock();
        FixedClusterLockService node = new FixedClusterLockService(entryManager, "purge-key", clock);

        assertTrue(node.tryClaim(task, "1", Duration.ofSeconds(60)).isPresent());
        assertTrue(node.tryClaim(task, "2", Duration.ofHours(1)).isPresent());
        assertTrue(node.saveState(node.find(task, "2").get(), current -> "{}"));
        assertTrue(node.tryClaim(otherTask, "1", Duration.ofSeconds(60)).isPresent());

        clock.advance(Duration.ofSeconds(120));

        assertEquals(node.purgeExpired(task, 100), 1);
        assertFalse(node.find(task, "1").isPresent());
        assertTrue(node.find(task, "2").isPresent());
        assertEquals(node.loadState(task), Optional.of("{}"));
        assertTrue(node.find(otherTask, "1").isPresent());
        assertEquals(findRows(task).size(), 2);
    }

    private String newTask() {
        String task = TASK_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        tasks.add(task);

        return task;
    }

    private static MutableClock newClock() {
        return new MutableClock(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private List<ClusterLock> findRows(String task) {
        List<ClusterLock> rows = entryManager.findEntries(BASE_DN, ClusterLock.class, Filter.createEqualityFilter("jansType", task));
        assertNotNull(rows);

        return rows;
    }

    private static Properties load(File file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
        }

        return properties;
    }

}
