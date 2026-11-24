/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.cluster;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.model.cluster.ClusterLock;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.search.filter.Filter;
import jakarta.inject.Inject;

/**
 * Cluster claim primitive based only on insert-or-fail.
 *
 * <p>A node claims one run slot of a task by inserting an entry with a DN built from the task and the
 * slot key. The insert of an existing DN fails on the backends that support claims, so exactly one
 * node holds each slot. A claim never changes its owner: a later slot uses a new DN. Because of that,
 * read-compare-merge in {@link #heartbeat(ClusterLock)} and {@link #release(ClusterLock, String, String)}
 * is safe without versioned updates or transactions.</p>
 *
 * <p>Only backends whose insert fails on an existing DN can hold claims: SQL and LDAP, also behind a
 * hybrid entry manager. Spanner and Couchbase write an insert as an upsert, so {@link #tryClaim} refuses
 * to claim there and logs a warning.</p>
 *
 * <p>Each task also has one state row ({@code jansId=<task>_state}) for data that must survive between
 * runs. It has no expiration date and {@code del=false}, so neither {@link #purgeExpired} nor an
 * external cleaner removes it.</p>
 *
 * <p>Claim lifetime: while a claim is running, its {@code exp} is the last heartbeat plus the lease, so
 * a claim whose owner died becomes inactive and expires after the lease. A released claim keeps its row
 * for {@link #RELEASED_RETENTION} and then expires.</p>
 */
