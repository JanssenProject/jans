/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import io.jans.configapi.plugin.facade.lock.TraceSession;

import org.json.JSONObject;

import uniffi.cedarling_uniffi.AuthorizeException;
import uniffi.cedarling_uniffi.AuthorizeOptions;
import uniffi.cedarling_uniffi.BatchAuthorizeMultiIssuerResponse;
import uniffi.cedarling_uniffi.BatchItem;
import uniffi.cedarling_uniffi.BatchItemMultiIssuerOutcome;
import uniffi.cedarling_uniffi.CedarlingException;
import uniffi.cedarling_uniffi.EntityException;
import uniffi.cedarling_uniffi.TraceContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the traffic plan through Cedarling. Each decision becomes a TRACE record that Cedarling signs and sends to
 * Lock. The executions that were sent are saved in {@code executions.json}, with {@code truth.json} and
 * {@code run.json}, in the output directory.
 */
public final class TraceGenerator {

    /** What Cedarling needs to start: a run id, and the bootstrap properties for a given policy store. */
    public interface Bootstrap {
        String runId();

        JSONObject bootstrap(Path policyStore, String logLevel);
    }

    public record Outcome(int executions, int allowed, int denied, int failed, int anomalies) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(TraceGenerator.class);

    private TraceGenerator() {
    }

    public static Outcome generate(AcmeInputs inputs, Bootstrap bootstrap, Path outDir, Path policyStoreDir)
            throws IOException {
        List<AcmePlan.Execution> plan = AcmePlan.build(bootstrap.runId(), inputs);
        LOG.info("Plan: {} executions, {} decisions", plan.size(),
                plan.stream().mapToInt(AcmePlan.Execution::expectedRecords).sum());

        Path store = null;
        try (TestIssuer issuer = new TestIssuer()) {
            store = AcmeStore.build(policyStoreDir, issuer.issuer(), issuer.jwksUrl());
            JSONObject properties = bootstrap.bootstrap(store, "INFO");
            try (CedarlingAdapter adapter = new CedarlingAdapter()) {
                adapter.loadFromJson(properties.toString());
                return run(adapter, issuer, bootstrap.runId(), inputs, plan, outDir);
            }
        } catch (CedarlingException e) {
            throw new IllegalStateException("Cedarling failed to start: " + e, e);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("could not start the local token issuer", e);
        } finally {
            if (store != null) {
                Files.deleteIfExists(store);
            }
        }
    }

    private static Outcome run(CedarlingAdapter adapter, TestIssuer issuer, String runId, AcmeInputs inputs,
            List<AcmePlan.Execution> plan, Path outDir) throws IOException {
        Map<String, Map<String, String>> tokensByUser = new HashMap<>();
        List<TraceFiles.Anomaly> anomalies = new ArrayList<>();
        List<TraceFiles.Sent> sent = new ArrayList<>();
        int allowed = 0;
        int denied = 0;
        int failed = 0;
        Instant testStart = null;

        try {
            for (AcmePlan.Execution execution : plan) {
                if (testStart == null && execution.phase().equals("test")) {
                    testStart = Instant.now();
                }
                int expected = 0;
                List<String> workloadIds = new ArrayList<>();
                for (AcmePlan.Call call : execution.calls()) {
                    String workloadId = call.user().workloadId();
                    Map<String, String> tokens = tokensByUser.get(workloadId);
                    if (tokens == null) {
                        tokens = tokens(issuer, inputs, call.user(), runId);
                        tokensByUser.put(workloadId, tokens);
                    }
                    BatchAuthorizeMultiIssuerResponse response = authorize(adapter, inputs, execution, call, tokens);
                    workloadIds.add(workloadId);
                    for (int i = 0; i < call.items().size(); i++) {
                        BatchItemMultiIssuerOutcome outcome = response.getResults().get(i);
                        if (!CedarlingAdapter.isOk(outcome)) {
                            failed++;
                            continue;
                        }
                        expected++;
                        var result = CedarlingAdapter.unwrap(outcome);
                        if (result.getDecision()) {
                            allowed++;
                        } else {
                            denied++;
                        }
                        AcmePlan.Item item = call.items().get(i);
                        if (item.anomaly() && result.getDecision()) {
                            anomalies.add(new TraceFiles.Anomaly(result.getRequestId(), workloadId,
                                    item.capability(), Instant.now().getEpochSecond()));
                        }
                    }
                }
                sent.add(new TraceFiles.Sent(execution.id(), inputs.executionAuthority(), execution.phase(),
                        expected, workloadIds));
                if (inputs.pauseMillis() > 0) {
                    Thread.sleep(inputs.pauseMillis());
                }
            }
        } catch (AuthorizeException | EntityException | java.security.GeneralSecurityException e) {
            throw new IllegalStateException("authorization failed: " + e, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while generating traces", e);
        }
        LOG.info("Decisions: allowed={} denied={} failed={}, anomalies recorded={}", allowed, denied, failed,
                anomalies.size());

        TraceFiles.writeExecutions(outDir, runId, TraceSession.PRODUCER_ID, sent);
        TraceFiles.writeTruth(outDir, anomalies);
        TraceFiles.writeRun(outDir, testStart == null ? Instant.now() : testStart);
        return new Outcome(sent.size(), allowed, denied, failed, anomalies.size());
    }

    private static BatchAuthorizeMultiIssuerResponse authorize(CedarlingAdapter adapter, AcmeInputs inputs,
            AcmePlan.Execution execution, AcmePlan.Call call, Map<String, String> tokens)
            throws AuthorizeException, EntityException {
        List<BatchItem> items = new ArrayList<>();
        for (AcmePlan.Item item : call.items()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("authorize execution={} workload={}\n  action:   {}\n  resource: {}\n  context:  {}",
                        execution.id(), call.user().workloadId(), item.action(), item.resource(), item.context());
            }
            items.add(adapter.batchItemFromJson(item.resource(), item.action(), item.context()));
        }
        String workloadId = call.user().workloadId();
        AuthorizeOptions options = new AuthorizeOptions(new TraceContext(
                execution.id(), inputs.executionAuthority(), workloadId, workloadId + "-1"));
        return adapter.authorizeMultiIssuerBatch(tokens, items, options);
    }

    private static Map<String, String> tokens(TestIssuer issuer, AcmeInputs inputs, AcmePlan.User user, String runId)
            throws IOException, java.security.GeneralSecurityException {
        String sub = user.inum() + "-" + runId;
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("AcmeOps::Access_token", issuer.accessToken(sub, inputs.clientId()));
        tokens.put("AcmeOps::id_token", issuer.idToken(sub, inputs.clientId()));
        tokens.put("AcmeOps::Userinfo_token",
                issuer.userinfoToken(sub, inputs.clientId(), user.givenName(), user.inum(), user.roles()));
        return tokens;
    }
}
