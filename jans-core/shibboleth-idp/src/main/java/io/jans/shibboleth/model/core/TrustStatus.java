package io.jans.shibboleth.model.core;


public enum TrustStatus {

    DRAFT, 
    READY,
    ACTIVATING,
    ACTIVE,
    INACTIVE;


    public boolean isActivating() {

        return this == ACTIVATING; 
    }
}