public abstract class ClusterLockService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterLockService.class);

    /** Maximum length of {@code jansId}: on SQL it is the first RDN value and the {@code doc_id} column. */
    public static final int MAX_ID_LENGTH = 64;

    /** Maximum length of the DN: the SQL {@code dn} column. */
    public static final int MAX_DN_LENGTH = 128;

    /** Maximum length of {@code jansStatus}: the SQL column. */
    public static final int MAX_STATUS_LENGTH = 16;

    /** How long a released claim is kept before it expires. */
    public static final Duration RELEASED_RETENTION = Duration.ofDays(1);

    /** Minimum time between two warnings of the same kind. */
    public static final Duration WARNING_INTERVAL = Duration.ofHours(1);

    public static final String STATE_SLOT_KEY = "state";

    private static final String ID_SEPARATOR = "_";

    private static final Pattern TASK_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern SLOT_KEY_PATTERN = Pattern.compile("[A-Za-z0-9.-]+");

    private static final Set<String> INSERT_OR_FAIL_PERSISTENCE_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            PersistenceEntryManager.PERSITENCE_TYPES.sql.name(), PersistenceEntryManager.PERSITENCE_TYPES.ldap.name())));

    static final String WARNING_PERSIST_FAILURE = "persist-failure:";
    static final String WARNING_UNSUPPORTED_BACKEND = "unsupported-backend:";

    @Inject
    private PersistenceEntryManager entryManager;

    private volatile Clock clock = Clock.systemUTC();

    private final ConcurrentMap<String, Instant> lastWarnings = new ConcurrentHashMap<>();

    /**
     * Tries to claim one run slot of a task.
     *
     * @param task task name; letters, digits, {@code .}, {@code -} and {@code _}
     * @param slotKey slot key; letters, digits, {@code .} and {@code -}; not {@value #STATE_SLOT_KEY}
     * @param lease how long the claim stays active without a heartbeat
     * @return the claim when this node won the slot; empty when another node holds it, when the backend
     *         cannot signal a duplicate insert, or when the insert failed for another reason
     * @throws IllegalArgumentException the task, slot key or lease is invalid, or the DN is too long
     */
    public Optional<ClusterLock> tryClaim(String task, String slotKey, Duration lease) {
        String dn = buildClaimDn(task, slotKey);
        if ((lease == null) || lease.isNegative() || lease.isZero()) {
            throw new IllegalArgumentException("Lease must be positive");
        }

        if (!isClaimSupported()) {
            return Optional.empty();
        }

        Instant now = clock.instant();
        ClusterLock claim = new ClusterLock();
        claim.setDn(dn);
        claim.setId(buildId(task, slotKey));
        claim.setTask(task);
        claim.setLockKey(getLockKey());
        claim.setNodeIdentifier(getNodeIdentifier());
        claim.setClusterNodeId(getClusterNodeId());
        claim.setCreationDate(Date.from(now));
        claim.setLastUpdate(Date.from(now));
        claim.setStatus(ClusterLock.STATUS_RUNNING);
        claim.setDeletable(Boolean.TRUE);
        setExpiration(claim, now, lease);

        try {
            entryManager().persist(claim);
        } catch (RuntimeException ex) {
            if (isDuplicate(ex)) {
                LOG.debug("Slot '{}' of task '{}' is already claimed", slotKey, task);
                return Optional.empty();
            }

            Throwable rootCause = getRootCause(ex);
            if (shouldWarn(WARNING_PERSIST_FAILURE + rootCause.getClass().getName())) {
                LOG.warn("Failed to insert cluster claim '{}'; tasks that need a claim stay idle until the insert works "
                        + "(further failures of this kind are logged at DEBUG for {}). Cause: {}: {}", dn, WARNING_INTERVAL,
                        rootCause.getClass().getName(), rootCause.getMessage());
            }
            LOG.debug("Failed to insert cluster claim '{}'", dn, ex);
            return Optional.empty();
        }

        lastWarnings.keySet().removeIf(key -> key.startsWith(WARNING_PERSIST_FAILURE));
        LOG.info("Claimed slot '{}' of task '{}' (node '{}', lease {})", slotKey, task, claim.getNodeIdentifier(), lease);

        return Optional.of(claim);
    }

    /**
     * Renews a claim held by this node: sets {@code jansLastUpd} to now and moves {@code exp} by the
     * lease that the claim was created with. On success the given object is updated too.
     *
     * @return {@code false} when the claim is missing, belongs to another node, is already released, or
     *         the update failed; the caller must then stop the work the claim protects
     */
    public boolean heartbeat(ClusterLock claim) {
        requireClaim(claim);

        ClusterLock stored;
        try {
            stored = readEntry(claim.getDn()).orElse(null);
        } catch (RuntimeException ex) {
            LOG.info("Lost claim '{}': failed to read it", claim.getDn());
            LOG.debug("Failed to read claim '{}'", claim.getDn(), ex);
            return false;
        }

        if (stored == null) {
            LOG.info("Lost claim '{}': the entry is missing", claim.getDn());
            return false;
        }
        if (!StringUtils.equals(getLockKey(), stored.getLockKey())) {
            LOG.info("Lost claim '{}': it belongs to node '{}'", claim.getDn(), stored.getNodeIdentifier());
            return false;
        }
        if (!ClusterLock.STATUS_RUNNING.equals(stored.getStatus())) {
            LOG.debug("Claim '{}' is already released with status '{}'", claim.getDn(), stored.getStatus());
            return false;
        }

        Duration lease = getLease(claim);
        if (lease == null) {
            lease = getLease(stored);
        }
        if (lease == null) {
            LOG.debug("Claim '{}' has no lease information", claim.getDn());
            return false;
        }

        Instant now = clock.instant();
        stored.setLastUpdate(Date.from(now));
        setExpiration(stored, now, lease);
        try {
            entryManager().merge(stored);
        } catch (RuntimeException ex) {
            LOG.info("Lost claim '{}': failed to update it", claim.getDn());
            LOG.debug("Failed to update claim '{}'", claim.getDn(), ex);
            return false;
        }

        claim.setLastUpdate(stored.getLastUpdate());
        claim.setExpirationDate(stored.getExpirationDate());
        claim.setTtl(stored.getTtl());
        LOG.debug("Renewed claim '{}' until {}", claim.getDn(), stored.getExpirationDate());

        return true;
    }

    /**
     * Releases a claim held by this node. The row is kept for {@link #RELEASED_RETENTION}.
     *
     * @param status final status, at most {@value #MAX_STATUS_LENGTH} characters, for example
     *        {@link ClusterLock#STATUS_DONE} or {@link ClusterLock#STATUS_SKIPPED}
     * @param summaryJson summary stored in {@code jansData}; may be {@code null}
     * @return {@code true} when the claim was released; {@code false} when it is missing, belongs to
     *         another node, or the update failed
     * @throws IllegalArgumentException the status is blank, too long or {@link ClusterLock#STATUS_RUNNING}
     */
    public boolean release(ClusterLock claim, String status, String summaryJson) {
        requireClaim(claim);
        validateStatus(status);
        if (ClusterLock.STATUS_RUNNING.equals(status)) {
            throw new IllegalArgumentException("A claim cannot be released with status '" + status + "'");
        }

        try {
            ClusterLock stored = readEntry(claim.getDn()).orElse(null);
            if (stored == null) {
                LOG.debug("Claim '{}' to release is missing", claim.getDn());
                return false;
            }
            if (!StringUtils.equals(getLockKey(), stored.getLockKey())) {
                LOG.debug("Claim '{}' to release belongs to node '{}'", claim.getDn(), stored.getNodeIdentifier());
                return false;
            }

            Instant now = clock.instant();
            stored.setStatus(status);
            stored.setData(summaryJson);
            stored.setLastUpdate(Date.from(now));
            setExpiration(stored, now, RELEASED_RETENTION);
            entryManager().merge(stored);

            claim.setStatus(stored.getStatus());
            claim.setData(stored.getData());
            claim.setLastUpdate(stored.getLastUpdate());
            claim.setExpirationDate(stored.getExpirationDate());
            claim.setTtl(stored.getTtl());
        } catch (RuntimeException ex) {
            LOG.debug("Failed to release claim '{}'", claim.getDn(), ex);
            return false;
        }

        LOG.debug("Released claim '{}' with status '{}'", claim.getDn(), status);
        return true;
    }

    /**
     * @return the claim of the given slot; empty when it does not exist or cannot be read
     */
    public Optional<ClusterLock> find(String task, String slotKey) {
        String dn = buildClaimDn(task, slotKey);
        try {
            return readEntry(dn);
        } catch (RuntimeException ex) {
            LOG.debug("Failed to read claim '{}'", dn, ex);
            return Optional.empty();
        }
    }

    /**
     * @return {@code true} when the claim is not released and its last heartbeat is within its lease
     */
    public boolean isActive(ClusterLock claim, Instant now) {
        if ((claim == null) || (now == null) || (claim.getExpirationDate() == null)) {
            return false;
        }

        return ClusterLock.STATUS_RUNNING.equals(claim.getStatus()) && claim.getExpirationDate().toInstant().isAfter(now);
    }

    /**
     * Removes claims of the task whose {@code exp} is not after now. The state row has no
     * {@code exp}, so it is never removed.
     *
     * @return the number of removed entries; 0 when the removal failed
     */
    public int purgeExpired(String task, int limit) {
        validateTask(task);
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        String baseDn = getBaseDn();
        try {
            Date now = Date.from(clock.instant());
            Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("jansType", task),
                    Filter.createEqualityFilter("del", true),
                    Filter.createLessOrEqualFilter("exp", entryManager().encodeTime(baseDn, now)));

            int removed = entryManager().remove(baseDn, ClusterLock.class, filter, limit);
            LOG.debug("Removed {} expired claims of task '{}'", removed, task);

            return removed;
        } catch (RuntimeException ex) {
            LOG.debug("Failed to remove expired claims of task '{}'", task, ex);
            return 0;
        }
    }

    /**
     * @return the {@code jansData} of the task's state row; empty when the row does not exist or has no
     *         data
     * @throws EntryPersistenceException the row cannot be read for a reason other than its absence
     */
    public Optional<String> loadState(String task) {
        String dn = buildStateDn(task);
        Optional<ClusterLock> state = readEntry(dn);

        return state.map(ClusterLock::getData);
    }

    /**
     * Writes the {@code jansData} of the task's state row, and creates the row on first use. Only the
     * holder of the task's current claim should call it.
     *
     * @return {@code true} when the data was written
     */
    public boolean saveState(String task, String json) {
        String dn = buildStateDn(task);
        Date now = Date.from(clock.instant());

        try {
            Optional<ClusterLock> existing = readEntry(dn);
            if (existing.isPresent()) {
                updateState(existing.get(), json, now);
                return true;
            }

            ClusterLock state = new ClusterLock();
            state.setDn(dn);
            state.setId(buildId(task, STATE_SLOT_KEY));
            state.setTask(task);
            state.setStatus(ClusterLock.STATUS_STATE);
            state.setDeletable(Boolean.FALSE);
            state.setCreationDate(now);
            state.setLastUpdate(now);
            state.setLockKey(getLockKey());
            state.setNodeIdentifier(getNodeIdentifier());
            state.setClusterNodeId(getClusterNodeId());
            state.setData(json);
            try {
                entryManager().persist(state);
            } catch (RuntimeException ex) {
                if (!isDuplicate(ex)) {
                    throw ex;
                }
                // Another node created the row between the read and the insert
                updateState(readEntry(dn).orElseThrow(() -> ex), json, now);
            }

            return true;
        } catch (RuntimeException ex) {
            LOG.debug("Failed to save state of task '{}'", task, ex);
            return false;
        }
    }

    /**
     * @return {@code true} when the backend that stores the claims fails an insert of an existing DN;
     *         otherwise logs a warning (at most once per {@link #WARNING_INTERVAL}) and returns
     *         {@code false}
     */
    public boolean isClaimSupported() {
        String persistenceType;
        try {
            persistenceType = entryManager().getPersistenceType(getBaseDn());
        } catch (RuntimeException ex) {
            LOG.debug("Failed to determine the persistence type of '{}'", getBaseDn(), ex);
            return false;
        }

        if (INSERT_OR_FAIL_PERSISTENCE_TYPES.contains(persistenceType)) {
            return true;
        }

        if (shouldWarn(WARNING_UNSUPPORTED_BACKEND + persistenceType)) {
            LOG.warn("Cluster claims are not supported on persistence type '{}': its insert does not fail on an existing entry. "
                    + "Tasks that need a claim stay idle", persistenceType);
        }

        return false;
    }

    /**
     * @return the DN of a claim: {@code jansId=<task>_<slotKey>,<baseDn>}
     * @throws IllegalArgumentException the task or slot key is invalid, or the DN is too long
     */
    public String buildClaimDn(String task, String slotKey) {
        validateTask(task);
        validateSlotKey(slotKey);
        if (STATE_SLOT_KEY.equals(slotKey)) {
            throw new IllegalArgumentException("Slot key '" + STATE_SLOT_KEY + "' is reserved for the state row");
        }

        return buildDn(buildId(task, slotKey));
    }

    /**
     * @return the DN of the task's state row: {@code jansId=<task>_state,<baseDn>}
     * @throws IllegalArgumentException the task is invalid, or the DN is too long
     */
    public String buildStateDn(String task) {
        validateTask(task);

        return buildDn(buildId(task, STATE_SLOT_KEY));
    }

    /**
     * @return {@code true} when the exception or one of its causes is a {@link DuplicateEntryException}
     */
    public static boolean isDuplicate(Throwable ex) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = ex; (current != null) && seen.add(current); current = current.getCause()) {
            if (current instanceof DuplicateEntryException) {
                return true;
            }
        }

        return false;
    }

    public Clock getClock() {
        return clock;
    }

    /**
     * Replaces the clock, for tests.
     */
    public void setClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock is null");
        }
        this.clock = clock;
    }

    /**
     * @return the entry manager; subclasses and tests may override it
     */
    protected PersistenceEntryManager getEntryManager() {
        return entryManager;
    }

    /**
     * @return the cluster node id stored in {@code jansNum}, or {@code null} when it is not known
     */
    protected Integer getClusterNodeId() {
        return null;
    }

    /**
     * @return the base DN of the claims, for example {@code ou=node,o=jans}
     */
    public abstract String getBaseDn();

    /**
     * @return a key unique to this service instance; it identifies the owner of a claim
     */
    public abstract String getLockKey();

    /**
     * @return a readable identifier of this node, stored in {@code jansHost}
     */
    public abstract String getNodeIdentifier();

    private PersistenceEntryManager entryManager() {
        PersistenceEntryManager result = getEntryManager();
        if (result == null) {
            throw new IllegalStateException("Persistence entry manager is not set");
        }

        return result;
    }

    private Optional<ClusterLock> readEntry(String dn) {
        try {
            return Optional.ofNullable(entryManager().find(ClusterLock.class, dn));
        } catch (EntryPersistenceException ex) {
            if (isMissingEntry(ex)) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    private void updateState(ClusterLock state, String json, Date now) {
        state.setData(json);
        state.setLastUpdate(now);
        state.setLockKey(getLockKey());
        state.setNodeIdentifier(getNodeIdentifier());
        state.setClusterNodeId(getClusterNodeId());
        state.setDeletable(Boolean.FALSE);
        state.setExpirationDate(null);
        state.setTtl(null);
        entryManager().merge(state);
    }

    /**
     * SQL and LDAP report a missing entry as {@link EntryPersistenceException} caused by a
     * {@link SearchException} that has no cause of its own. A failed query has a deeper cause.
     */
    static boolean isMissingEntry(EntryPersistenceException ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return true;
        }

        return (cause instanceof SearchException) && (cause.getCause() == null);
    }

    private static Throwable getRootCause(Throwable ex) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable result = ex;
        while ((result.getCause() != null) && seen.add(result)) {
            result = result.getCause();
        }

        return result;
    }

    boolean shouldWarn(String key) {
        Instant now = clock.instant();
        boolean[] warn = new boolean[1];
        lastWarnings.compute(key, (k, last) -> {
            if ((last == null) || !now.isBefore(last.plus(WARNING_INTERVAL))) {
                warn[0] = true;
                return now;
            }
            return last;
        });

        return warn[0];
    }

    private static void setExpiration(ClusterLock claim, Instant from, Duration lifetime) {
        claim.setExpirationDate(Date.from(from.plus(lifetime)));
        claim.setTtl((int) Math.max(1L, Math.min(Integer.MAX_VALUE, lifetime.getSeconds())));
    }

    private static Duration getLease(ClusterLock claim) {
        if ((claim.getLastUpdate() == null) || (claim.getExpirationDate() == null)) {
            return null;
        }

        Duration lease = Duration.between(claim.getLastUpdate().toInstant(), claim.getExpirationDate().toInstant());
        if (lease.isNegative() || lease.isZero()) {
            return null;
        }

        return lease;
    }

    private static void requireClaim(ClusterLock claim) {
        if ((claim == null) || StringUtils.isBlank(claim.getDn())) {
            throw new IllegalArgumentException("Claim or its DN is null");
        }
    }

    private static String buildId(String task, String slotKey) {
        return task + ID_SEPARATOR + slotKey;
    }

    private String buildDn(String id) {
        if (id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(String.format("Claim id '%s' is longer than %d characters", id, MAX_ID_LENGTH));
        }

        String baseDn = getBaseDn();
        if (StringUtils.isBlank(baseDn)) {
            throw new IllegalStateException("Base DN of cluster claims is not configured");
        }

        String dn = String.format("jansId=%s,%s", id, baseDn);
        if (dn.length() > MAX_DN_LENGTH) {
            throw new IllegalArgumentException(String.format("Claim DN '%s' is longer than %d characters", dn, MAX_DN_LENGTH));
        }

        return dn;
    }

    private static void validateTask(String task) {
        if ((task == null) || !TASK_PATTERN.matcher(task).matches()) {
            throw new IllegalArgumentException("Task name '" + task + "' is empty or has characters other than [A-Za-z0-9._-]");
        }
    }

    private static void validateSlotKey(String slotKey) {
        if ((slotKey == null) || !SLOT_KEY_PATTERN.matcher(slotKey).matches()) {
            throw new IllegalArgumentException("Slot key '" + slotKey + "' is empty or has characters other than [A-Za-z0-9.-]");
        }
    }

    private static void validateStatus(String status) {
        if (StringUtils.isBlank(status) || (status.length() > MAX_STATUS_LENGTH)) {
            throw new IllegalArgumentException(String.format("Status '%s' is blank or longer than %d characters", status, MAX_STATUS_LENGTH));
        }
    }

}
