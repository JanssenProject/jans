package io.jans.shibboleth.trust.activation.error;

/**
 * A lease already exists for a work item at the generation a worker tried to acquire — the claim was lost
 * to another worker. This is the domain-side reading of a duplicate-identity create in the store: because a
 * lease's identity is derived from {@code (workItemId, generation)}, two workers racing for the same
 * generation collide and exactly one wins; the loser sees this.
 */
public class LeaseAlreadyHeld extends ActivationError {

    private LeaseAlreadyHeld(String message) {

        super(message);
    }

    public static LeaseAlreadyHeld instance() {

        return new LeaseAlreadyHeld(
            "A lease already exists for this work item and generation; the claim was lost to another worker");
    }
}
