/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.configapi.plugin.facade.convert.TraceReader;
import io.jans.configapi.plugin.facade.generate.AcmeInputs;
import io.jans.configapi.plugin.facade.generate.TraceCollector;
import io.jans.configapi.plugin.facade.generate.TraceFiles;
import io.jans.configapi.plugin.facade.generate.TraceGenerator;
import io.jans.configapi.plugin.facade.lock.LockClient;
import io.jans.configapi.plugin.facade.lock.LockSettings;
import io.jans.configapi.plugin.facade.lock.TraceSession;
import io.jans.configapi.plugin.facade.service.JobState.Stage;
import io.jans.configapi.plugin.facade.util.Constants;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The three jobs that turn authorizations into a trained FACADE model. Each runs on its own timer, and they pass work
 * on through {@code state.json} in the output directory:
 * <ol>
 * <li>{@link #send()}: if the input directory has {@code authorization.json}, Cedarling authorizes the requests in it
 * and sends a signed TRACE record to Lock for each decision.</li>
 * <li>{@link #collect()}: fetches those records back from Lock into {@code traces/}. Lock takes time to store them, so
 * a collection that finds records missing is tried again on the next run.</li>
 * <li>{@link #train()}: converts the collected traces to FACADE files, and trains and scores if a FACADE checkout is
 * configured. Without {@code authorization.json} it uses Lock trace files from the input directory instead.</li>
 * </ol>
 * Only one job runs at a time. A job that finds another running skips that run.
 */
@ApplicationScoped
public class FacadeJobService {

    public enum Status {
        NO_INPUT, UNCHANGED, SUCCESS, PARTIAL, FAILED, BUSY
    }

    public record Result(Status status, String message) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(FacadeJobService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReentrantLock running = new ReentrantLock();

    public Result send() {
        return send(FacadeSettings.load());
    }

    public Result send(FacadeSettings settings) {
        return exclusive("send", () -> doSend(settings));
    }

    public Result collect() {
        return collect(FacadeSettings.load());
    }

    public Result collect(FacadeSettings settings) {
        return exclusive("collect", () -> doCollect(settings));
    }

    public Result train() {
        return train(FacadeSettings.load());
    }

    public Result train(FacadeSettings settings) {
        return exclusive("train", () -> doTrain(settings));
    }

    private Result exclusive(String job, Supplier<Result> body) {
        if (!running.tryLock()) {
            LOG.info("FACADE {} job skipped: another FACADE job is running", job);
            return new Result(Status.BUSY, "another job is running");
        }
        try {
            return body.get();
        } finally {
            running.unlock();
        }
    }

    private Result doSend(FacadeSettings settings) {
        Path input = settings.inputDir();
        Path output = settings.outputDir();
        Path authorization = input.resolve(AcmeInputs.FILE_NAME);
        if (!Files.isRegularFile(authorization)) {
            LOG.info("FACADE send job: no {} in {}", AcmeInputs.FILE_NAME, input);
            return new Result(Status.NO_INPUT, "no authorization.json in " + input);
        }
        JobState state = null;
        String fingerprint = null;
        try {
            state = JobState.read(output);
            fingerprint = fingerprint(input, List.of(authorization));
            Stage last = state.send();
            if (!settings.rerunUnchanged() && last != null && last.succeeded()
                    && fingerprint.equals(last.fingerprint())) {
                LOG.info("FACADE send job: {} is unchanged since the last send", AcmeInputs.FILE_NAME);
                return new Result(Status.UNCHANGED, "authorization.json unchanged");
            }

            String message = sendRecords(settings, authorization, output);
            new JobState(new Stage(JobState.SUCCESS, Instant.now(), message, 1, fingerprint, null), null, null)
                    .write(output);
            LOG.info("FACADE send job finished: {}", message);
            return new Result(Status.SUCCESS, message);
        } catch (IOException | InterruptedException | RuntimeException e) {
            return failed("send", e, output, state, JobState::withSend, 1, fingerprint, null);
        }
    }

    /**
     * Runs the authorizations in {@code authorization.json} through Cedarling, which signs a TRACE record for each
     * decision and sends it to Lock.
     */
    private String sendRecords(FacadeSettings settings, Path authorization, Path output)
            throws IOException, InterruptedException {
        LockSettings lockSettings = settings.lock();
        AcmeInputs inputs = AcmeInputs.load(authorization);
        clearGenerated(output);

        LOG.info("FACADE send job: running authorizations against Lock {}", lockSettings.lockBaseUrl());
        try (TraceSession session = TraceSession.open(lockSettings)) {
            TraceGenerator.Outcome outcome = TraceGenerator.generate(inputs, session, output, settings.policyStoreDir());
            return "authorized " + outcome.executions() + " executions (" + outcome.allowed() + " allowed, "
                    + outcome.denied() + " denied) and sent their TRACE records to Lock";
        }
    }

    private Result doCollect(FacadeSettings settings) {
        Path output = settings.outputDir();
        JobState state = null;
        int attempts = 0;
        Instant sentAt = null;
        try {
            state = JobState.read(output);
            Stage send = state.send();
            if (send == null || !send.succeeded()) {
                LOG.info("FACADE collect job: no TRACE records have been sent");
                return new Result(Status.NO_INPUT, "no TRACE records have been sent");
            }
            sentAt = send.at();

            Stage last = state.collect();
            boolean sameSend = last != null && sentAt.equals(last.basedOn());
            if (sameSend && last.succeeded()) {
                LOG.info("FACADE collect job: the traces were already collected");
                return new Result(Status.UNCHANGED, "already collected");
            }
            attempts = sameSend ? last.attempts() : 0;
            if (sameSend && attempts >= settings.maxAttempts()) {
                return gaveUp("collect", attempts);
            }
            attempts++;

            LockClient lock = new LockClient(settings.lock());
            int missing = TraceCollector.collect(lock, lock.token(), output,
                    Duration.ofSeconds(settings.collectTimeoutSeconds()));
            boolean accept = missing == 0 || attempts >= settings.maxAttempts();
            String message = "collected traces (" + missing + " records missing)"
                    + (missing > 0 && accept ? ", accepted after " + attempts + " attempts" : "");
            state.withCollect(new Stage(accept ? JobState.SUCCESS : JobState.PARTIAL, Instant.now(), message,
                    attempts, null, sentAt)).write(output);
            LOG.info("FACADE collect job finished: {}", message);
            return new Result(accept ? Status.SUCCESS : Status.PARTIAL, accept ? message
                    : message + ", will try again on the next run");
        } catch (IOException | InterruptedException | RuntimeException e) {
            return failed("collect", e, output, state, JobState::withCollect, attempts, null, sentAt);
        }
    }

    private Result doTrain(FacadeSettings settings) {
        Path input = settings.inputDir();
        Path output = settings.outputDir();
        JobState state = null;
        int attempts = 0;
        String fingerprint = null;
        Instant basedOn = null;
        try {
            state = JobState.read(output);
            Stage last = state.train();
            Path traces;
            Path controlDir;
            String prefix;
            boolean same;
            if (Files.isRegularFile(input.resolve(AcmeInputs.FILE_NAME))) {
                Stage collected = state.collect();
                if (collected == null || !collected.succeeded()) {
                    LOG.info("FACADE train job: no collected traces yet");
                    return new Result(Status.NO_INPUT, "no collected traces yet");
                }
                basedOn = collected.at();
                same = last != null && basedOn.equals(last.basedOn());
                traces = output.resolve(TraceFiles.TRACES_DIR);
                controlDir = output;
                prefix = state.send().message() + ", " + collected.message();
            } else {
                List<Path> files = Files.isDirectory(input) ? TraceReader.traceFiles(input) : List.of();
                if (files.isEmpty()) {
                    LOG.info("FACADE train job: no {} or trace files in {}", AcmeInputs.FILE_NAME, input);
                    return new Result(Status.NO_INPUT, "no authorization.json or trace files in " + input);
                }
                fingerprint = fingerprint(input, withControlFiles(input, files));
                same = last != null && fingerprint.equals(last.fingerprint())
                        && (last.succeeded() ? !settings.rerunUnchanged() : true);
                traces = input;
                controlDir = input;
                prefix = "converted " + files.size() + " files";
            }

            if (same && last.succeeded()) {
                LOG.info("FACADE train job: nothing new since the last training");
                return new Result(Status.UNCHANGED, "nothing new to train on");
            }
            attempts = same ? last.attempts() : 0;
            if (same && attempts >= settings.maxAttempts()) {
                return gaveUp("train", attempts);
            }
            attempts++;

            String message = convertAndTrain(settings, traces, output, controlDir, prefix);
            state.withTrain(new Stage(JobState.SUCCESS, Instant.now(), message, attempts, fingerprint, basedOn))
                    .write(output);
            LOG.info("FACADE train job finished: {}", message);
            return new Result(Status.SUCCESS, message);
        } catch (IOException | InterruptedException | RuntimeException e) {
            return failed("train", e, output, state, JobState::withTrain, attempts, fingerprint, basedOn);
        }
    }

    private String convertAndTrain(FacadeSettings settings, Path traces, Path output, Path controlDir,
            String message) throws IOException, InterruptedException {
        FacadePipeline pipeline = new FacadePipeline(settings);
        FacadePipeline.Prepared prepared = pipeline.prepare(traces, output,
                testStart(controlDir.resolve(Constants.RUN_FILE)));
        LOG.info("FACADE train job: converted input\n{}", Files.readString(prepared.dir().resolve("summary.txt")));

        if (settings.runFacade() && settings.facadeDir().isPresent()) {
            pipeline.trainAndScore(prepared, settings.facadeDir().get(), controlDir.resolve(Constants.TRUTH_FILE));
            return message + ", trained and scored";
        }
        LOG.info("FACADE train job: training skipped (facade.run is false or facade.dir is not set)");
        return message + ", training skipped";
    }

    private static Result gaveUp(String job, int attempts) {
        LOG.info("FACADE {} job: gave up after {} attempts, waiting for new data", job, attempts);
        return new Result(Status.UNCHANGED, "gave up after " + attempts + " attempts, waiting for new data");
    }

    private interface WithStage {
        JobState apply(JobState state, Stage stage);
    }

    private static Result failed(String job, Exception e, Path output, JobState state, WithStage with, int attempts,
            String fingerprint, Instant basedOn) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        LOG.error("FACADE {} job failed", job, e);
        String message = String.valueOf(e.getMessage());
        try {
            JobState base = state != null ? state : JobState.read(output);
            with.apply(base, new Stage(JobState.FAILED, Instant.now(), message, Math.max(1, attempts), fingerprint,
                    basedOn)).write(output);
        } catch (IOException | RuntimeException io) {
            LOG.warn("Could not write {} in {}", Constants.STATE_FILE, output, io);
        }
        return new Result(Status.FAILED, message);
    }

    private static void clearGenerated(Path output) throws IOException {
        for (String name : List.of(TraceFiles.TRACES_DIR, TraceFiles.EXECUTIONS_FILE, TraceFiles.TRUTH_FILE,
                TraceFiles.RUN_FILE)) {
            Path path = output.resolve(name);
            if (!Files.exists(path)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(path)) {
                for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) {
                    Files.delete(p);
                }
            }
        }
    }

    private static Instant testStart(Path run) throws IOException {
        if (!Files.isRegularFile(run)) {
            return null;
        }
        JsonNode start = MAPPER.readTree(run.toFile()).path("test_start");
        return start.isTextual() ? Instant.parse(start.asText()) : null;
    }

    private static List<Path> withControlFiles(Path input, List<Path> traceFiles) {
        List<Path> all = new java.util.ArrayList<>(traceFiles);
        for (String name : List.of(Constants.RUN_FILE, Constants.TRUTH_FILE)) {
            Path control = input.resolve(name);
            if (Files.isRegularFile(control)) {
                all.add(control);
            }
        }
        return all;
    }

    private static String fingerprint(Path input, List<Path> files) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Path file : files) {
                String entry = input.relativize(file) + "|" + Files.size(file) + "|"
                        + Files.getLastModifiedTime(file).toMillis() + "\n";
                digest.update(entry.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
