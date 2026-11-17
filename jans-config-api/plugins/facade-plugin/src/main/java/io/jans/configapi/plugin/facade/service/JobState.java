/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.configapi.plugin.facade.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * What the send, collect and train jobs have done so far. It is the file {@code state.json} in the output directory,
 * so the jobs, which run on separate timers, can see each other's progress and it survives a restart.
 */
public record JobState(Stage send, Stage collect, Stage train) {

    public static final String SUCCESS = "SUCCESS";
    public static final String PARTIAL = "PARTIAL";
    public static final String FAILED = "FAILED";

    /**
     * The result of the last attempt of one job.
     *
     * @param status      {@code SUCCESS}, {@code PARTIAL} (collect got only some of the records) or {@code FAILED}
     * @param at          when the attempt finished
     * @param attempts    attempts so far for the same basis
     * @param fingerprint fingerprint of the input the attempt used, if the job reads input files
     * @param basedOn     the {@code at} of the earlier stage the attempt used, if any
     */
    public record Stage(String status, Instant at, String message, int attempts, String fingerprint,
            Instant basedOn) {

        public boolean succeeded() {
            return SUCCESS.equals(status);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static JobState read(Path outputDir) throws IOException {
        Path file = outputDir.resolve(Constants.STATE_FILE);
        if (!Files.isRegularFile(file)) {
            return new JobState(null, null, null);
        }
        JsonNode node = MAPPER.readTree(file.toFile());
        return new JobState(stage(node.path("send")), stage(node.path("collect")), stage(node.path("train")));
    }

    public void write(Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        ObjectNode node = MAPPER.createObjectNode();
        put(node, "send", send);
        put(node, "collect", collect);
        put(node, "train", train);
        MAPPER.writeValue(outputDir.resolve(Constants.STATE_FILE).toFile(), node);
    }

    public JobState withSend(Stage stage) {
        return new JobState(stage, collect, train);
    }

    public JobState withCollect(Stage stage) {
        return new JobState(send, stage, train);
    }

    public JobState withTrain(Stage stage) {
        return new JobState(send, collect, stage);
    }

    private static Stage stage(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }
        return new Stage(node.path("status").asText(), Instant.parse(node.path("at").asText()),
                node.path("message").asText(""), node.path("attempts").asInt(0),
                node.path("input_fingerprint").asText(null),
                node.hasNonNull("based_on") ? Instant.parse(node.get("based_on").asText()) : null);
    }

    private static void put(ObjectNode root, String name, Stage stage) {
        if (stage == null) {
            return;
        }
        ObjectNode node = root.putObject(name);
        node.put("status", stage.status());
        node.put("at", stage.at().toString());
        node.put("message", stage.message());
        node.put("attempts", stage.attempts());
        if (stage.fingerprint() != null) {
            node.put("input_fingerprint", stage.fingerprint());
        }
        if (stage.basedOn() != null) {
            node.put("based_on", stage.basedOn().toString());
        }
    }
}
