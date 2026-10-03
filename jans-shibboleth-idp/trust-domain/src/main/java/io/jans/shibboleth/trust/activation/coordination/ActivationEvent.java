package io.jans.shibboleth.trust.activation.coordination;

public sealed interface ActivationEvent permits
    WorkItemAssigned,
    WorkItemLeaseExpired {
}
