package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uniffi.cedarling_uniffi.Cedarling;
import uniffi.cedarling_uniffi.EntityData;
import uniffi.cedarling_uniffi.TokenInput;

/**
 * Cedarling Java/UniFFI cross-platform benchmark harness.
 *
 * <p>Loads fixtures from {@code bindings/benchmarks/fixtures/scenarios.json}
 * and emits one JSONL line per scenario per the contract documented in
 * {@code bindings/benchmarks/CONTRACT.md}.
 *
 * <p>Path resolution: scenario {@code policy_store_fn} entries are
 * repository-root-relative. The harness resolves the repo root as, in order:
 * <ol>
 *   <li>{@code CEDARLING_REPO_ROOT} env var</li>
 *   <li>three levels up from the JVM working directory (typical when run
 *       via {@code mvn exec:java} from the {@code javaApp} directory)</li>
 * </ol>
 */
public class Benchmark {

    private static final String BINDING_NAME = "java";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int JVM_WARMUP_MULTIPLIER = 50;

    @FunctionalInterface
    private interface BenchFn {
        boolean run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        Path repoRoot = resolveRepoRoot();
        Path manifestPath = repoRoot.resolve(
            "bindings/benchmarks/fixtures/scenarios.json"
        );
        JsonNode manifest = MAPPER.readTree(Files.readAllBytes(manifestPath));

        JsonNode policy = manifest.path("iteration_policy");
        int warmupIters =
            policy.path("warmup_iters").asInt(100) * JVM_WARMUP_MULTIPLIER;
        int measureIters = policy.path("measure_iters").asInt(1000);

        for (JsonNode scenario : manifest.path("scenarios")) {
            runScenario(scenario, repoRoot, warmupIters, measureIters);
        }
    }

    private static Path resolveRepoRoot() {
        String envRoot = System.getenv("CEDARLING_REPO_ROOT");
        if (envRoot != null && !envRoot.isEmpty()) {
            return Path.of(envRoot).toAbsolutePath().normalize();
        }
        // Default: assume the bench is invoked from the javaApp directory
        // (e.g., `mvn -Dexec.mainClass=org.example.Benchmark ...`).
        return Path.of(System.getProperty("user.dir"))
            .resolve("../../..")
            .toAbsolutePath()
            .normalize();
    }

    private static void runScenario(
        JsonNode scenario,
        Path repoRoot,
        int warmupIters,
        int measureIters
    ) {
        String id = scenario.path("id").asText();

        if (scenario.path("mock_op_required").asBoolean(false)) {
            emitSkipped(id, "mock_op_unavailable");
            return;
        }

        try {
            String configJson = buildConfigJson(scenario, repoRoot);
            Cedarling cedarling = Cedarling.Companion.loadFromJson(configJson);

            String kind = scenario.path("kind").asText();
            BenchFn fn;
            switch (kind) {
                case "unsigned":
                    fn = buildUnsignedFn(cedarling, scenario);
                    break;
                case "multi_issuer":
                    fn = buildMultiIssuerFn(cedarling, scenario);
                    break;
                default:
                    emitSkipped(id, "unknown_kind:" + kind);
                    return;
            }

            // One pre-warmup validation call: catches misconfigured fixtures
            // that authorize cleanly but return Deny (which would otherwise be
            // timed silently for measureIters iterations).
            if (!fn.run()) {
                emitSkipped(id, "validation_deny");
                return;
            }

            for (int i = 0; i < warmupIters; i++) {
                fn.run();
            }

            long[] samples = new long[measureIters];
            for (int i = 0; i < measureIters; i++) {
                long t0 = System.nanoTime();
                fn.run();
                long t1 = System.nanoTime();
                samples[i] = t1 - t0;
            }
            emitOk(id, samples);
        } catch (Exception e) {
            emitSkipped(
                id,
                "error:" + e.getClass().getSimpleName() + ":" + e.getMessage()
            );
        }
    }

    private static String buildConfigJson(JsonNode scenario, Path repoRoot) {
        ObjectNode config = MAPPER.createObjectNode();
        JsonNode overrides = scenario.path("config_overrides");
        if (overrides.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = overrides.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                config.set(e.getKey(), e.getValue());
            }
        }
        Path policyStorePath = repoRoot.resolve(
            scenario.path("policy_store_fn").asText()
        );
        config.put(
            "CEDARLING_POLICY_STORE_LOCAL_FN",
            policyStorePath.toString()
        );
        return config.toString();
    }

    private static BenchFn buildUnsignedFn(
        Cedarling cedarling,
        JsonNode scenario
    ) throws Exception {
        JsonNode principalNode = scenario.path("principal");
        EntityData principal =
            principalNode.isNull() || principalNode.isMissingNode()
                ? null
                : EntityData.Companion.fromJson(principalNode.toString());

        String action = scenario.path("action").asText();
        EntityData resource = EntityData.Companion.fromJson(
            scenario.path("resource").toString()
        );
        String context = scenario.path("context").asText("{}");

        return () ->
            cedarling
                .authorizeUnsigned(principal, action, resource, context)
                .getDecision();
    }

    private static BenchFn buildMultiIssuerFn(
        Cedarling cedarling,
        JsonNode scenario
    ) throws Exception {
        List<TokenInput> tokens = new ArrayList<>();
        for (JsonNode t : scenario.path("tokens")) {
            tokens.add(
                new TokenInput(
                    t.path("mapping").asText(),
                    t.path("payload").asText()
                )
            );
        }
        String action = scenario.path("action").asText();
        EntityData resource = EntityData.Companion.fromJson(
            scenario.path("resource").toString()
        );
        String context = scenario.path("context").asText("{}");

        return () ->
            cedarling
                .authorizeMultiIssuer(tokens, action, resource, context)
                .getDecision();
    }

    private static void emitOk(String id, long[] samples) {
        long[] sorted = samples.clone();
        Arrays.sort(sorted);

        long sum = 0;
        for (long v : samples) {
            sum += v;
        }
        long mean = sum / samples.length;
        long min = sorted[0];
        long max = sorted[sorted.length - 1];
        long p50 = sorted[(int) (samples.length * 0.50)];
        long p95 = sorted[(int) (samples.length * 0.95)];
        long p99 = sorted[(int) (samples.length * 0.99)];

        Map<String, Object> row = new HashMap<>();
        row.put("binding", BINDING_NAME);
        row.put("scenario", id);
        row.put("iter", samples.length);
        row.put("mean_ns", mean);
        row.put("p50_ns", p50);
        row.put("p95_ns", p95);
        row.put("p99_ns", p99);
        row.put("min_ns", min);
        row.put("max_ns", max);
        row.put("allocs_per_op", null);
        row.put("status", "ok");
        try {
            System.out.println(MAPPER.writeValueAsString(row));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void emitSkipped(String id, String reason) {
        Map<String, Object> row = new HashMap<>();
        row.put("binding", BINDING_NAME);
        row.put("scenario", id);
        row.put("status", "skipped");
        row.put("reason", reason);
        try {
            System.out.println(MAPPER.writeValueAsString(row));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
