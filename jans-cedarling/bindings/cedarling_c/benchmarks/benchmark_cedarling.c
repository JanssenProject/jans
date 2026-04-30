/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc.
 */


#define _POSIX_C_SOURCE 200809L

#include <libgen.h>
#include <limits.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "../target/include/cedarling_c.h"

#define WARMUP_ITERS  100
#define MEASURE_ITERS 1000
#define CONFIG_MAX    1024

static char UNSIGNED_CONFIG[CONFIG_MAX];
static char MULTI_ISSUER_CONFIG[CONFIG_MAX];

static void init_benchmark_configs(const char *argv0) {
    char test_files_dir[PATH_MAX] = {0};
    const char *env_test_files = getenv("CEDARLING_TEST_FILES_DIR");
    if (env_test_files != NULL && env_test_files[0] != '\0') {
        snprintf(test_files_dir, sizeof(test_files_dir), "%s", env_test_files);
    } else {
        char resolved_binary[PATH_MAX];
        if (realpath(argv0, resolved_binary) != NULL) {
            char candidate[PATH_MAX];
            snprintf(candidate, sizeof(candidate), "%s/../../../test_files", dirname(resolved_binary));
            if (realpath(candidate, test_files_dir) == NULL) {
                snprintf(test_files_dir, sizeof(test_files_dir), "%s", candidate);
            }
        } else {
            snprintf(test_files_dir, sizeof(test_files_dir), "../../../test_files");
        }
    }

    char unsigned_policy[PATH_MAX];
    char multi_issuer_policy[PATH_MAX];
    snprintf(unsigned_policy, sizeof(unsigned_policy), "%s/policy-store_ok_2.yaml", test_files_dir);
    snprintf(
        multi_issuer_policy,
        sizeof(multi_issuer_policy),
        "%s/policy-store-multi-issuer-test.yaml",
        test_files_dir
    );

    int n = snprintf(
        UNSIGNED_CONFIG,
        sizeof(UNSIGNED_CONFIG),
        "{"
        "\"CEDARLING_APPLICATION_NAME\":\"BenchmarkApp\","
        "\"CEDARLING_POLICY_STORE_ID\":\"a1bf93115de86de760ee0bea1d529b521489e5a11747\","
        "\"CEDARLING_JWT_SIG_VALIDATION\":\"disabled\","
        "\"CEDARLING_JWT_STATUS_VALIDATION\":\"disabled\","
        "\"CEDARLING_LOG_TYPE\":\"off\","
        "\"CEDARLING_POLICY_STORE_LOCAL_FN\":\"%s\""
        "}",
        unsigned_policy
    );
    if (n < 0 || n >= (int)sizeof(UNSIGNED_CONFIG)) {
        fprintf(stderr, "UNSIGNED_CONFIG buffer too small\n");
        exit(1);
    }

    n = snprintf(
        MULTI_ISSUER_CONFIG,
        sizeof(MULTI_ISSUER_CONFIG),
        "{"
        "\"CEDARLING_APPLICATION_NAME\":\"BenchmarkApp\","
        "\"CEDARLING_POLICY_STORE_ID\":\"a1bf93115de86de760ee0bea1d529b521489e5a11747\","
        "\"CEDARLING_JWT_SIG_VALIDATION\":\"disabled\","
        "\"CEDARLING_JWT_STATUS_VALIDATION\":\"disabled\","
        "\"CEDARLING_LOG_TYPE\":\"off\","
        "\"CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED\":[\"HS256\"],"
        "\"CEDARLING_POLICY_STORE_LOCAL_FN\":\"%s\""
        "}",
        multi_issuer_policy
    );
    if (n < 0 || n >= (int)sizeof(MULTI_ISSUER_CONFIG)) {
        fprintf(stderr, "MULTI_ISSUER_CONFIG buffer too small\n");
        exit(1);
    }
}



