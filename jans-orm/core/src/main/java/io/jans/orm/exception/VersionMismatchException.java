/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

/**
 * An exception is a result of a failed optimistic-concurrency check: the entry's stored
 * {@code @Version} value no longer matched the expected value at update time.
 *
 * @author Yuriy Movchan
 */
public class VersionMismatchException extends BasePersistenceException {

    private static final long serialVersionUID = 1L;

    private String dn;
    private Long expectedVersion;

    public VersionMismatchException(String dn, Long expectedVersion) {
        super(String.format(
                "Version mismatch updating entry '%s': expected version '%s' no longer matches "
                + "(stale read or entry no longer exists)", dn, expectedVersion));
        this.dn = dn;
        this.expectedVersion = expectedVersion;
    }

    public String getDn() {
        return dn;
    }

    public Object getExpectedVersion() {
        return expectedVersion;
    }

}
