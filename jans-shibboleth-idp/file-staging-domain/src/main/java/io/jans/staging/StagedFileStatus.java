package io.jans.staging;

/**
 * Lifecycle state of a staged file. A file is {@code STAGED} on upload and becomes {@code CLAIMED}
 * once a backend takes ownership and it is moved to its durable location; there is no in-between.
 */
public enum StagedFileStatus {

    STAGED,
    CLAIMED;

    public boolean isStaged() {

        return this == STAGED;
    }

    public boolean isClaimed() {

        return this == CLAIMED;
    }
}
