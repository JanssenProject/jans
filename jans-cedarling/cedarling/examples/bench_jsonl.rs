// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cross-platform benchmark harness for the Rust binding.
//!
//! Reads the shared manifest at `bindings/benchmarks/fixtures/scenarios.json`
//! and emits one canonical JSONL record per scenario on stdout (logging on
//! stderr). See `bindings/benchmarks/CONTRACT.md`. Mirrors the Go harness at
//! `bindings/cedarling_go/benchmarks/main.go`.
//!
//! Run from the `cedarling` crate dir:
//!   cargo run --release --example `bench_jsonl`

// Percentile/mean math casts between usize/f64/i64 by design; the values are
// wall-clock nanosecond counts well within range.
#![allow(
    clippy::cast_possible_truncation,
    clippy::cast_sign_loss,
    clippy::cast_precision_loss,
    clippy::cast_possible_wrap
)]

use std::path::{Path, PathBuf};
use std::time::Instant;

use cedarling::{
    AuthorizeMultiIssuerRequest, BatchAuthorizeMultiIssuerRequest, BatchAuthorizeUnsignedRequest,
    BatchItem, BootstrapConfig, Cedarling, EntityData, RequestUnsigned, TokenInput,
};
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};
use tokio::runtime::Runtime;

const BINDING: &str = "rust";

#[derive(Deserialize)]
struct Manifest {
    iteration_policy: IterationPolicy,
    scenarios: Vec<Scenario>,
}

#[derive(Deserialize)]
struct IterationPolicy {
    warmup_iters: usize,
    measure_iters: usize,
}

#[derive(Deserialize)]
struct Scenario {
    id: String,
    kind: String,
    #[serde(default)]
    item_count: usize,
    policy_store_fn: String,
    #[serde(default)]
    config_overrides: Map<String, Value>,
    #[serde(default)]
    principal: Option<Value>,
    #[serde(default)]
    action: String,
    resource: Value,
    #[serde(default)]
    context: String,
    #[serde(default)]
    tokens: Vec<TokenSpec>,
    #[serde(default)]
    mock_op_required: bool,
}

#[derive(Deserialize)]
struct TokenSpec {
    mapping: String,
    payload: String,
}

#[derive(Serialize)]
struct BenchResult {
    binding: &'static str,
    scenario: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    iter: Option<usize>,
    #[serde(skip_serializing_if = "Option::is_none")]
    mean_ns: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    p50_ns: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    p95_ns: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    p99_ns: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    min_ns: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    max_ns: Option<i64>,
    // Always present, even when null — Rust does not measure per-op allocs.
    allocs_per_op: Option<f64>,
    status: &'static str,
    #[serde(skip_serializing_if = "Option::is_none")]
    reason: Option<String>,
}

impl BenchResult {
    fn skipped(scenario: String, reason: String) -> Self {
        Self {
            binding: BINDING,
            scenario,
            iter: None,
            mean_ns: None,
            p50_ns: None,
            p95_ns: None,
            p99_ns: None,
            min_ns: None,
            max_ns: None,
            allocs_per_op: None,
            status: "skipped",
            reason: Some(reason),
        }
    }
}

fn emit(result: &BenchResult) {
    match serde_json::to_string(result) {
        Ok(line) => println!("{line}"),
        Err(e) => eprintln!("marshal failed: {e}"),
    }
}

fn repo_root() -> PathBuf {
    // CARGO_MANIFEST_DIR = <repo_root>/cedarling; repo root is its parent.
    let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    manifest_dir
        .parent()
        .map(Path::to_path_buf)
        .unwrap_or(manifest_dir)
}

fn main() {
    let root = repo_root();
    let manifest_path = root
        .join("bindings")
        .join("benchmarks")
        .join("fixtures")
        .join("scenarios.json");

    let raw = match std::fs::read_to_string(&manifest_path) {
        Ok(raw) => raw,
        Err(e) => {
            eprintln!("failed to read manifest {}: {e}", manifest_path.display());
            std::process::exit(1);
        },
    };
    let manifest: Manifest = match serde_json::from_str(&raw) {
        Ok(m) => m,
        Err(e) => {
            eprintln!("failed to parse manifest: {e}");
            std::process::exit(1);
        },
    };

    let runtime = match Runtime::new() {
        Ok(rt) => rt,
        Err(e) => {
            eprintln!("failed to init tokio runtime: {e}");
            std::process::exit(1);
        },
    };

    for scenario in &manifest.scenarios {
        let result = run_scenario(
            &runtime,
            scenario,
            &root,
            manifest.iteration_policy.warmup_iters,
            manifest.iteration_policy.measure_iters,
        );
        emit(&result);
    }
}