static const char *UNSIGNED_REQUEST =
    "{"
    "\"principal\":{"
        "\"cedar_entity_mapping\":{"
            "\"entity_type\":\"Jans::TestPrincipal1\","
            "\"id\":\"test_principal_1\""
        "},"
        "\"is_ok\":true"
    "},"
    "\"action\":\"Jans::Action::\\\"UpdateForTestPrincipals\\\"\","
    "\"resource\":{"
        "\"cedar_entity_mapping\":{"
            "\"entity_type\":\"Jans::Issue\","
            "\"id\":\"random_id\""
        "},"
        "\"org_id\":\"some_long_id\","
        "\"country\":\"US\""
    "},"
    "\"context\":{}"
    "}";


/* header shared by all three tokens */
#define JWT_HEADER "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"

/* clang-format off */
static const char *ACCESS_TOKEN =
    JWT_HEADER
    ".eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19"
    ".7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik";

static const char *ID_TOKEN =
    JWT_HEADER
    ".eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjpbIjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSJdLCJleHAiOjE3MjQ4MzU4NTksImlhdCI6MTcyNDgzMjI1OSwic3ViIjoiYm9HOGRmYzVNS1RuMzdvN2dzZENleXFMOExwV1F0Z29PNDFtMUtad2RxMCIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsImp0aSI6InNrM1Q0ME5ZU1l1azVzYUhaTnBrWnciLCJub25jZSI6ImMzODcyYWY5LWEwZjUtNGMzZi1hMWFmLWY5ZDBlODg0NmU4MSIsInNpZCI6IjZhN2ZlNTBhLWQ4MTAtNDU0ZC1iZTVkLTU0OWQyOTU5NWEwOSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiY19oYXNoIjoicEdvSzZZX1JLY1dIa1VlY005dXc2USIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDIsInVyaSI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19LCJyb2xlIjoiQWRtaW4ifQ"
    ".RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA";

static const char *USERINFO_TOKEN =
    JWT_HEADER
    ".eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ"
    ".t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4";


/* Multi-issuer request built at runtime to embed the JWT token strings */
#define MULTI_ISSUER_REQUEST_MAX 16384
static char MULTI_ISSUER_REQUEST[MULTI_ISSUER_REQUEST_MAX];

static void build_multi_issuer_request(void) {
    int n = snprintf(MULTI_ISSUER_REQUEST, MULTI_ISSUER_REQUEST_MAX,
        "{"
        "\"tokens\":["
            "{\"mapping\":\"Jans::Access_Token\",\"payload\":\"%s\"},"
            "{\"mapping\":\"Jans::Id_Token\",\"payload\":\"%s\"},"
            "{\"mapping\":\"Jans::Userinfo_Token\",\"payload\":\"%s\"}"
        "],"
        "\"resource\":{"
            "\"cedar_entity_mapping\":{"
                "\"entity_type\":\"Jans::Issue\","
                "\"id\":\"random_id\""
            "},"
            "\"country\":\"US\","
            "\"org_id\":\"some_long_id\""
        "},"
        "\"action\":\"Jans::Action::\\\"Update\\\"\","
        "\"context\":{}"
        "}",
        ACCESS_TOKEN, ID_TOKEN, USERINFO_TOKEN);
    if (n < 0 || n >= MULTI_ISSUER_REQUEST_MAX) {
        fprintf(stderr, "MULTI_ISSUER_REQUEST buffer too small (need %d)\n", n);
        exit(1);
    }
}


static double elapsed_us(struct timespec start, struct timespec end) {
    double ns = (double)(end.tv_sec - start.tv_sec) * 1e9
              + (double)(end.tv_nsec - start.tv_nsec);
    return ns / 1000.0;
}


typedef int (*bench_fn)(uint64_t, const char *, CedarlingResult *);

