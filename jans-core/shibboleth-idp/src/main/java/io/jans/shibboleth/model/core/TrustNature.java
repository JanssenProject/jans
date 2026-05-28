package io.jans.shibboleth.model.core;

public enum TrustNature {

    INDIVIDUAL,
    AGGREGATE;

    public boolean isIndividual() {

        return this == INDIVIDUAL; 
    }

    public boolean isAggregate() {

        return this == AGGREGATE;
    }
}