/// A closure that runs one authorization call and yields whether it Allowed.
type BenchFn<'a> = Box<dyn Fn() -> Result<bool, String> + 'a>;

fn run_scenario(
    runtime: &Runtime,
    scenario: &Scenario,
    root: &Path,
    warmup_iters: usize,
    measure_iters: usize,
) -> BenchResult {
    if scenario.mock_op_required {
        return BenchResult::skipped(scenario.id.clone(), "mock_op_unavailable".to_string());
    }

    let cedarling = match build_cedarling(runtime, scenario, root) {
        Ok(c) => c,
        Err(e) => return BenchResult::skipped(scenario.id.clone(), format!("init:{e}")),
    };

    let bench_fn = match build_bench_fn(runtime, &cedarling, scenario) {
        Ok(f) => f,
        Err(e) => return BenchResult::skipped(scenario.id.clone(), format!("build_fn:{e}")),
    };

    // One validation call before timing: an error or a Deny means skip.
    match bench_fn() {
        Ok(true) => {},
        Ok(false) => {
            return BenchResult::skipped(scenario.id.clone(), "validation_deny".to_string());
        },
        Err(e) => {
            return BenchResult::skipped(scenario.id.clone(), format!("validation_error:{e}"));
        },
    }

    for _ in 0..warmup_iters {
        if let Err(e) = bench_fn() {
            return BenchResult::skipped(scenario.id.clone(), format!("warmup_loop:{e}"));
        }
    }

    let mut samples: Vec<i64> = Vec::with_capacity(measure_iters);
    for _ in 0..measure_iters {
        let t0 = Instant::now();
        let outcome = bench_fn();
        let elapsed = t0.elapsed().as_nanos() as i64;
        if let Err(e) = outcome {
            return BenchResult::skipped(scenario.id.clone(), format!("measure_loop:{e}"));
        }
        samples.push(elapsed);
    }

    if samples.is_empty() {
        return BenchResult::skipped(scenario.id.clone(), "no_samples".to_string());
    }
    build_ok_result(scenario.id.clone(), &samples)
}

fn build_cedarling(
    runtime: &Runtime,
    scenario: &Scenario,
    root: &Path,
) -> Result<Cedarling, String> {
    let mut obj = scenario.config_overrides.clone();
    let policy_path = root.join(&scenario.policy_store_fn);
    obj.insert(
        "CEDARLING_POLICY_STORE_LOCAL_FN".to_string(),
        Value::String(policy_path.to_string_lossy().into_owned()),
    );
    let json = serde_json::to_string(&obj).map_err(|e| e.to_string())?;
    let cfg = BootstrapConfig::load_from_json(&json).map_err(|e| e.to_string())?;
    runtime
        .block_on(Cedarling::new(&cfg))
        .map_err(|e| e.to_string())
}

fn parse_context(ctx: &str) -> Result<Value, String> {
    if ctx.is_empty() || ctx == "{}" {
        return Ok(Value::Object(Map::new()));
    }
    serde_json::from_str(ctx).map_err(|e| e.to_string())
}

fn entity_from_value(v: &Value) -> Result<EntityData, String> {
    EntityData::from_json(&v.to_string()).map_err(|e| e.to_string())
}

