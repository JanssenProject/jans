package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.IdpInstanceStatus;

import java.util.Map;

public class IdpInstances {

    private final Map<String,IdpInstanceStatus> instancesById;

    private IdpInstances(Map<String,IdpInstanceStatus> instancesById) {

        this.instancesById  = instancesById != null ? Map.copyOf(instancesById) : Map.of();
    }

    public static IdpInstances empty() {

        return new IdpInstances(null);
    }

    public boolean hasAnyRegisteredInstance() {

        return !instancesById.isEmpty();
    }
}