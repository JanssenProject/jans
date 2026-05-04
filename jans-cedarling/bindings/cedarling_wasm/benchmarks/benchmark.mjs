// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2025, Gluu, Inc.

import { performance } from "node:perf_hooks";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const require = createRequire(import.meta.url);
const fs = require("node:fs");
const path = require("node:path");
const cedarlingWasm = require("../pkg/cedarling_wasm.js");
const { init } = cedarlingWasm;

const WARMUP_ITERS = 100;
const MEASURE_ITERS = 1000;
const THIS_DIR = path.dirname(fileURLToPath(import.meta.url));
const TEST_FILES_DIR = path.resolve(THIS_DIR, "../../../test_files");

function yamlToJsonString(yamlPath) {
  const script = `
import json, pathlib, yaml
path = pathlib.Path(r'''${yamlPath}''')
print(json.dumps(yaml.safe_load(path.read_text()), separators=(',',':')))
`;
  return execFileSync("python3", ["-c", script], { encoding: "utf8" }).trim();
}

const UNSIGNED_POLICY_STORE = yamlToJsonString(
  path.join(TEST_FILES_DIR, "policy-store_ok_2.yaml")
);
const MULTI_ISSUER_POLICY_STORE = yamlToJsonString(
  path.join(TEST_FILES_DIR, "policy-store-multi-issuer-test.yaml")
);

function makeUnsignedConfig() {
  return {
    CEDARLING_APPLICATION_NAME: "BenchmarkApp",
    CEDARLING_POLICY_STORE_LOCAL: UNSIGNED_POLICY_STORE,
    CEDARLING_JWT_SIG_VALIDATION: "disabled",
    CEDARLING_JWT_STATUS_VALIDATION: "disabled",
    CEDARLING_LOG_TYPE: "off",
  };
}

function makeMultiIssuerConfig() {
  return {
    CEDARLING_APPLICATION_NAME: "BenchmarkApp",
    CEDARLING_POLICY_STORE_LOCAL: MULTI_ISSUER_POLICY_STORE,
    CEDARLING_JWT_SIG_VALIDATION: "disabled",
    CEDARLING_JWT_STATUS_VALIDATION: "disabled",
    CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED: ["HS256"],
    CEDARLING_LOG_TYPE: "off",
  };
}

const UNSIGNED_REQUEST = {
  principal: {
    cedar_entity_mapping: {
      entity_type: "Jans::TestPrincipal1",
      id: "test_principal_1",
    },
    is_ok: true,
  },
  action: 'Jans::Action::"UpdateForTestPrincipals"',
  resource: {
    cedar_entity_mapping: {
      entity_type: "Jans::Issue",
      id: "random_id",
    },
    org_id: "some_long_id",
    country: "US",
  },
  context: {},
};

const JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
const ACCESS_TOKEN =
  JWT_HEADER +
  ".eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik";
const ID_TOKEN =
  JWT_HEADER +
  ".eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjpbIjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSJdLCJleHAiOjE3MjQ4MzU4NTksImlhdCI6MTcyNDgzMjI1OSwic3ViIjoiYm9HOGRmYzVNS1RuMzdvN2dzZENleXFMOExwV1F0Z29PNDFtMUtad2RxMCIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsImp0aSI6InNrM1Q0ME5ZU1l1azVzYUhaTnBrWnciLCJub25jZSI6ImMzODcyYWY5LWEwZjUtNGMzZi1hMWFmLWY5ZDBlODg0NmU4MSIsInNpZCI6IjZhN2ZlNTBhLWQ4MTAtNDU0ZC1iZTVkLTU0OWQyOTU5NWEwOSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiY19oYXNoIjoicEdvSzZZX1JLY1dIa1VlY005dXc2USIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDIsInVyaSI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19LCJyb2xlIjoiQWRtaW4ifQ.RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA";
const USERINFO_TOKEN =
  JWT_HEADER +
  ".eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4";

const MULTI_ISSUER_REQUEST = {
  tokens: [
    { mapping: "Jans::Access_Token", payload: ACCESS_TOKEN },
    { mapping: "Jans::Id_Token", payload: ID_TOKEN },
    { mapping: "Jans::Userinfo_Token", payload: USERINFO_TOKEN },
  ],
  action: 'Jans::Action::"Update"',
  resource: {
    cedar_entity_mapping: {
      entity_type: "Jans::Issue",
      id: "random_id",
    },
    org_id: "some_long_id",
    country: "US",
  },
  context: {},
};

function stats(samples) {
  const sum = samples.reduce((acc, x) => acc + x, 0);
  const mean = sum / samples.length;
  const variance = samples.reduce((acc, x) => {
    const d = x - mean;
    return acc + d * d;
  }, 0) / samples.length;

  return {
    mean,
    stddev: Math.sqrt(variance),
    min: Math.min(...samples),
    max: Math.max(...samples),
  };
}

async function runBench(name, fn) {
  for (let i = 0; i < WARMUP_ITERS; i += 1) {
    await fn();
  }

  const samplesUs = [];
  for (let i = 0; i < MEASURE_ITERS; i += 1) {
    const t0 = performance.now();
    await fn();
    const t1 = performance.now();
    samplesUs.push((t1 - t0) * 1000.0);
  }

  const s = stats(samplesUs);
  console.log(
    `name=${name} mean_us=${s.mean.toFixed(1)} stddev_us=${s.stddev.toFixed(1)} min_us=${s.min.toFixed(1)} max_us=${s.max.toFixed(1)}`
  );
}

async function benchAuthorizeUnsigned() {
  const cedarling = await init(makeUnsignedConfig());
  const validation = await cedarling.authorize_unsigned(UNSIGNED_REQUEST);
  if (!validation.decision) {
    throw new Error("Validation failed for authorize_unsigned");
  }

  await runBench("authorize_unsigned", async () => {
    await cedarling.authorize_unsigned(UNSIGNED_REQUEST);
  });
}

async function benchAuthorizeMultiIssuer() {
  const cedarling = await init(makeMultiIssuerConfig());
  const validation = await cedarling.authorize_multi_issuer(MULTI_ISSUER_REQUEST);
  if (!validation.decision) {
    throw new Error("Validation failed for authorize_multi_issuer");
  }

  await runBench("authorize_multi_issuer", async () => {
    await cedarling.authorize_multi_issuer(MULTI_ISSUER_REQUEST);
  });
}

async function main() {
  await benchAuthorizeUnsigned();
  await benchAuthorizeMultiIssuer();
}

main().catch((err) => {
  console.error(`benchmark failed: ${err.message ?? err}`);
  process.exit(1);
});