static void run_bench(const char *name, bench_fn fn, uint64_t instance_id,
                      const char *request) {
    CedarlingResult r;
    struct timespec t0, t1;
    double samples[MEASURE_ITERS];

    /* warm-up — not timed */
    for (int i = 0; i < WARMUP_ITERS; i++) {
        int ret = fn(instance_id, request, &r);
        if (ret != 0) {
            fprintf(stderr, "warmup call failed at iteration %d: %s\n", i,
                    r.error_message ? r.error_message : "unknown");
            cedarling_free_result(&r);
            exit(1);
        }
        cedarling_free_result(&r);
    }

    /* measured iterations */
    for (int i = 0; i < MEASURE_ITERS; i++) {
        clock_gettime(CLOCK_MONOTONIC, &t0);
        int ret = fn(instance_id, request, &r);
        clock_gettime(CLOCK_MONOTONIC, &t1);
        if (ret != 0) {
            fprintf(stderr, "bench call failed at iteration %d: %s\n", i,
                    r.error_message ? r.error_message : "unknown");
            cedarling_free_result(&r);
            exit(1);
        }
        cedarling_free_result(&r);
        samples[i] = elapsed_us(t0, t1);
    }

    /* compute mean / stddev / min / max */
    double sum = 0.0, min_us = samples[0], max_us = samples[0];
    for (int i = 0; i < MEASURE_ITERS; i++) {
        sum += samples[i];
        if (samples[i] < min_us) min_us = samples[i];
        if (samples[i] > max_us) max_us = samples[i];
    }
    double mean = sum / MEASURE_ITERS;

    double var = 0.0;
    for (int i = 0; i < MEASURE_ITERS; i++) {
        double d = samples[i] - mean;
        var += d * d;
    }
    double stddev = sqrt(var / MEASURE_ITERS);

    printf("name=%s mean_us=%.1f stddev_us=%.1f min_us=%.1f max_us=%.1f\n",
           name, mean, stddev, min_us, max_us);
}


static void bench_authorize_unsigned(void) {
    CedarlingInstanceResult ir;
    int ret = cedarling_new(UNSIGNED_CONFIG, &ir);
    if (ret != 0) {
        fprintf(stderr, "Failed to create unsigned instance: %s\n",
                ir.error_message ? ir.error_message : "unknown");
        cedarling_free_instance_result(&ir);
        exit(1);
    }
    uint64_t id = ir.instance_id;
    cedarling_free_instance_result(&ir);

    /* pre-measurement validation — mirrors validate_cedarling_works in Rust */
    CedarlingResult vr;
    ret = cedarling_authorize_unsigned(id, UNSIGNED_REQUEST, &vr);
    if (ret != 0 || vr.data == NULL ||
        strstr((char *)vr.data, "\"decision\":true") == NULL) {
        fprintf(stderr, "Validation failed for authorize_unsigned\n");
        cedarling_free_result(&vr);
        cedarling_drop(id);
        exit(1);
    }
    cedarling_free_result(&vr);

    run_bench("authorize_unsigned",
              (bench_fn)cedarling_authorize_unsigned, id, UNSIGNED_REQUEST);

    cedarling_drop(id);
}

static void bench_authorize_multi_issuer(void) {
    CedarlingInstanceResult ir;
    int ret = cedarling_new(MULTI_ISSUER_CONFIG, &ir);
    if (ret != 0) {
        fprintf(stderr, "Failed to create multi-issuer instance: %s\n",
                ir.error_message ? ir.error_message : "unknown");
        cedarling_free_instance_result(&ir);
        exit(1);
    }
    uint64_t id = ir.instance_id;
    cedarling_free_instance_result(&ir);

    /* pre-measurement validation */
    CedarlingResult vr;
    ret = cedarling_authorize_multi_issuer(id, MULTI_ISSUER_REQUEST, &vr);
    if (ret != 0 || vr.data == NULL ||
        strstr((char *)vr.data, "\"decision\":true") == NULL) {
        fprintf(stderr, "Validation failed for authorize_multi_issuer\n");
        cedarling_free_result(&vr);
        cedarling_drop(id);
        exit(1);
    }
    cedarling_free_result(&vr);

    run_bench("authorize_multi_issuer",
              (bench_fn)cedarling_authorize_multi_issuer, id, MULTI_ISSUER_REQUEST);

    cedarling_drop(id);
}

int main(int argc, char **argv) {
    if (argc < 1 || argv[0] == NULL) {
        fprintf(stderr, "argv[0] missing; cannot resolve benchmark paths\n");
        return 1;
    }
    init_benchmark_configs(argv[0]);
    cedarling_init();
    build_multi_issuer_request();
    bench_authorize_unsigned();
    bench_authorize_multi_issuer();
    cedarling_cleanup();
    return 0;
}
