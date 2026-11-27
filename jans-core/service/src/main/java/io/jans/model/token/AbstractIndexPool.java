package io.jans.model.token;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.model.base.BaseEntry;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yuriy Z
 */
public class AbstractIndexPool extends BaseEntry {

    public static final String JANS_NUM_ATTRIBUTE_NAME = "jansNum";

    @AttributeName(ignoreDuringUpdate = true, name = "jansNum")
    private Integer id;

    @AttributeName(name = "jansNodeId")
    private Integer nodeId;

    @AttributeName(name = "dat")
    private String data;

    @AttributeName(name = "jansLastUpd")
    private Date lastUpdate;

    @AttributeName(name = "exp")
    private Date expirationDate;

    @AttributeName(name = "lockKey")
    private String lockKey;

    @Transient
    private transient Integer startIndex;

    @Transient
    private transient Integer endIndex;

    @Transient
    private transient AtomicInteger currentIndex = new AtomicInteger(-1);

    @Transient
    private transient Date issueUntil;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    /**
     * In-memory deadline after which the owning node stops issuing indexes from this pool. Persisted
     * expiration date ("exp") is the reuse boundary instead: it covers the latest expiration of any token
     * that holds an index from this pool, so it can be later than this deadline.
     *
     * @return issue deadline or null if not set (then expiration date is used)
     */
    public Date getIssueUntil() {
        return issueUntil;
    }

    public void setIssueUntil(Date issueUntil) {
        this.issueUntil = issueUntil;
    }

    public boolean containsIndex(int index) {
        return startIndex != null && endIndex != null && index >= startIndex && index <= endIndex;
    }

    public List<Integer> enumerateAllIndexes() {
        List<Integer> indexes = new ArrayList<>();
        for (int i = getStartIndex(); i <= getEndIndex(); i++ ) {
            indexes.add(i);
        }
        return indexes;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
        this.currentIndex.set(startIndex);
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(Integer endIndex) {
        this.endIndex = endIndex;
    }

    public int nextIndex() {
        // This block not used for while and were expired already
        final Date issueDeadline = issueUntil != null ? issueUntil : expirationDate;
        if ((issueDeadline == null) || issueDeadline.before(new Date())) {
            return -1;
        }

        int nextIndex = currentIndex.getAndIncrement();
        if (nextIndex > endIndex) {
            // Index out of pool range
            return -1;
        }

        // Correct index
        return nextIndex;

    }
}
