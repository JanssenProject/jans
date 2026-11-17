/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.lock;

import io.jans.configapi.plugin.facade.generate.TraceGenerator;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;

/**
 * One run's connection to Lock: gets a token, registers a new producer key and chain, and builds the Cedarling
 * bootstrap that makes Cedarling sign and send a TRACE record for each decision.
 */
public final class TraceSession implements TraceGenerator.Bootstrap, AutoCloseable {

    public static final String PRODUCER_ID = "facade-plugin";

    private final LockSettings config;
    private final LockClient lock;
    private final String token;
    private final Path keyFile;
    private final String kid;
    private final String runId;

    private TraceSession(LockSettings config, LockClient lock, String token, Path keyFile, String kid,
            String runId) {
        this.config = config;
        this.lock = lock;
        this.token = token;
        this.keyFile = keyFile;
        this.kid = kid;
        this.runId = runId;
    }

    public static TraceSession open(LockSettings config) throws IOException, InterruptedException {
        LockClient lock = new LockClient(config);
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String kid = PRODUCER_ID + "-kid-" + runId;

        String token = lock.token();
        TraceSigningKey key = TraceSigningKey.generate();
        lock.registerProducerKey(PRODUCER_ID, kid, key, token);
        lock.registerProducerChain(PRODUCER_ID, PRODUCER_ID + "-instance-" + runId, PRODUCER_ID + "-chain-" + runId,
                token);

        Path keyFile = Files.createTempFile("trace-key-", ".pem",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(keyFile, key.privateKeyPem());
        return new TraceSession(config, lock, token, keyFile, kid, runId);
    }

    @Override
    public JSONObject bootstrap(Path store, String logLevel) {
        return new JSONObject()
                .put("CEDARLING_APPLICATION_NAME", PRODUCER_ID)
                .put("CEDARLING_POLICY_STORE_LOCAL_FN", store.toString())
                .put("CEDARLING_LOG_TYPE", "std_out")
                .put("CEDARLING_LOG_LEVEL", logLevel)
                .put("CEDARLING_LOCK", "enabled")
                .put("CEDARLING_LOCK_SERVER_CONFIGURATION_URI", config.wellKnownUri())
                .put("CEDARLING_LOCK_ACCESS_TOKEN_JWT", token)
                .put("CEDARLING_TRACE", "enabled")
                .put("CEDARLING_TRACE_PRODUCER_ID", PRODUCER_ID)
                .put("CEDARLING_TRACE_SIGNING_KEY", keyFile.toString())
                .put("CEDARLING_TRACE_SIGNING_KID", kid);
    }

    public LockClient lock() {
        return lock;
    }

    public String readToken() {
        return token;
    }

    @Override
    public String runId() {
        return runId;
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(keyFile);
        } catch (IOException e) {
            keyFile.toFile().deleteOnExit();
        }
    }
}
