package org.example;

import java.util.List;

import uniffi.cedarling_uniffi.AuthorizeResult;
import uniffi.cedarling_uniffi.Cedarling;
import uniffi.cedarling_uniffi.EntityData;
import uniffi.cedarling_uniffi.MultiIssuerAuthorizeResult;
import uniffi.cedarling_uniffi.TokenInput;

public class Benchmark {
    private static final int WARMUP_ITERS = 100;
    private static final int MEASURE_ITERS = 1000;

    private static final String UNSIGNED_CONFIG = "{"
            + "\"CEDARLING_APPLICATION_NAME\":\"BenchmarkApp\","
            + "\"CEDARLING_POLICY_STORE_LOCAL_FN\":\"../../../test_files/policy-store_ok_2.yaml\","
            + "\"CEDARLING_JWT_SIG_VALIDATION\":\"disabled\","
            + "\"CEDARLING_JWT_STATUS_VALIDATION\":\"disabled\","
            + "\"CEDARLING_LOG_TYPE\":\"off\""
            + "}";

    private static final String MULTI_ISSUER_CONFIG = "{"
            + "\"CEDARLING_APPLICATION_NAME\":\"BenchmarkApp\","
            + "\"CEDARLING_POLICY_STORE_LOCAL_FN\":\"../../../test_files/policy-store-multi-issuer-test.yaml\","
            + "\"CEDARLING_JWT_SIG_VALIDATION\":\"disabled\","
            + "\"CEDARLING_JWT_STATUS_VALIDATION\":\"disabled\","
            + "\"CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED\":[\"HS256\"],"
            + "\"CEDARLING_LOG_TYPE\":\"off\""
            + "}";

    private static final String UNSIGNED_PRINCIPAL = "{"
            + "\"cedar_entity_mapping\":{\"entity_type\":\"Jans::TestPrincipal1\",\"id\":\"test_principal_1\"},"
            + "\"is_ok\":true"
            + "}";

    private static final String RESOURCE = "{"
            + "\"cedar_entity_mapping\":{\"entity_type\":\"Jans::Issue\",\"id\":\"random_id\"},"
            + "\"org_id\":\"some_long_id\","
            + "\"country\":\"US\""
            + "}";

    private static final String JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    private static final String ACCESS_TOKEN = JWT_HEADER
            + ".eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik";
    private static final String ID_TOKEN = JWT_HEADER
            + ".eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjpbIjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSJdLCJleHAiOjE3MjQ4MzU4NTksImlhdCI6MTcyNDgzMjI1OSwic3ViIjoiYm9HOGRmYzVNS1RuMzdvN2dzZENleXFMOExwV1F0Z29PNDFtMUtad2RxMCIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsImp0aSI6InNrM1Q0ME5ZU1l1azVzYUhaTnBrWnciLCJub25jZSI6ImMzODcyYWY5LWEwZjUtNGMzZi1hMWFmLWY5ZDBlODg0NmU4MSIsInNpZCI6IjZhN2ZlNTBhLWQ4MTAtNDU0ZC1iZTVkLTU0OWQyOTU5NWEwOSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiY19oYXNoIjoicEdvSzZZX1JLY1dIa1VlY005dXc2USIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDIsInVyaSI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19LCJyb2xlIjoiQWRtaW4ifQ.RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA";
    private static final String USERINFO_TOKEN = JWT_HEADER
            + ".eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4";

    @FunctionalInterface
    interface BenchFn {
        void run() throws Exception;
    }

    private static double elapsedUs(long t0, long t1) {
        return (t1 - t0) / 1000.0;
    }

    private static void runBench(String name, BenchFn fn) throws Exception {
        for (int i = 0; i < WARMUP_ITERS; i++) {
            fn.run();
        }

        double[] samples = new double[MEASURE_ITERS];
        for (int i = 0; i < MEASURE_ITERS; i++) {
            long t0 = System.nanoTime();
            fn.run();
            long t1 = System.nanoTime();
            samples[i] = elapsedUs(t0, t1);
        }

        double sum = 0.0;
        double min = samples[0];
        double max = samples[0];
        for (double v : samples) {
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double mean = sum / MEASURE_ITERS;
        double var = 0.0;
        for (double v : samples) {
            double d = v - mean;
            var += d * d;
        }
        double stddev = Math.sqrt(var / MEASURE_ITERS);

        System.out.printf(
                "name=%s mean_us=%.1f stddev_us=%.1f min_us=%.1f max_us=%.1f%n",
                name, mean, stddev, min, max);
    }

    private static void benchAuthorizeUnsigned() throws Exception {
        Cedarling cedarling = Cedarling.Companion.loadFromJson(UNSIGNED_CONFIG);
        EntityData principal = EntityData.Companion.fromJson(UNSIGNED_PRINCIPAL);
        EntityData resource = EntityData.Companion.fromJson(RESOURCE);

        AuthorizeResult validation = cedarling.authorizeUnsigned(
                principal,
                "Jans::Action::\"UpdateForTestPrincipals\"",
                resource,
                "{}");
        if (!validation.getDecision()) {
            throw new RuntimeException("Validation failed for authorize_unsigned");
        }

        runBench("authorize_unsigned", () -> cedarling.authorizeUnsigned(
                principal,
                "Jans::Action::\"UpdateForTestPrincipals\"",
                resource,
                "{}"));
    }

    private static void benchAuthorizeMultiIssuer() throws Exception {
        Cedarling cedarling = Cedarling.Companion.loadFromJson(MULTI_ISSUER_CONFIG);
        EntityData resource = EntityData.Companion.fromJson(RESOURCE);
        List<TokenInput> tokens = List.of(
                new TokenInput("Jans::Access_Token", ACCESS_TOKEN),
                new TokenInput("Jans::Id_Token", ID_TOKEN),
                new TokenInput("Jans::Userinfo_Token", USERINFO_TOKEN));

        MultiIssuerAuthorizeResult validation = cedarling.authorizeMultiIssuer(
                tokens,
                "Jans::Action::\"Update\"",
                resource,
                "{}");
        if (!validation.getDecision()) {
            throw new RuntimeException("Validation failed for authorize_multi_issuer");
        }

        runBench("authorize_multi_issuer", () -> cedarling.authorizeMultiIssuer(
                tokens,
                "Jans::Action::\"Update\"",
                resource,
                "{}"));
    }

    public static void main(String[] args) throws Exception {
        benchAuthorizeUnsigned();
        benchAuthorizeMultiIssuer();
    }
}
