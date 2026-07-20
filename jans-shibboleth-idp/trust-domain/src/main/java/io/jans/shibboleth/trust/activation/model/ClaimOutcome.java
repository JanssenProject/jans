package io.jans.shibboleth.trust.activation.model;

/**
 * The result of a claim-next poll: either the work item that was claimed, or nothing when no item of
 * the requested type is available. Absence is a normal outcome (an empty queue), not a failure — hence
 * a value type rather than a failed {@code Result}. Mirrors the null-object style of {@code Lease.NONE}.
 */
public final class ClaimOutcome {

    private static final ClaimOutcome NONE = new ClaimOutcome(null);

    private final WorkItem claimed;

    private ClaimOutcome(WorkItem claimed) {

        this.claimed = claimed;
    }

    public static ClaimOutcome of(WorkItem claimed) {

        if (claimed == null) {

            return NONE;
        }

        return new ClaimOutcome(claimed);
    }

    public static ClaimOutcome none() {

        return NONE;
    }

    public boolean isClaimed() {

        return claimed != null;
    }

    public boolean isEmpty() {

        return claimed == null;
    }

    public WorkItem workItem() {

        if (claimed == null) {

            throw new IllegalStateException("no work item was claimed; check isClaimed() first");
        }

        return claimed;
    }
}
