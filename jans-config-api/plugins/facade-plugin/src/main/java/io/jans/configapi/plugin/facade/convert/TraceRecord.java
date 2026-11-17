package io.jans.configapi.plugin.facade.convert;

import java.util.List;

public record TraceRecord(
        String recordId,
        String producerId,
        String eventKind,
        long signedAt,
        String execId,
        String execAuthority,
        String workloadId,
        List<Capability> capabilities,
        List<String> tokenIssuers,
        String policyStoreId,
        String policyStoreVersion,
        String policyBundleHash,
        List<Parent> parents,
        Long receivedAt,
        Boolean signatureValid,
        Boolean keyAuthorized,
        String receiptState) {

    public static final String EVENT_AUTHORIZATION_DECISION = "AUTHORIZATION_DECISION";
    public static final String EVENT_CAPABILITY_INVOKED = "CAPABILITY_INVOKED";
    public static final String EVENT_RUNTIME_EFFECT = "RUNTIME_EFFECT";

    public record Capability(String key, String outcome) {
    }

    public record Parent(String producerId, String recordId, String relationship) {
    }

    public String identity() {
        return producerId + "\u0000" + recordId;
    }

    public String execKey() {
        return execAuthority + "|" + execId;
    }
}
