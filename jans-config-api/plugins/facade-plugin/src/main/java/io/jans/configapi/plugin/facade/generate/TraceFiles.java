/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes the files that connect the authorization step and the collection step. */
public final class TraceFiles {

    public static final String EXECUTIONS_FILE = "executions.json";
    public static final String TRUTH_FILE = "truth.json";
    public static final String RUN_FILE = "run.json";
    public static final String TRACES_DIR = "traces";

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private TraceFiles() {
    }

    /** An execution that was sent to Cedarling. {@code expectedRecords} is the number of decisions it made. */
    public record Sent(String id, String authority, String phase, int expectedRecords, List<String> workloadIds) {
    }

    /** An execution to fetch from Lock. */
    public record Ref(String id, String authority, int expectedRecords) {
    }

    /** An allowed request that was an injected anomaly. */
    public record Anomaly(String recordId, String principal, String resource, long signedAt) {
    }

    public static void writeExecutions(Path outDir, String runId, String producerId, List<Sent> sent)
            throws IOException {
        Files.createDirectories(outDir);
        ObjectNode root = MAPPER.createObjectNode();
        root.put("run_id", runId);
        root.put("producer_id", producerId);
        root.put("written_at", Instant.now().toString());
        ArrayNode list = root.putArray("executions");
        for (Sent e : sent) {
            ObjectNode node = list.addObject();
            node.put("execution_id", e.id());
            node.put("execution_authority", e.authority());
            node.put("phase", e.phase());
            node.put("expected_records", e.expectedRecords());
            ArrayNode ids = node.putArray("workload_ids");
            e.workloadIds().forEach(ids::add);
        }
        MAPPER.writeValue(outDir.resolve(EXECUTIONS_FILE).toFile(), root);
    }

    public static List<Ref> readExecutions(Path outDir) throws IOException {
        Path file = outDir.resolve(EXECUTIONS_FILE);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException(file + " not found");
        }
        List<Ref> refs = new ArrayList<>();
        for (JsonNode node : MAPPER.readTree(file.toFile()).path("executions")) {
            refs.add(new Ref(node.path("execution_id").asText(), node.path("execution_authority").asText(),
                    node.path("expected_records").asInt()));
        }
        return refs;
    }

    public static void writeTruth(Path outDir, List<Anomaly> anomalies) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode list = root.putArray("anomalies");
        for (Anomaly anomaly : anomalies) {
            list.addObject()
                    .put("record_id", anomaly.recordId())
                    .put("principal", anomaly.principal())
                    .put("resource", anomaly.resource())
                    .put("signed_at", anomaly.signedAt());
        }
        MAPPER.writeValue(outDir.resolve(TRUTH_FILE).toFile(), root);
    }

    public static void writeRun(Path outDir, Instant testStart) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("test_start", testStart.toString());
        MAPPER.writeValue(outDir.resolve(RUN_FILE).toFile(), root);
    }

    public static void writeTrace(Path outDir, String executionId, JsonNode stored) throws IOException {
        Path dir = outDir.resolve(TRACES_DIR);
        Files.createDirectories(dir);
        MAPPER.writeValue(dir.resolve(executionId + ".json").toFile(), stored);
    }
}
