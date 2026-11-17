package io.jans.configapi.plugin.facade.convert;

import io.jans.configapi.plugin.facade.convert.FacadeRows.Action;
import io.jans.configapi.plugin.facade.convert.FacadeRows.Context;
import io.jans.configapi.plugin.facade.convert.FacadeRows.Peer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class FacadeMapper {

    public static final String ACTION_TYPE = "capability_use";
    public static final String CONTEXT_TYPE = "trace_peer";

    private static final Set<String> PRINCIPAL_PARENT_RELATIONSHIPS = Set.of("authorized_by", "authorized", "produced_effect");

    public record Options(List<String> actionKinds, List<String> outcomes, List<String> peerAttrs,
            boolean useReceivedAt) {
        public static Options defaults() {
            return new Options(List.of(TraceRecord.EVENT_AUTHORIZATION_DECISION), List.of("ALLOW", "SUCCESS"),
                    List.of("execution"), false);
        }
    }

    public record Stats(int recordsIn, int droppedUnverified, int droppedVoid, int droppedDuplicate,
            int droppedNoPrincipal, int skippedKind, int skippedOutcome, int actions, int contexts) {
    }

    public record Result(List<Action> actions, List<Context> contexts, Stats stats) {
    }

    private FacadeMapper() {
    }

    public static Result convert(List<TraceRecord> traces, Options options) {
        int recordsIn = 0;
        int unverified = 0;
        int voided = 0;
        int duplicates = 0;
        int noPrincipal = 0;
        int skippedKind = 0;
        int skippedOutcome = 0;

        Set<String> seen = new HashSet<>();
        List<TraceRecord> kept = new ArrayList<>();
        for (TraceRecord t : traces) {
            recordsIn++;
            if (Boolean.FALSE.equals(t.signatureValid()) || Boolean.FALSE.equals(t.keyAuthorized())) {
                unverified++;
                continue;
            }
            if ("VOID".equals(t.receiptState())) {
                voided++;
                continue;
            }
            if (!seen.add(t.identity())) {
                duplicates++;
                continue;
            }
            kept.add(t);
        }

        Map<String, String> principals = resolvePrincipals(kept);
        List<TraceRecord> ordered = new ArrayList<>(kept);
        ordered.sort(Comparator.comparingLong((TraceRecord t) -> when(t, options)).thenComparing(TraceRecord::recordId));

        List<Action> actions = new ArrayList<>();
        Map<String, Long> firstSeen = new LinkedHashMap<>();
        Map<String, String[]> keyParts = new HashMap<>();
        Map<String, Map<String, Set<String>>> extras = new HashMap<>();

        for (TraceRecord t : ordered) {
            String principal = principals.get(t.identity());
            if (principal == null) {
                noPrincipal++;
                continue;
            }
            long when = when(t, options);
            String key = principal + "\u0000" + t.execKey();
            keyParts.put(key, new String[] { principal, t.execKey() });
            firstSeen.merge(key, when, Math::min);
            Map<String, Set<String>> extra = extras.computeIfAbsent(key, k -> newExtra());
            if (t.policyStoreId() != null) {
                extra.get("policy_store").add(t.policyStoreId());
                if (t.policyStoreVersion() != null) {
                    extra.get("policy_release").add(t.policyStoreId() + "@" + t.policyStoreVersion());
                }
            } else if (t.policyStoreVersion() != null) {
                extra.get("policy_release").add(t.policyStoreVersion());
            }
            if (t.policyBundleHash() != null) {
                extra.get("bundle_hash").add(t.policyBundleHash());
            }
            extra.get("issuer").addAll(t.tokenIssuers());

            if (!options.actionKinds().contains(t.eventKind())) {
                skippedKind++;
                continue;
            }
            for (int i = 0; i < t.capabilities().size(); i++) {
                TraceRecord.Capability capability = t.capabilities().get(i);
                if (!options.outcomes().isEmpty() && !options.outcomes().contains(capability.outcome())) {
                    skippedOutcome++;
                    continue;
                }
                actions.add(new Action(FacadeMapper.ACTION_TYPE, t.producerId() + "/" + t.recordId() + ":" + i,
                        capability.key(), principal, when, capability.key()));
            }
        }

        List<Context> contexts = new ArrayList<>();
        List<Map.Entry<String, Long>> entries = new ArrayList<>(firstSeen.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                .thenComparing(e -> keyParts.get(e.getKey())[0])
                .thenComparing(e -> keyParts.get(e.getKey())[1]));
        for (Map.Entry<String, Long> entry : entries) {
            String[] parts = keyParts.get(entry.getKey());
            List<Peer> peers = new ArrayList<>();
            if (options.peerAttrs().contains("execution")) {
                peers.add(new Peer("execution", parts[1], "UNSET", 1.0));
            }
            Map<String, Set<String>> extra = extras.get(entry.getKey());
            for (String attr : List.of("policy_store", "policy_release", "bundle_hash", "issuer")) {
                if (options.peerAttrs().contains(attr)) {
                    for (String value : extra.get(attr)) {
                        peers.add(new Peer(attr, value, "UNSET", 1.0));
                    }
                }
            }
            if (!peers.isEmpty()) {
                contexts.add(new Context(CONTEXT_TYPE, parts[0], entry.getValue(), peers));
            }
        }

        Stats stats = new Stats(recordsIn, unverified, voided, duplicates, noPrincipal, skippedKind, skippedOutcome,
                actions.size(), contexts.size());
        return new Result(actions, contexts, stats);
    }

    private static Map<String, Set<String>> newExtra() {
        Map<String, Set<String>> extra = new HashMap<>();
        for (String name : List.of("policy_store", "policy_release", "bundle_hash", "issuer")) {
            extra.put(name, new TreeSet<>());
        }
        return extra;
    }

    private static long when(TraceRecord t, Options options) {
        return options.useReceivedAt() && t.receivedAt() != null ? t.receivedAt() : t.signedAt();
    }

    private static Map<String, String> resolvePrincipals(List<TraceRecord> traces) {
        Map<String, TraceRecord> byIdentity = new HashMap<>();
        Map<String, Integer> idCounts = new HashMap<>();
        for (TraceRecord t : traces) {
            byIdentity.put(t.identity(), t);
            idCounts.merge(t.recordId(), 1, Integer::sum);
        }
        Map<String, TraceRecord> byRecordId = new HashMap<>();
        for (TraceRecord t : traces) {
            if (idCounts.get(t.recordId()) == 1) {
                byRecordId.put(t.recordId(), t);
            }
        }
        Map<String, String> cache = new HashMap<>();
        Map<String, String> out = new HashMap<>();
        for (TraceRecord t : traces) {
            String principal = resolve(t, new HashSet<>(), cache, byIdentity, byRecordId);
            if (principal != null) {
                out.put(t.identity(), principal);
            }
        }
        return out;
    }

    private static String resolve(TraceRecord t, Set<String> seen, Map<String, String> cache,
            Map<String, TraceRecord> byIdentity, Map<String, TraceRecord> byRecordId) {
        String cached = cache.get(t.identity());
        if (cached != null) {
            return cached;
        }
        if (t.workloadId() != null) {
            cache.put(t.identity(), t.workloadId());
            return t.workloadId();
        }
        Set<String> next = new HashSet<>(seen);
        next.add(t.identity());
        for (TraceRecord.Parent parent : t.parents()) {
            if (!PRINCIPAL_PARENT_RELATIONSHIPS.contains(parent.relationship())) {
                continue;
            }
            TraceRecord found = parent.producerId().isEmpty() ? null
                    : byIdentity.get(parent.producerId() + "\u0000" + parent.recordId());
            if (found == null) {
                found = byRecordId.get(parent.recordId());
            }
            if (found != null && !next.contains(found.identity())) {
                String got = resolve(found, next, cache, byIdentity, byRecordId);
                if (got != null) {
                    cache.put(t.identity(), got);
                    return got;
                }
            }
        }
        if (t.execAuthority() != null && !t.execAuthority().isEmpty()) {
            cache.put(t.identity(), t.execAuthority());
            return t.execAuthority();
        }
        return null;
    }
}
