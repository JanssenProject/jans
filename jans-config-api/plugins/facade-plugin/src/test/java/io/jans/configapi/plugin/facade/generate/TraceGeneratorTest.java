/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class TraceGeneratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Path root;

    @BeforeMethod
    public void setUp() throws IOException {
        root = Files.createTempDirectory("facade-generator-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    public void sampleInputsParseAndPlanIsReproducible() throws IOException {
        AcmeInputs inputs = AcmeInputs.load(writeInputs(200, 40, 6));

        List<AcmePlan.Execution> first = AcmePlan.build("run1", inputs);
        List<AcmePlan.Execution> second = AcmePlan.build("run1", inputs);

        assertEquals(first.size(), 246);
        assertEquals(first.stream().mapToInt(AcmePlan.Execution::expectedRecords).sum(),
                second.stream().mapToInt(AcmePlan.Execution::expectedRecords).sum());
        assertEquals(first.stream().filter(e -> e.phase().equals("test")).count(), 46);
        assertTrue(first.stream().flatMap(e -> e.calls().stream()).flatMap(c -> c.items().stream())
                .filter(AcmePlan.Item::anomaly).count() == 6);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*not defined under.*")
    public void unknownActionIsRejected() throws IOException {
        Path file = writeInputs(1, 1, 1);
        JSONObject json = new JSONObject(Files.readString(file));
        json.getJSONObject("anomaly").put("action", "Nope");
        Files.writeString(file, json.toString(2));
        AcmeInputs.load(file);
    }

    @Test
    public void policyStoreArchiveHasTheBundledPoliciesAndATrustedIssuer() throws IOException {
        Path cjar = AcmeStore.build(root.resolve("no-policy-store"), "http://localhost:1234", "http://localhost:1234/jwks");
        try (var zip = new java.util.zip.ZipFile(cjar.toFile())) {
            assertTrue(zip.getEntry("schema.cedarschema") != null);
            assertTrue(zip.getEntry("policies/deploy-staging-token.cedar") != null);
            JsonNode issuers = MAPPER.readTree(zip.getInputStream(zip.getEntry("trusted-issuers.json")));
            assertEquals(issuers.path("AcmeOps::Access_token").path("configuration_endpoints").path("iss").asText(),
                    "http://localhost:1234");
            assertEquals(MAPPER.readTree(zip.getInputStream(zip.getEntry("metadata.json")))
                    .path("policy_store_spec_version").asInt(), 2);
        } finally {
            Files.deleteIfExists(cjar);
        }
    }

    @Test
    public void policyStoreIsReadFromTheConfiguredDirectory() throws IOException {
        Path store = root.resolve("custom-store");
        Files.createDirectories(store.resolve("policies"));
        Files.writeString(store.resolve("metadata.json"), "{\"cedar_version\":\"4.0.0\"}");
        Files.writeString(store.resolve("schema.cedarschema"), "namespace X {}");
        Files.writeString(store.resolve("policies/only.cedar"), "permit(principal, action, resource);");
        Files.writeString(store.resolve("authorization.json"), "{}");
        Path cjar = AcmeStore.build(store, "http://localhost:1234", "http://localhost:1234/jwks");
        try (var zip = new java.util.zip.ZipFile(cjar.toFile())) {
            assertTrue(zip.getEntry("policies/only.cedar") != null);
            assertTrue(zip.getEntry("policies/deploy-staging-token.cedar") == null);
            assertTrue(zip.getEntry("authorization.json") == null);
            assertTrue(zip.getEntry("trusted-issuers.json") != null);
        } finally {
            Files.deleteIfExists(cjar);
        }
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*missing policies.*")
    public void incompletePolicyStoreDirectoryIsRejected() throws IOException {
        Path store = root.resolve("broken-store");
        Files.createDirectories(store);
        Files.writeString(store.resolve("metadata.json"), "{}");
        Files.writeString(store.resolve("schema.cedarschema"), "namespace X {}");
        AcmeStore.build(store, "http://localhost:1234", "http://localhost:1234/jwks");
    }

    @Test
    public void generatesExecutionsThroughCedarlingAndMarksAnomalies() throws IOException {
        AcmeInputs inputs = AcmeInputs.load(writeInputs(6, 3, 2));
        Path out = root.resolve("output");
        Files.createDirectories(out);

        TraceGenerator.Outcome outcome;
        try {
            outcome = TraceGenerator.generate(inputs, new LocalBootstrap(), out, root.resolve("no-policy-store"));
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new SkipException("Cedarling native library is not available: " + e);
        }

        assertEquals(outcome.executions(), 11);
        assertEquals(outcome.failed(), 0);
        assertEquals(outcome.anomalies(), 2);
        assertTrue(outcome.allowed() > 0);

        List<TraceFiles.Ref> executions = TraceFiles.readExecutions(out);
        assertEquals(executions.size(), 11);
        assertEquals(executions.stream().mapToInt(TraceFiles.Ref::expectedRecords).sum(),
                outcome.allowed() + outcome.denied());
        assertEquals(MAPPER.readTree(out.resolve("truth.json").toFile()).path("anomalies").size(), 2);
        assertTrue(Files.isRegularFile(out.resolve("run.json")));
    }

    private Path writeInputs(int train, int test, int anomalies) throws IOException {
        String text;
        try (InputStream in = getClass().getResourceAsStream("/facade/authorization.sample.json")) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        JSONObject json = new JSONObject(text);
        json.getJSONObject("traffic").put("train_executions", train).put("test_executions", test)
                .put("anomalies", anomalies).put("pause_millis", 0);
        Path file = root.resolve("authorization.json");
        Files.writeString(file, json.toString(2));
        return file;
    }

    private static final class LocalBootstrap implements TraceGenerator.Bootstrap {
        @Override
        public String runId() {
            return "localrun";
        }

        @Override
        public JSONObject bootstrap(Path policyStore, String logLevel) {
            return new JSONObject()
                    .put("CEDARLING_APPLICATION_NAME", "facade-plugin-test")
                    .put("CEDARLING_POLICY_STORE_LOCAL_FN", policyStore.toString())
                    .put("CEDARLING_LOG_TYPE", "off")
                    .put("CEDARLING_LOCK", "disabled")
                    .put("CEDARLING_TRACE", "disabled");
        }
    }
}
