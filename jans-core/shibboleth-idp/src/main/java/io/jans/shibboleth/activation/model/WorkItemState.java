package io.jans.shibboleth.activation.model;


public enum WorkItemState {

    PENDING,
    ASSIGNED,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {

        return this == COMPLETED || this == CANCELLED;
    }
}