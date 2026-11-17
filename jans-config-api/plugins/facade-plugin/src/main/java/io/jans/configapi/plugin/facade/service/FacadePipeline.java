/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.service;

import io.jans.configapi.plugin.facade.convert.FacadeFiles;
import io.jans.configapi.plugin.facade.convert.FacadeMapper;
import io.jans.configapi.plugin.facade.convert.FacadeRows.Action;
import io.jans.configapi.plugin.facade.convert.FacadeRows.Context;
import io.jans.configapi.plugin.facade.convert.FacadeTemplates;
import io.jans.configapi.plugin.facade.convert.TraceReader;
import io.jans.configapi.plugin.facade.convert.TraceRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class FacadePipeline {

    private static final DateTimeFormatter FACADE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);
    private static final String EVALUATE_RESOURCE = "/facade/evaluate_scores.py";
    private static final String EVALUATE_FILE = "evaluate_scores.py";

    public record Prepared(Path dir, FacadeMapper.Stats stats, String trainStart, String trainEnd, String testEnd,
            FacadeTemplates.Windows windows) {
    }

    private final FacadeSettings settings;

    public FacadePipeline(FacadeSettings settings) {
        this.settings = settings;
    }

    public Prepared prepare(Path traceDir, Path outDir, Instant testStart) throws IOException {
        Files.createDirectories(outDir);

        List<TraceRecord> traces = TraceReader.read(traceDir);
        FacadeMapper.Options options = new FacadeMapper.Options(settings.actionKinds(), settings.outcomes(),
                settings.peerAttrs(), settings.useReceivedAt());
        FacadeMapper.Result result = FacadeMapper.convert(traces, options);
        List<Action> actions = result.actions();
        List<Context> contexts = result.contexts();
        if (actions.isEmpty() || contexts.isEmpty()) {
            throw new IOException("no actions or contexts produced from " + traceDir + ": " + result.stats());
        }

        long minTime = Math.min(actions.stream().mapToLong(Action::occurredAt).min().getAsLong(),
                contexts.stream().mapToLong(Context::validFrom).min().getAsLong());
        long maxTime = actions.stream().mapToLong(Action::occurredAt).max().getAsLong();
        long span = Math.max(1, maxTime - minTime);
        long split = testStart != null ? testStart.getEpochSecond() : splitTime(actions, minTime, maxTime);

        long trainSpan = split - minTime;
        long testSpan = maxTime + 1 - split;
        if (trainSpan < 2 || testSpan < 1) {
            throw new IOException("too little trace data to train: " + actions.size() + " actions over " + span
                    + "s (train " + trainSpan + "s, test " + testSpan + "s). Provide more traces than the "
                    + traceDir.getFileName() + " directory holds");
        }
        FacadeTemplates.Windows windows = windows(span, trainSpan);
        FacadeFiles.writeActionsJsonl(outDir.resolve("actions.jsonl"), actions);
        FacadeFiles.writeContextsJsonl(outDir.resolve("contexts.jsonl"), contexts);
        FacadeFiles.writeActionTfRecord(outDir.resolve("action.tfrecord"), actions);
        FacadeFiles.writeContextTfRecord(outDir.resolve("context.tfrecord"), contexts);
        Files.writeString(outDir.resolve("directive.textproto"),
                FacadeTemplates.directive(settings.peerAttrs(), windows));
        Files.writeString(outDir.resolve("config.textproto"),
                FacadeTemplates.config(settings.peerAttrs(), settings.trainingExamples()));

        Prepared prepared = new Prepared(outDir, result.stats(), format(minTime), format(split), format(maxTime + 1),
                windows);
        Files.writeString(outDir.resolve("summary.txt"), summary(prepared));
        return prepared;
    }

    public void trainAndScore(Prepared prepared, Path facadeDir, Path truth) throws IOException, InterruptedException {
        FacadeCommands commands = new FacadeCommands(facadeDir, settings.python(facadeDir),
                prepared.dir().resolve("logs"));
        commands.datasetMakerTrain(prepared.dir(), prepared.trainStart(), prepared.trainEnd());
        commands.train(prepared.dir());
        commands.inference(prepared.dir(), prepared.trainEnd(), prepared.testEnd());
        commands.readScores(prepared.dir(), 15);
        if (truth != null && Files.isRegularFile(truth)) {
            commands.evaluate(extractEvaluateScript(prepared.dir()), prepared.dir(), truth);
        }
    }

    private static Path extractEvaluateScript(Path outDir) throws IOException {
        Path script = outDir.resolve(EVALUATE_FILE);
        try (InputStream in = FacadePipeline.class.getResourceAsStream(EVALUATE_RESOURCE)) {
            if (in == null) {
                throw new IOException("missing resource " + EVALUATE_RESOURCE);
            }
            Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
        }
        return script;
    }

    private long splitTime(List<Action> actions, long minTime, long maxTime) {
        List<Long> times = actions.stream().map(Action::occurredAt).sorted().toList();
        long byFraction = maxTime - Math.round((maxTime - minTime) * settings.testFraction());
        long byCount = times.get((int) Math.floor(times.size() * (1 - settings.testFraction())));
        return Math.max(Math.min(byFraction, byCount), minTime + 1);
    }

    private FacadeTemplates.Windows windows(long span, long trainSpan) {
        long snapshot = Math.max(1, Math.min(value("snapshotSeconds", Math.max(5, span / 60)), trainSpan / 2));
        long history = value("historySeconds", Math.max(3600, span * 4));
        long lookback = value("lookbackSeconds", history);
        long dedup = value("dedupSeconds", Math.max(1, snapshot / 10));
        return new FacadeTemplates.Windows(history, dedup, lookback, snapshot);
    }

    private long value(String name, long auto) {
        String configured = settings.windowOverride(name);
        return configured.equals("auto") ? auto : Long.parseLong(configured);
    }

    private static String format(long epochSeconds) {
        return FACADE_TIME.format(Instant.ofEpochSecond(epochSeconds));
    }

    private static String summary(Prepared p) {
        FacadeMapper.Stats s = p.stats();
        return """
                records read:          %d
                dropped unverified:    %d
                dropped void receipt:  %d
                dropped duplicates:    %d
                dropped no principal:  %d
                skipped by event kind: %d
                skipped by outcome:    %d
                actions:               %d
                contexts:              %d
                train window:          %s to %s
                test window:           %s to %s
                snapshot seconds:      %d
                history seconds:       %d
                lookback seconds:      %d
                dedup seconds:         %d
                """.formatted(s.recordsIn(), s.droppedUnverified(), s.droppedVoid(), s.droppedDuplicate(),
                s.droppedNoPrincipal(), s.skippedKind(), s.skippedOutcome(), s.actions(), s.contexts(),
                p.trainStart(), p.trainEnd(), p.trainEnd(), p.testEnd(), p.windows().snapshotSeconds(),
                p.windows().historySeconds(), p.windows().lookbackSeconds(), p.windows().dedupSeconds());
    }
}
