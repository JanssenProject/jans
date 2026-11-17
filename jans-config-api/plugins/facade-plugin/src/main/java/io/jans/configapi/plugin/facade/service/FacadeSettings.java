/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.service;

import io.jans.configapi.plugin.facade.lock.LockSettings;
import io.jans.configapi.plugin.facade.util.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;

public final class FacadeSettings {

    private final Function<String, String> source;

    public FacadeSettings(Function<String, String> source) {
        this.source = source;
    }

    public static FacadeSettings load() {
        return new FacadeSettings(name -> ConfigProvider.getConfig().getOptionalValue(name, String.class).orElse(null));
    }

    public Path inputDir() {
        return Path.of(get(Constants.INPUT_DIR, Constants.DEFAULT_INPUT_DIR)).toAbsolutePath();
    }

    /** Directory holding the policy store (metadata.json, schema.cedarschema, policies/). */
    public Path policyStoreDir() {
        return Path.of(get(Constants.POLICY_STORE_DIR, Constants.DEFAULT_POLICY_STORE_DIR)).toAbsolutePath();
    }

    public Path outputDir() {
        return Path.of(get(Constants.OUTPUT_DIR, Constants.DEFAULT_OUTPUT_DIR)).toAbsolutePath();
    }

    public boolean jobEnabled() {
        return Boolean.parseBoolean(get(Constants.JOB_ENABLED, "true"));
    }

    /** Seconds before the first send, and between sends. */
    public int sendInitialDelaySeconds() {
        return Integer.parseInt(get("facade.send.initial.delay.seconds", "300"));
    }

    public int sendIntervalSeconds() {
        return Integer.parseInt(get("facade.send.interval.seconds", "3600"));
    }

    /** By default the first collection is one send interval after the first send. */
    public int collectInitialDelaySeconds() {
        return Integer.parseInt(get("facade.collect.initial.delay.seconds",
                Integer.toString(sendInitialDelaySeconds() + sendIntervalSeconds())));
    }

    public int collectIntervalSeconds() {
        return Integer.parseInt(get("facade.collect.interval.seconds", "3600"));
    }

    /** By default the first training is one collect interval after the first collection. */
    public int trainInitialDelaySeconds() {
        return Integer.parseInt(get("facade.train.initial.delay.seconds",
                Integer.toString(collectInitialDelaySeconds() + collectIntervalSeconds())));
    }

    public int trainIntervalSeconds() {
        return Integer.parseInt(get("facade.train.interval.seconds", "3600"));
    }

    /** Tries for a collection or a training that keeps failing, before the job stops until there is new data. */
    public int maxAttempts() {
        return Integer.parseInt(get("facade.job.max.attempts", "3"));
    }

    public boolean rerunUnchanged() {
        return Boolean.parseBoolean(get("facade.job.rerun.unchanged", "false"));
    }

    public int collectTimeoutSeconds() {
        return Integer.parseInt(get("facade.collect.timeout.seconds", "60"));
    }

    /**
     * Connection to Lock and the Auth Server, needed to run authorizations and collect their traces.
     *
     * @throws IllegalStateException if a required setting is missing
     */
    public LockSettings lock() {
        String lockUrl = trimSlash(get("facade.lock.base.url", null));
        String authUrl = trimSlash(get("facade.auth.base.url", lockUrl));
        String clientId = get("facade.lock.client.id", null);
        String clientSecret = get("facade.lock.client.secret", null);
        if (lockUrl == null || clientId == null || clientSecret == null) {
            throw new IllegalStateException("authorization.json needs a Lock connection: set facade.lock.base.url,"
                    + " facade.lock.client.id and facade.lock.client.secret (and facade.auth.base.url if the Auth"
                    + " Server is on another host)");
        }
        return new LockSettings(lockUrl, authUrl, clientId, clientSecret);
    }

    public Optional<Path> facadeDir() {
        String value = get("facade.dir", null);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Path dir = Path.of(value).toAbsolutePath();
        if (!Files.isDirectory(dir.resolve("batch"))) {
            throw new IllegalStateException(dir + " is not a FACADE checkout (no batch/ directory)");
        }
        return Optional.of(dir);
    }

    public String python(Path facadeDir) {
        String configured = get("facade.python", null);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        Path venv = facadeDir.resolve("env/bin/python");
        return Files.isExecutable(venv) ? venv.toString() : "python3";
    }

    public boolean runFacade() {
        return Boolean.parseBoolean(get("facade.run", "true"));
    }

    public List<String> peerAttrs() {
        return list("facade.peerAttrs", "execution");
    }

    public List<String> outcomes() {
        return list("facade.outcomes", "ALLOW,SUCCESS");
    }

    public List<String> actionKinds() {
        return list("facade.actionKinds", "AUTHORIZATION_DECISION");
    }

    public boolean useReceivedAt() {
        return get("facade.timeSource", "signed_at").equals("received_at");
    }

    public int trainingExamples() {
        return Integer.parseInt(get("facade.trainingExamples", "20000"));
    }

    public double testFraction() {
        return Double.parseDouble(get("facade.testFraction", "0.2"));
    }

    public String windowOverride(String name) {
        return get("facade." + name, "auto");
    }

    private static String trimSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private List<String> list(String key, String fallback) {
        return Arrays.stream(get(key, fallback).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private String get(String key, String fallback) {
        String value = source.apply(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
