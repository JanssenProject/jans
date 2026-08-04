/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc.
 *
 * C cross-platform bench harness. See bindings/benchmarks/CONTRACT.md.
 * Fixtures arrive via scenarios_generated.h (built by gen_scenarios.py).
 */

#define _POSIX_C_SOURCE 200809L
#define _DEFAULT_SOURCE

#include <libgen.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "../target/include/cedarling_c.h"
#include "scenarios_generated.h"

#define CONFIG_MAX  4096
#define BINDING_NAME "c"

static void resolve_repo_root(const char *argv0, char *out, size_t out_size) {
    const char *env = getenv("CEDARLING_REPO_ROOT");
    char resolved[PATH_MAX];
    if (env != NULL && env[0] != '\0' && realpath(env, resolved) != NULL) {
        snprintf(out, out_size, "%s", resolved);
        return;
    }
    // Default: three levels up from argv[0]'s real path.
    char resolved_binary[PATH_MAX], candidate[PATH_MAX], abs[PATH_MAX];
    if (realpath(argv0, resolved_binary) != NULL) {
        snprintf(candidate, sizeof(candidate), "%s/../../..", dirname(resolved_binary));
        if (realpath(candidate, abs) != NULL) {
            snprintf(out, out_size, "%s", abs);
            return;
        }
    }
    snprintf(out, out_size, "../../..");
}

static long long elapsed_ns(struct timespec t0, struct timespec t1) {
    long long ns = (long long)(t1.tv_sec - t0.tv_sec) * 1000000000LL
                 + (long long)(t1.tv_nsec - t0.tv_nsec);
    return ns;
}

static int cmp_long_long(const void *a, const void *b) {
    long long da = *(const long long *)a;
    long long db = *(const long long *)b;
    return (da > db) - (da < db);
}

// JSON-escape `s` into `out` (truncates at out_size-1). Cedarling error
// messages can contain quotes/backslashes/newlines that would otherwise
// break the JSONL row.
static void json_escape(const char *s, char *out, size_t out_size) {
    size_t j = 0;
    if (out_size == 0) return;
    for (size_t i = 0; s != NULL && s[i] != '\0' && j + 2 < out_size; i++) {
        unsigned char c = (unsigned char)s[i];
        const char *esc = NULL;
        switch (c) {
            case '"':  esc = "\\\""; break;
            case '\\': esc = "\\\\"; break;
            case '\n': esc = "\\n";  break;
            case '\r': esc = "\\r";  break;
            case '\t': esc = "\\t";  break;
            case '\b': esc = "\\b";  break;
            case '\f': esc = "\\f";  break;
        }
        if (esc != NULL) {
            if (j + 2 >= out_size) break;
            out[j++] = esc[0];
            out[j++] = esc[1];
        } else if (c < 0x20) {
            if (j + 6 >= out_size) break;
            j += (size_t)snprintf(out + j, out_size - j, "\\u%04x", c);
        } else {
            out[j++] = (char)c;
        }
    }
    out[j] = '\0';
}

static void emit_skipped(const char *id, const char *reason) {
    char escaped[1024];
    json_escape(reason, escaped, sizeof(escaped));
    printf(
        "{\"binding\":\"%s\",\"scenario\":\"%s\",\"status\":\"skipped\",\"reason\":\"%s\"}\n",
        BINDING_NAME, id, escaped
    );
}

static void emit_ok(const char *id, long long *samples, int iter) {
    long long sum = 0;
    for (int i = 0; i < iter; i++) sum += samples[i];
    long long mean = sum / iter;

    long long *sorted = malloc(sizeof(long long) * (size_t)iter);
    if (sorted == NULL) {
        emit_skipped(id, "alloc_failed");
        return;
    }
    memcpy(sorted, samples, sizeof(long long) * (size_t)iter);
    qsort(sorted, (size_t)iter, sizeof(long long), cmp_long_long);

    long long min = sorted[0];
    long long max = sorted[iter - 1];
    long long p50 = sorted[(int)((double)iter * 0.50)];
    long long p95 = sorted[(int)((double)iter * 0.95)];
    long long p99 = sorted[(int)((double)iter * 0.99)];
    free(sorted);

    printf(
        "{\"binding\":\"%s\",\"scenario\":\"%s\",\"iter\":%d,"
        "\"mean_ns\":%lld,\"p50_ns\":%lld,\"p95_ns\":%lld,\"p99_ns\":%lld,"
        "\"min_ns\":%lld,\"max_ns\":%lld,"
        "\"allocs_per_op\":null,\"status\":\"ok\"}\n",
        BINDING_NAME, id, iter, mean, p50, p95, p99, min, max
    );
}

typedef int (*authorize_fn_t)(uint64_t, const char *, CedarlingResult *);

