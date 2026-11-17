package io.jans.configapi.plugin.facade.convert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class TraceReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TraceReader() {
    }

    public static List<TraceRecord> read(Path path) throws IOException {
        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(path)) {
            files.addAll(traceFiles(path));
        } else {
            files.add(path);
        }
        List<TraceRecord> records = new ArrayList<>();
        for (Path file : files) {
            try {
                for (JsonNode value : values(file)) {
                    walk(value, records);
                }
            } catch (IllegalArgumentException e) {
                throw new IOException(file + ": " + e.getMessage(), e);
            }
        }
        return records;
    }

    public static final Set<String> CONTROL_FILES = Set.of("truth.json", "run.json", "executions.json", "authorization.json");

    public static List<Path> traceFiles(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".jsonl"))
                    .filter(p -> !CONTROL_FILES.contains(p.getFileName().toString()))
                    .sorted().toList();
        }
    }

    private static List<JsonNode> values(Path file) throws IOException {
        String text = Files.readString(file);
        List<JsonNode> values = new ArrayList<>();
        if (!file.toString().endsWith(".jsonl")) {
            values.add(MAPPER.readTree(text));
            return values;
        }
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                values.add(MAPPER.readTree(line));
            }
        }
        return values;
    }

    private static void walk(JsonNode node, List<TraceRecord> out) throws IOException {
        if (node.isArray()) {
            for (JsonNode child : node) {
                walk(child, out);
            }
        } else if (node.isObject()) {
            if (node.path("records").isArray()) {
                walk(node.get("records"), out);
            } else if (node.has("assertion") || node.has("assertion_raw")) {
                JsonNode assertion = node.path("assertion_raw").isTextual()
                        ? MAPPER.readTree(node.get("assertion_raw").asText())
                        : node.get("assertion");
                if (assertion.isTextual()) {
                    assertion = MAPPER.readTree(assertion.asText());
                }
                out.add(parse(assertion, node));
            } else if (node.has("trace") && node.has("record_id")) {
                out.add(parse(node, null));
            } else {
                throw new IllegalArgumentException("unrecognised JSON object (no assertion, assertion_raw, trace or records key)");
            }
        }
    }

    private static TraceRecord parse(JsonNode assertion, JsonNode envelope) {
        JsonNode trace = assertion.path("trace");
        JsonNode event = trace.path("event");
        String kind = required(trace, "event_kind");

        List<String> issuers = new ArrayList<>();
        for (JsonNode token : event.path("tokens")) {
            String issuer = token.path("issuer").asText("");
            if (!issuer.isEmpty() && !issuers.contains(issuer)) {
                issuers.add(issuer);
            }
        }
        issuers.sort(String::compareTo);

        List<TraceRecord.Parent> parents = new ArrayList<>();
        for (JsonNode parent : assertion.path("parent_record_ids")) {
            if (parent.hasNonNull("record_id")) {
                parents.add(new TraceRecord.Parent(parent.path("producer_id").asText(""),
                        parent.get("record_id").asText(), parent.path("relationship_type").asText("")));
            }
        }

        Long receivedAt = null;
        Boolean signatureValid = null;
        Boolean keyAuthorized = null;
        String receiptState = null;
        if (envelope != null) {
            JsonNode ingestion = envelope.path("ingestion");
            if (ingestion.hasNonNull("received_at")) {
                receivedAt = OffsetDateTime.parse(ingestion.get("received_at").asText()).toEpochSecond();
            }
            receiptState = text(ingestion, "receipt_state");
            JsonNode verification = envelope.path("verification");
            signatureValid = bool(verification, "signature_valid");
            keyAuthorized = bool(verification, "key_authorized");
        }

        JsonNode policy = trace.path("policy");
        return new TraceRecord(
                required(assertion, "record_id"),
                required(assertion, "producer"),
                kind,
                trace.path("signed_at").asLong(),
                required(trace, "trace_execution_id"),
                required(trace, "execution_authority"),
                text(trace.path("subject"), "workload_id"),
                capabilities(event, kind),
                issuers,
                text(policy, "policy_store_id"),
                text(policy, "policy_store_version"),
                text(policy, "bundle_hash"),
                parents,
                receivedAt,
                signatureValid,
                keyAuthorized,
                receiptState);
    }

    private static List<TraceRecord.Capability> capabilities(JsonNode event, String kind) {
        List<JsonNode> facts = new ArrayList<>();
        String single;
        String plural;
        if (kind.equals(TraceRecord.EVENT_RUNTIME_EFFECT)) {
            return List.of();
        } else if (kind.equals(TraceRecord.EVENT_CAPABILITY_INVOKED)) {
            single = "invocation";
            plural = "invocations";
        } else {
            single = "decision";
            plural = "decisions";
        }
        if (event.path(single).isObject()) {
            facts.add(event.get(single));
        }
        if (event.path(plural).isArray()) {
            event.get(plural).forEach(f -> {
                if (f.isObject()) {
                    facts.add(f);
                }
            });
        }
        List<TraceRecord.Capability> capabilities = new ArrayList<>();
        for (JsonNode fact : facts) {
            String action = text(fact, "action");
            String resourceType = text(fact, "resource_type");
            if (action != null && resourceType != null) {
                capabilities.add(new TraceRecord.Capability(action + "|" + resourceType, text(fact, "outcome")));
            }
        }
        return capabilities;
    }

    private static String required(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            throw new IllegalArgumentException("missing required field " + field);
        }
        return node.get(field).asText();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isEmpty() ? null : value.asText();
    }

    private static Boolean bool(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asBoolean() : null;
    }
}