fn build_bench_fn<'a>(
    runtime: &'a Runtime,
    cedarling: &'a Cedarling,
    scenario: &Scenario,
) -> Result<BenchFn<'a>, String> {
    let tokens: Vec<TokenInput> = scenario
        .tokens
        .iter()
        .map(|t| TokenInput::new(t.mapping.clone(), t.payload.clone()))
        .collect();
    let context = parse_context(&scenario.context)?;

    match scenario.kind.as_str() {
        "unsigned" => {
            let principal = match &scenario.principal {
                Some(p) => Some(entity_from_value(p)?),
                None => None,
            };
            let resource = entity_from_value(&scenario.resource)?;
            let req = RequestUnsigned {
                principal,
                action: scenario.action.clone(),
                resource,
                context,
            };
            Ok(Box::new(move || {
                runtime
                    .block_on(cedarling.authorize_unsigned(req.clone()))
                    .map(|r| r.decision)
                    .map_err(|e| e.to_string())
            }))
        },
        "multi_issuer" => {
            let resource = entity_from_value(&scenario.resource)?;
            let req = AuthorizeMultiIssuerRequest::new_with_fields(
                tokens,
                resource,
                scenario.action.clone(),
                Some(context),
            );
            Ok(Box::new(move || {
                runtime
                    .block_on(cedarling.authorize_multi_issuer(req.clone()))
                    .map(|r| r.decision)
                    .map_err(|e| e.to_string())
            }))
        },
        "unsigned_batch" => {
            if scenario.item_count == 0 {
                return Err("item_count must be > 0 for batch scenario".to_string());
            }
            let principal = match &scenario.principal {
                Some(p) => Some(entity_from_value(p)?),
                None => None,
            };
            let base = entity_from_value(&scenario.resource)?;
            let items = build_unsigned_batch_items(
                &base,
                &scenario.action,
                &context,
                scenario.item_count,
            );
            let req = BatchAuthorizeUnsignedRequest::new(principal, items);
            Ok(Box::new(move || {
                match runtime.block_on(cedarling.authorize_unsigned_batch(req.clone())) {
                    Ok(resp) => Ok(!resp.results.is_empty()
                        && resp
                            .results
                            .iter()
                            .all(|r| r.as_ref().is_ok_and(|res| res.decision))),
                    Err(e) => Err(e.to_string()),
                }
            }))
        },
        "multi_issuer_batch" => {
            if scenario.item_count == 0 {
                return Err("item_count must be > 0 for batch scenario".to_string());
            }
            let base = entity_from_value(&scenario.resource)?;
            // Multi-issuer fixture scopes to one resource — every item reuses it.
            let item = BatchItem {
                resource: base,
                action: scenario.action.clone(),
                context: context.clone(),
            };
            let items: Vec<BatchItem> = (0..scenario.item_count).map(|_| item.clone()).collect();
            let req = BatchAuthorizeMultiIssuerRequest::new(tokens, items);
            Ok(Box::new(move || {
                match runtime.block_on(cedarling.authorize_multi_issuer_batch(req.clone())) {
                    Ok(resp) => Ok(!resp.results.is_empty()
                        && resp
                            .results
                            .iter()
                            .all(|r| r.as_ref().is_ok_and(|res| res.decision))),
                    Err(e) => Err(e.to_string()),
                }
            }))
        },
        other => Err(format!("unknown scenario kind {other:?}")),
    }
}

/// Clone the fixture resource `count` times with distinct ids `{base}-{i}`.
fn build_unsigned_batch_items(
    base: &EntityData,
    action: &str,
    context: &Value,
    count: usize,
) -> Vec<BatchItem> {
    let base_id = base.cedar_mapping.id.clone();
    (0..count)
        .map(|i| {
            let mut resource = base.clone();
            resource.cedar_mapping.id = format!("{base_id}-{i}");
            BatchItem {
                resource,
                action: action.to_string(),
                context: context.clone(),
            }
        })
        .collect()
}

fn build_ok_result(scenario: String, samples: &[i64]) -> BenchResult {
    let iter = samples.len();
    let mut sorted = samples.to_vec();
    sorted.sort_unstable();

    let sum: i64 = samples.iter().sum();
    let mean = sum / iter as i64;
    let min = sorted[0];
    let max = sorted[iter - 1];
    let pct = |p: f64| sorted[(iter as f64 * p) as usize];

    BenchResult {
        binding: BINDING,
        scenario,
        iter: Some(iter),
        mean_ns: Some(mean),
        p50_ns: Some(pct(0.50)),
        p95_ns: Some(pct(0.95)),
        p99_ns: Some(pct(0.99)),
        min_ns: Some(min),
        max_ns: Some(max),
        allocs_per_op: None,
        status: "ok",
        reason: None,
    }
}