static authorize_fn_t pick_authorize_fn(const char *kind) {
    if (strcmp(kind, "unsigned") == 0) return cedarling_authorize_unsigned;
    if (strcmp(kind, "multi_issuer") == 0) return cedarling_authorize_multi_issuer;
    if (strcmp(kind, "unsigned_batch") == 0) return cedarling_authorize_unsigned_batch;
    if (strcmp(kind, "multi_issuer_batch") == 0) return cedarling_authorize_multi_issuer_batch;
    return NULL;
}

// All-Allow means at least one `"decision":true`, no `"decision":false`, and
// no per-item `"Err":{` envelope in the batch response.
static int response_all_allow(const char *data) {
    if (data == NULL) return 0;
    if (strstr(data, "\"decision\":true") == NULL) return 0;
    if (strstr(data, "\"decision\":false") != NULL) return 0;
    if (strstr(data, "\"Err\":{") != NULL) return 0;
    return 1;
}

static void run_scenario(const bench_scenario_t *s, const char *repo_root) {
    if (s->mock_op_required) {
        emit_skipped(s->id, "mock_op_unavailable");
        return;
    }

    char policy_path[PATH_MAX];
    snprintf(policy_path, sizeof(policy_path), "%s/%s", repo_root, s->policy_store_fn);
    char config[CONFIG_MAX];
    int n = snprintf(config, sizeof(config), s->config_template, policy_path);
    if (n < 0 || n >= (int)sizeof(config)) {
        emit_skipped(s->id, "config_buffer_too_small");
        return;
    }

    CedarlingInstanceResult ir;
    int ret = cedarling_new(config, &ir);
    if (ret != 0) {
        char reason[256];
        snprintf(reason, sizeof(reason), "init:%s",
                 ir.error_message ? ir.error_message : "unknown");
        cedarling_free_instance_result(&ir);
        emit_skipped(s->id, reason);
        return;
    }
    uint64_t id = ir.instance_id;
    cedarling_free_instance_result(&ir);

    authorize_fn_t fn = pick_authorize_fn(s->kind);
    if (fn == NULL) {
        cedarling_drop(id);
        emit_skipped(s->id, "unknown_kind");
        return;
    }


    {
        CedarlingResult vr;
        int ret = fn(id, s->request_json, &vr);
        if (ret != 0) {
            char reason[512];
            snprintf(reason, sizeof(reason), "validation_error:%s",
                     vr.error_message ? vr.error_message : "unknown");
            cedarling_free_result(&vr);
            cedarling_drop(id);
            emit_skipped(s->id, reason);
            return;
        }
        if (!response_all_allow((const char *)vr.data)) {
            cedarling_free_result(&vr);
            cedarling_drop(id);
            emit_skipped(s->id, "validation_deny");
            return;
        }
        cedarling_free_result(&vr);
    }

    for (int i = 0; i < BENCH_WARMUP_ITERS; i++) {
        CedarlingResult r;
        if (fn(id, s->request_json, &r) != 0) {
            cedarling_free_result(&r);
            cedarling_drop(id);
            emit_skipped(s->id, "warmup_loop_failed");
            return;
        }
        cedarling_free_result(&r);
    }

    /* Measured iterations. */
    long long *samples = malloc(sizeof(long long) * BENCH_MEASURE_ITERS);
    if (samples == NULL) {
        cedarling_drop(id);
        emit_skipped(s->id, "alloc_failed");
        return;
    }
    for (int i = 0; i < BENCH_MEASURE_ITERS; i++) {
        struct timespec t0, t1;
        CedarlingResult r;
        clock_gettime(CLOCK_MONOTONIC, &t0);
        ret = fn(id, s->request_json, &r);
        clock_gettime(CLOCK_MONOTONIC, &t1);
        cedarling_free_result(&r);
        if (ret != 0) {
            free(samples);
            cedarling_drop(id);
            emit_skipped(s->id, "measure_loop_failed");
            return;
        }
        samples[i] = elapsed_ns(t0, t1);
    }

    emit_ok(s->id, samples, BENCH_MEASURE_ITERS);
    free(samples);
    cedarling_drop(id);
}

int main(int argc, char **argv) {
    if (argc < 1 || argv[0] == NULL) {
        fprintf(stderr, "argv[0] missing; cannot resolve benchmark paths\n");
        return 1;
    }
    char repo_root[PATH_MAX];
    resolve_repo_root(argv[0], repo_root, sizeof(repo_root));

    cedarling_init();
    for (size_t i = 0; i < NUM_SCENARIOS; i++) {
        run_scenario(&SCENARIOS[i], repo_root);
    }
    cedarling_cleanup();
    return 0;
}
