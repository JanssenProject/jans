/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.jans.configapi.plugin.facade.generate.TraceFiles;
import io.jans.configapi.plugin.facade.service.FacadeJobService.Status;
import io.jans.configapi.plugin.facade.service.JobState.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FacadeJobServiceTest {

    private static final long BASE = 1_780_000_000L;
    private static final int EXECUTIONS = 60;
    private static final int PER_EXECUTION = 5;

    private Path root;
    private Path input;
    private Path output;
    private HttpServer lock;
    /** Records Lock has for exec-0 only, so that a short execution costs one poll, not sixty. */
    private final AtomicInteger recordsPerExecution = new AtomicInteger(PER_EXECUTION);
    private final FacadeJobService service = new FacadeJobService();

    @BeforeMethod
    public void setUp() throws IOException {
        root = Files.createTempDirectory("facade-plugin-test");
        input = root.resolve("input");
        output = root.resolve("output");
        recordsPerExecution.set(PER_EXECUTION);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (lock != null) {
            lock.stop(0);
            lock = null;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    // ---- train job with Lock trace files in the input directory

    @Test
    public void missingInputDirectoryIsNotAnError() {
        assertEquals(service.train(settings()).status(), Status.NO_INPUT);
        assertEquals(service.send(settings()).status(), Status.NO_INPUT);
        assertEquals(service.collect(settings()).status(), Status.NO_INPUT);
    }

    @Test
    public void controlFilesAloneProduceNoOutput() throws IOException {
        Files.createDirectories(input);
        Files.writeString(input.resolve("truth.json"), "{\"anomalies\":[]}");

        assertEquals(service.train(settings()).status(), Status.NO_INPUT);
        assertTrue(Files.notExists(output.resolve("actions.jsonl")));
    }

    @Test
    public void traceFilesAreConvertedOnceAndAgainWhenTheyChange() throws IOException {
        Files.createDirectories(input);
        Files.writeString(input.resolve("traces.jsonl"), traces(300));
        Files.writeString(input.resolve("truth.json"), "{\"anomalies\":[]}");
        Files.writeString(input.resolve("run.json"), "{\"test_start\":\"" + Instant.ofEpochSecond(BASE + 1800) + "\"}");

        FacadeJobService.Result first = service.train(settings());
        assertEquals(first.status(), Status.SUCCESS, first.message());
        for (String name : List.of("actions.jsonl", "contexts.jsonl", "action.tfrecord", "context.tfrecord",
                "directive.textproto", "config.textproto", "summary.txt", "state.json")) {
            assertTrue(Files.isRegularFile(output.resolve(name)), name);
        }

        assertEquals(service.train(settings()).status(), Status.UNCHANGED);

        Files.writeString(input.resolve("more.jsonl"), traces(320));
        assertEquals(service.train(settings()).status(), Status.SUCCESS);
    }

    @Test
    public void tooLittleDataFailsThenTheJobGivesUpUntilTheInputChanges() throws IOException {
        Files.createDirectories(input);
        Files.writeString(input.resolve("traces.jsonl"), traces(1));

        for (int attempt = 1; attempt <= 3; attempt++) {
            FacadeJobService.Result result = service.train(settings());
            assertEquals(result.status(), Status.FAILED);
            assertTrue(result.message().contains("too little trace data"), result.message());
            assertEquals(JobState.read(output).train().attempts(), attempt);
        }
        FacadeJobService.Result last = service.train(settings());

        assertEquals(last.status(), Status.UNCHANGED);
        assertTrue(last.message().contains("gave up after 3 attempts"), last.message());

        Files.writeString(input.resolve("traces.jsonl"), traces(300));
        assertEquals(service.train(settings()).status(), Status.SUCCESS);
    }

    // ---- send job

    @Test
    public void sendWithoutLockSettingsFailsWithAClearMessage() throws IOException {
        Files.createDirectories(input);
        Files.writeString(input.resolve("authorization.json"), "{}");

        FacadeJobService.Result result = service.send(settings());

        assertEquals(result.status(), Status.FAILED);
        assertTrue(result.message().contains("facade.lock.base.url"), result.message());
        assertEquals(JobState.read(output).send().status(), JobState.FAILED);
        assertTrue(Files.notExists(output.resolve("executions.json")));
    }

    // ---- collect job and train job with authorization.json

    @Test
    public void collectAndTrainDoNothingBeforeRecordsAreSent() throws IOException {
        Files.createDirectories(input);
        Files.writeString(input.resolve("authorization.json"), "{}");

        assertEquals(service.collect(settings()).status(), Status.NO_INPUT);
        assertEquals(service.train(settings()).status(), Status.NO_INPUT);
    }

    @Test
    public void collectThenTrainRunAsSeparateJobs() throws IOException {
        sendWasDone();
        startFakeLock();

        assertEquals(service.train(lockSettings()).status(), Status.NO_INPUT, "train must wait for the collection");

        FacadeJobService.Result collected = service.collect(lockSettings());
        assertEquals(collected.status(), Status.SUCCESS, collected.message());
        assertTrue(collected.message().contains("0 records missing"), collected.message());
        assertTrue(Files.isRegularFile(output.resolve("traces/exec-0.json")));
        assertTrue(Files.notExists(output.resolve("actions.jsonl")), "collect must not train");
        assertEquals(service.collect(lockSettings()).status(), Status.UNCHANGED);

        FacadeJobService.Result trained = service.train(lockSettings());
        assertEquals(trained.status(), Status.SUCCESS, trained.message());
        assertTrue(trained.message().startsWith("sent 60 executions, collected traces (0 records missing)"),
                trained.message());
        assertTrue(Files.isRegularFile(output.resolve("actions.jsonl")));
        assertEquals(service.train(lockSettings()).status(), Status.UNCHANGED);

        JobState state = JobState.read(output);
        assertTrue(state.send().succeeded() && state.collect().succeeded() && state.train().succeeded());
        assertEquals(state.collect().basedOn(), state.send().at());
        assertEquals(state.train().basedOn(), state.collect().at());
    }

    @Test
    public void collectIsRetriedWhileRecordsAreMissingThenAccepted() throws IOException {
        sendWasDone();
        startFakeLock();
        recordsPerExecution.set(PER_EXECUTION - 1);

        FacadeJobService.Result first = service.collect(lockSettings());
        assertEquals(first.status(), Status.PARTIAL, first.message());
        assertTrue(first.message().contains("1 records missing"), first.message());
        assertEquals(service.train(lockSettings()).status(), Status.NO_INPUT, "train waits for a finished collection");

        recordsPerExecution.set(PER_EXECUTION);
        FacadeJobService.Result second = service.collect(lockSettings());
        assertEquals(second.status(), Status.SUCCESS, second.message());
        assertTrue(second.message().contains("0 records missing"), second.message());
        assertEquals(JobState.read(output).collect().attempts(), 2);
    }

    @Test
    public void collectAcceptsWhatLockHasAfterTheLastAttempt() throws IOException {
        sendWasDone();
        startFakeLock();
        recordsPerExecution.set(PER_EXECUTION - 1);

        assertEquals(service.collect(lockSettings()).status(), Status.PARTIAL);
        assertEquals(service.collect(lockSettings()).status(), Status.PARTIAL);
        FacadeJobService.Result last = service.collect(lockSettings());

        assertEquals(last.status(), Status.SUCCESS, last.message());
        assertTrue(last.message().contains("accepted after 3 attempts"), last.message());
        assertEquals(service.train(lockSettings()).status(), Status.SUCCESS);
    }

    @Test
    public void failedCollectionStopsAfterTheMaximumAttempts() throws IOException {
        sendWasDone();

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertEquals(service.collect(settings()).status(), Status.FAILED);
            assertEquals(JobState.read(output).collect().attempts(), attempt);
        }
        FacadeJobService.Result last = service.collect(settings());

        assertEquals(last.status(), Status.UNCHANGED);
        assertTrue(last.message().contains("gave up after 3 attempts"), last.message());
    }

    @Test
    public void aNewSendRestartsCollectionAndTraining() throws IOException {
        sendWasDone();
        startFakeLock();
        assertEquals(service.collect(lockSettings()).status(), Status.SUCCESS);
        assertEquals(service.train(lockSettings()).status(), Status.SUCCESS);

        JobState before = JobState.read(output);
        new JobState(new Stage(JobState.SUCCESS, Instant.now().plusSeconds(60), "sent again", 1, "fp-2", null),
                before.collect(), before.train()).write(output);

        assertEquals(service.collect(lockSettings()).status(), Status.SUCCESS);
        assertEquals(service.train(lockSettings()).status(), Status.SUCCESS);
        assertNotNull(JobState.read(output).train());
        assertNull(JobState.read(output).send().basedOn());
    }

    @Test
    public void schedulesAreStaggeredByDefaultAndConfigurable() {
        FacadeSettings defaults = settings();
        assertEquals(defaults.sendInitialDelaySeconds(), 300);
        assertEquals(defaults.sendIntervalSeconds(), 3600);
        assertEquals(defaults.collectInitialDelaySeconds(), 300 + 3600);
        assertEquals(defaults.collectIntervalSeconds(), 3600);
        assertEquals(defaults.trainInitialDelaySeconds(), 300 + 3600 + 3600);
        assertEquals(defaults.trainIntervalSeconds(), 3600);

        Map<String, String> values = settingsValues();
        values.put("facade.send.initial.delay.seconds", "10");
        values.put("facade.send.interval.seconds", "600");
        values.put("facade.collect.interval.seconds", "1200");
        values.put("facade.train.initial.delay.seconds", "5000");
        FacadeSettings custom = new FacadeSettings(values::get);
        assertEquals(custom.collectInitialDelaySeconds(), 610);
        assertEquals(custom.collectIntervalSeconds(), 1200);
        assertEquals(custom.trainInitialDelaySeconds(), 5000);
    }

    // ---- helpers

    private void sendWasDone() throws IOException {
        Files.createDirectories(input);
        Files.writeString(input.resolve("authorization.json"), "{}");
        List<TraceFiles.Sent> sent = new ArrayList<>();
        for (int i = 0; i < EXECUTIONS; i++) {
            sent.add(new TraceFiles.Sent("exec-" + i, "https://gateway.example.org", i < 45 ? "train" : "test",
                    PER_EXECUTION, List.of("agent-0")));
        }
        TraceFiles.writeExecutions(output, "run1", "facade-plugin", sent);
        new JobState(new Stage(JobState.SUCCESS, Instant.now(), "sent 60 executions", 1, "fp-1", null), null, null)
                .write(output);
    }

    private void startFakeLock() throws IOException {
        lock = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        lock.createContext("/jans-auth/restv1/token", exchange -> reply(exchange, "{\"access_token\":\"t\"}"));
        lock.createContext("/jans-lock/api/v1/audit/trace/executions/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            int index = Integer.parseInt(path.substring(path.lastIndexOf("exec-") + 5));
            List<String> records = new ArrayList<>();
            int count = index == 0 ? recordsPerExecution.get() : PER_EXECUTION;
            for (int i = 0; i < count; i++) {
                int n = index * PER_EXECUTION + i;
                records.add(record("agent-" + (n % 5), "exec-" + index, BASE + (long) n * 10));
            }
            reply(exchange, "{\"records\":[" + String.join(",", records) + "]}");
        });
        lock.start();
    }

    private FacadeSettings lockSettings() {
        Map<String, String> values = settingsValues();
        values.put("facade.lock.base.url", "http://localhost:" + lock.getAddress().getPort());
        values.put("facade.lock.client.id", "client-1");
        values.put("facade.lock.client.secret", "secret-1");
        values.put("facade.collect.timeout.seconds", "1");
        return new FacadeSettings(values::get);
    }

    private FacadeSettings settings() {
        return new FacadeSettings(settingsValues()::get);
    }

    private Map<String, String> settingsValues() {
        Map<String, String> values = new HashMap<>();
        values.put("facade.input.dir", input.toString());
        values.put("facade.output.dir", output.toString());
        values.put("facade.run", "false");
        return values;
    }

    private static void reply(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String record(String principal, String execution, long signedAt) {
        return "{\"record_id\":\"" + UUID.randomUUID() + "\",\"producer\":\"cedarling-test\","
                + "\"trace\":{\"event_kind\":\"AUTHORIZATION_DECISION\",\"signed_at\":" + signedAt
                + ",\"trace_execution_id\":\"" + execution + "\","
                + "\"execution_authority\":\"https://gateway.example.org\","
                + "\"subject\":{\"workload_id\":\"" + principal + "\"},"
                + "\"event\":{\"decision\":{\"action\":\"Acme::Action::\\\"Deploy\\\"\","
                + "\"resource_type\":\"Acme::Resource\",\"outcome\":\"ALLOW\"}}}}";
    }

    private static String traces(int count) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(record("agent-" + (i % 5), "exec-" + (i / 5), BASE + (long) i * 10));
        }
        return String.join("\n", lines) + "\n";
    }
}
