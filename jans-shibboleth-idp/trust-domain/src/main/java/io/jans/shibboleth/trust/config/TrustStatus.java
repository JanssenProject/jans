package io.jans.shibboleth.trust.config;


public enum TrustStatus {

    DRAFT, 
    READY,
    ACTIVATING,
    ACTIVE,
    INACTIVE;

    public boolean isDraft() {

        return this == DRAFT;
    }

    public boolean isReady() {

        return this == READY;
    }

    public boolean isActivating() {

        return this == ACTIVATING; 
    }

    public boolean isActive() {

        return this == ACTIVE;
    }

    public boolean isInactive() {

        return this == INACTIVE;
    }
}