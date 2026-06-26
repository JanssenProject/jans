// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2025, Gluu, Inc.
//
// WASM cross-platform bench harness. See bindings/benchmarks/CONTRACT.md.
// YAML→JSON conversion done via a Python subprocess to avoid a js-yaml dep.

import { performance } from "node:perf_hooks";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const require = createRequire(import.meta.url);
const fs = require("node:fs");
const path = require("node:path");
const cedarlingWasm = require("../pkg/cedarling_wasm.js");
const { init } = cedarlingWasm;

const BINDING_NAME = "wasm";

function resolveRepoRoot() {
  const env = process.env.CEDARLING_REPO_ROOT;
  if (env && env.length > 0) return path.resolve(env);
  // Default: bench file is in bindings/cedarling_wasm/benchmarks/ ⇒ ../../..
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");
}

function yamlFileToJsonString(yamlPath) {
  const script = `
import json, pathlib, yaml
print(json.dumps(yaml.safe_load(pathlib.Path(r'''${yamlPath}''').read_text()), separators=(',',':')))
`;
  return execFileSync("python3", ["-c", script], { encoding: "utf8" }).trim();
}

function emit(row) {
  process.stdout.write(JSON.stringify(row) + "\n");
}

function buildConfig(scenario, repoRoot) {
  // WASM accepts the policy store inline; convert YAML on the host.
  const policyStorePath = path.join(repoRoot, scenario.policy_store_fn);
  return {
    ...(scenario.config_overrides || {}),
    CEDARLING_POLICY_STORE_LOCAL: yamlFileToJsonString(policyStorePath),
  };
}

function buildRequest(scenario) {
  // Manifest carries context as JSON-as-string; WASM API expects an object.
  const ctxStr = scenario.context || "{}";
  const ctx = ctxStr === "" ? {} : JSON.parse(ctxStr);

  if (scenario.kind === "unsigned") {
    const req = {
      action: scenario.action,
      resource: scenario.resource,
      context: ctx,
    };
    if (scenario.principal) {
      req.principal = scenario.principal;
    }
    return req;
  }
  if (scenario.kind === "multi_issuer") {
    return {
      tokens: scenario.tokens || [],
      action: scenario.action,
      resource: scenario.resource,
      context: ctx,
    };
  }
  throw new Error(`unknown scenario kind: ${scenario.kind}`);
}

function percentile(sorted, frac) {
  return sorted[Math.floor(sorted.length * frac)];
}

async function runScenario(scenario, repoRoot, warmupIters, measureIters) {
  const sid = scenario.id;
  if (scenario.mock_op_required) {
    emit({ binding: BINDING_NAME, scenario: sid, status: "skipped", reason: "mock_op_unavailable" });
    return;
  }

  let cedarling;
  let request;
  let invoke;
  try {
    const config = buildConfig(scenario, repoRoot);
    cedarling = await init(config);
    request = buildRequest(scenario);
    invoke = scenario.kind === "unsigned"
      ? () => cedarling.authorize_unsigned(request)
      : () => cedarling.authorize_multi_issuer(request);
    await invoke(); // pre-measurement; fixture errors surface as a skip

  } catch (err) {
    emit({
      binding: BINDING_NAME,
      scenario: sid,
      status: "skipped",
      reason: `error:${err.name || "Error"}:${err.message || err}`,
    });
    return;
  }

  for (let i = 0; i < warmupIters; i += 1) {
    await invoke();
  }

  // performance.now() returns ms with sub-µs resolution; *1e6 → integer ns.
  const samplesNs = new Array(measureIters);
  for (let i = 0; i < measureIters; i += 1) {
    const t0 = performance.now();
    await invoke();
    const t1 = performance.now();
    samplesNs[i] = Math.round((t1 - t0) * 1e6);
  }

  const sorted = samplesNs.slice().sort((a, b) => a - b);
  const sum = samplesNs.reduce((acc, v) => acc + v, 0);
  emit({
    binding: BINDING_NAME,
    scenario: sid,
    iter: measureIters,
    mean_ns: Math.round(sum / measureIters),
    p50_ns: percentile(sorted, 0.50),
    p95_ns: percentile(sorted, 0.95),
    p99_ns: percentile(sorted, 0.99),
    min_ns: sorted[0],
    max_ns: sorted[sorted.length - 1],
    allocs_per_op: null,
    status: "ok",
  });
}

async function main() {
  const repoRoot = resolveRepoRoot();
  const manifestPath = path.join(repoRoot, "bindings", "benchmarks", "fixtures", "scenarios.json");
  const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
  const policy = manifest.iteration_policy || {};
  const warmupIters = policy.warmup_iters ?? 100;
  const measureIters = policy.measure_iters ?? 1000;

  for (const scenario of manifest.scenarios || []) {
    await runScenario(scenario, repoRoot, warmupIters, measureIters);
  }
}

main().catch((err) => {
  process.stderr.write(`benchmark failed: ${err.message ?? err}\n`);
  process.exit(1);
});
