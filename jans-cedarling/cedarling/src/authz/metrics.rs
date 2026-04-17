// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Telemetry metrics collection following the Cedarling Telemetry RFC.
//!
//! Collects three categories of metrics:
//! - `policy_stats`: per-policy evaluation counts
//! - `error_counters`: classified error counts
//! - `operational_stats`: authorization, cache, JWT, data, lock, and instance metrics
//!
//! All counters reset at each telemetry interval. Gauges reflect point-in-time snapshots.

use chrono::{DateTime, Utc};
use std::{
    collections::HashMap,
    sync::{
        Mutex, RwLock,
        atomic::{AtomicI64, Ordering},
    },
};

use crate::log::Decision;

/// Trait for error types that map to a telemetry metric key.
pub(crate) trait ErrorMetricKey {
    /// Returns the dot-separated metric key for this error variant
    /// (e.g., `"jwt.decode_failed"`, `"data.invalid_key"`).
    fn metric_key(&self) -> &'static str;
}

/// Per-policy statistics tracking evaluation outcomes
#[derive(Debug, Default)]
pub(crate) struct PolicyStats {
    /// Number of times this policy contributed to a decision
    evaluations: AtomicI64,
    /// Number of times this policy contributed to an ALLOW decision
    allow_count: AtomicI64,
    /// Number of times this policy contributed to a DENY decision
    deny_count: AtomicI64,
}

impl PolicyStats {
    fn record(&self, decision: Decision) {
        self.evaluations.fetch_add(1, Ordering::Relaxed);
        match decision {
            Decision::Allow => self.allow_count.fetch_add(1, Ordering::Relaxed),
            Decision::Deny => self.deny_count.fetch_add(1, Ordering::Relaxed),
        };
    }

    fn snapshot(&self) -> PolicyStatsSnapshot {
        PolicyStatsSnapshot {
            evaluations: self.evaluations.load(Ordering::Relaxed),
            allow_count: self.allow_count.load(Ordering::Relaxed),
            deny_count: self.deny_count.load(Ordering::Relaxed),
        }
    }
}

/// Immutable snapshot of policy statistics
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct PolicyStatsSnapshot {
    evaluations: i64,
    allow_count: i64,
    deny_count: i64,
}

/// Telemetry snapshot containing the three metric maps and interval duration.
///
/// Produced by [`MetricsCollector::snapshot_and_reset`].
#[derive(Debug, Clone)]
pub(crate) struct MetricsSnapshot {
    pub policy_stats: HashMap<String, i64>,
    pub error_counters: HashMap<String, i64>,
    pub operational_stats: HashMap<String, i64>,
    pub interval_secs: i64,
}

/// Thread-safe telemetry metrics collector.
///
/// Uses atomic operations for hot-path counters (lock-free). Cold-path maps
/// (`policy_stats`, `error_counters`) use `RwLock`. Eval time tracking uses
/// a `RwLock<Vec>` for percentile computation via sort at snapshot time.
#[derive(Debug)]
pub(crate) struct MetricsCollector {
    // -- Interval tracking --
    interval_start: Mutex<DateTime<Utc>>,

    authz_requests_total: AtomicI64,
    authz_requests_unsigned: AtomicI64,
    authz_requests_multi_issuer: AtomicI64,
    authz_decision_allow: AtomicI64,
    authz_decision_deny: AtomicI64,
    authz_errors_total: AtomicI64,

    eval_times_us: RwLock<Vec<i64>>,
    last_eval_time_us: AtomicI64,

    token_cache_hits: AtomicI64,
    token_cache_misses: AtomicI64,
    token_cache_evictions: AtomicI64,

    jwt_validations_total: AtomicI64,
    jwt_validations_success: AtomicI64,
    jwt_validations_failed: AtomicI64,

    data_push_ops: AtomicI64,
    data_get_ops: AtomicI64,
    data_remove_ops: AtomicI64,

    init_time: DateTime<Utc>,
    policy_count: AtomicI64,

    policy_stats: RwLock<HashMap<String, PolicyStats>>,
    error_counters: RwLock<HashMap<String, i64>>,
}

impl MetricsCollector {
    /// Creates a new metrics collector with the given initial policy count.
    pub(crate) fn new(initial_policy_count: usize) -> Self {
        let now = Utc::now();
        Self {
            interval_start: Mutex::new(now),
            init_time: now,
            policy_count: AtomicI64::new(saturating_usize_to_i64(initial_policy_count)),

            authz_requests_total: AtomicI64::new(0),
            authz_requests_unsigned: AtomicI64::new(0),
            authz_requests_multi_issuer: AtomicI64::new(0),
            authz_decision_allow: AtomicI64::new(0),
            authz_decision_deny: AtomicI64::new(0),
            authz_errors_total: AtomicI64::new(0),

            eval_times_us: RwLock::new(Vec::new()),
            last_eval_time_us: AtomicI64::new(0),

            token_cache_hits: AtomicI64::new(0),
            token_cache_misses: AtomicI64::new(0),
            token_cache_evictions: AtomicI64::new(0),

            jwt_validations_total: AtomicI64::new(0),
            jwt_validations_success: AtomicI64::new(0),
            jwt_validations_failed: AtomicI64::new(0),

            data_push_ops: AtomicI64::new(0),
            data_get_ops: AtomicI64::new(0),
            data_remove_ops: AtomicI64::new(0),
            policy_stats: RwLock::new(HashMap::new()),
            error_counters: RwLock::new(HashMap::new()),
        }
    }

    /// Records a completed authorization evaluation with timing and policy data.
    pub(crate) fn record_evaluation<'a>(
        &self,
        decision_time_us: i64,
        decision: Decision,
        is_unsigned: bool,
        evaluated_policies: impl Iterator<Item = (&'a str, Decision)>,
    ) {
        self.authz_requests_total.fetch_add(1, Ordering::Relaxed);

        if is_unsigned {
            self.authz_requests_unsigned.fetch_add(1, Ordering::Relaxed);
        } else {
            self.authz_requests_multi_issuer
                .fetch_add(1, Ordering::Relaxed);
        }

        match decision {
            Decision::Allow => self.authz_decision_allow.fetch_add(1, Ordering::Relaxed),
            Decision::Deny => self.authz_decision_deny.fetch_add(1, Ordering::Relaxed),
        };

        self.last_eval_time_us
            .store(decision_time_us, Ordering::Relaxed);

        {
            let mut times = self
                .eval_times_us
                .write()
                .expect("eval_times_us lock should not be poisoned");
            times.push(decision_time_us);
        }

        {
            let mut stats = self
                .policy_stats
                .write()
                .expect("policy_stats lock poisoned");
            for (policy_id, pol_decision) in evaluated_policies {
                stats
                    .entry(policy_id.to_string())
                    .or_default()
                    .record(pol_decision);
            }
        }
    }

    /// Increments `authz.errors_total` counter.
    pub(crate) fn record_authz_error(&self) {
        self.authz_errors_total.fetch_add(1, Ordering::Relaxed);
    }

    /// Increments a classified error counter using a typed error that implements [`ErrorMetricKey`]
    pub(crate) fn record_error(&self, err: &impl ErrorMetricKey) {
        self.increment_error(err.metric_key());
    }

    /// Increments a classified error counter by raw key string.
    pub(crate) fn increment_error(&self, key: &str) {
        let mut counters = self
            .error_counters
            .write()
            .expect("error_counters lock should not be poisoned");
        *counters.entry(key.to_string()).or_insert(0) += 1;
    }

    pub(crate) fn record_cache_hit(&self) {
        self.token_cache_hits.fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn record_cache_miss(&self) {
        self.token_cache_misses.fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn record_cache_eviction(&self, count: usize) {
        self.token_cache_evictions
            .fetch_add(saturating_usize_to_i64(count), Ordering::Relaxed);
    }

    pub(crate) fn record_jwt_validation(&self, success: bool) {
        self.jwt_validations_total.fetch_add(1, Ordering::Relaxed);
        if success {
            self.jwt_validations_success.fetch_add(1, Ordering::Relaxed);
        } else {
            self.jwt_validations_failed.fetch_add(1, Ordering::Relaxed);
        }
    }
    pub(crate) fn record_data_push(&self) {
        self.data_push_ops.fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn record_data_get(&self) {
        self.data_get_ops.fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn record_data_remove(&self) {
        self.data_remove_ops.fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn set_policy_count(&self, count: usize) {
        self.policy_count
            .store(saturating_usize_to_i64(count), Ordering::Relaxed);
    }

    /// Captures a snapshot of all metrics and resets counters for the next interval.
    ///
    /// Counters are zeroed. Gauges retain their current values. The eval times
    /// buffer is cleared after computing percentiles.
    pub(crate) fn snapshot_and_reset(&self) -> MetricsSnapshot {
        // Compute interval duration and reset start time
        let now = Utc::now();
        let interval_secs = {
            let mut start = self
                .interval_start
                .lock()
                .expect("interval_start lock should not be poisoned");
            let duration = now.signed_duration_since(*start);
            *start = now;
            duration.num_seconds()
        };

        let policy_stats = {
            let mut map = self
                .policy_stats
                .write()
                .expect("policy_stats lock should not be poisoned");
            let snapshot = map
                .iter()
                .flat_map(|(id, stats)| {
                    let snap = stats.snapshot();
                    [
                        (id.clone(), snap.evaluations),
                        (format!("{id}_allow"), snap.allow_count),
                        (format!("{id}_deny"), snap.deny_count),
                    ]
                })
                .collect();
            map.clear();
            snapshot
        };

        // Take and reset error_counters
        let error_counters = {
            let mut counters = self
                .error_counters
                .write()
                .expect("error_counters lock should not be poisoned");
            std::mem::take(&mut *counters)
        };

        // Compute percentiles from eval times, then clear buffer
        let times_snapshot = {
            let mut times = self.eval_times_us.write().expect("eval_time lock poisoned");
            std::mem::take(&mut *times)
        };
        let (p50, p95, p99, max_time) = compute_percentiles(times_snapshot);

        // Build operational_stats map from atomic fields
        let mut ops = HashMap::with_capacity(36);

        insert_swap(&mut ops, "authz.requests_total", &self.authz_requests_total);
        insert_swap(
            &mut ops,
            "authz.requests_unsigned",
            &self.authz_requests_unsigned,
        );
        insert_swap(
            &mut ops,
            "authz.requests_multi_issuer",
            &self.authz_requests_multi_issuer,
        );
        insert_swap(&mut ops, "authz.decision_allow", &self.authz_decision_allow);
        insert_swap(&mut ops, "authz.decision_deny", &self.authz_decision_deny);
        insert_swap(&mut ops, "authz.errors_total", &self.authz_errors_total);

        // Authorization latency (gauges, swap)
        insert_load(&mut ops, "authz.last_eval_time_us", &self.last_eval_time_us);
        insert_val(&mut ops, "authz.eval_time_p50_us", p50);
        insert_val(&mut ops, "authz.eval_time_p95_us", p95);
        insert_val(&mut ops, "authz.eval_time_p99_us", p99);
        insert_val(&mut ops, "authz.eval_time_max_us", max_time);

        // Token cache (counters swap, gauge load)
        insert_swap(&mut ops, "token_cache.hits", &self.token_cache_hits);
        insert_swap(&mut ops, "token_cache.misses", &self.token_cache_misses);
        insert_swap(
            &mut ops,
            "token_cache.evictions",
            &self.token_cache_evictions,
        );

        // JWT validation (counters swap)
        insert_swap(
            &mut ops,
            "jwt.validations_total",
            &self.jwt_validations_total,
        );
        insert_swap(
            &mut ops,
            "jwt.validations_success",
            &self.jwt_validations_success,
        );
        insert_swap(
            &mut ops,
            "jwt.validations_failed",
            &self.jwt_validations_failed,
        );

        insert_swap(&mut ops, "data.push_ops", &self.data_push_ops);
        insert_swap(&mut ops, "data.get_ops", &self.data_get_ops);
        insert_swap(&mut ops, "data.remove_ops", &self.data_remove_ops);

        insert_val(
            &mut ops,
            "instance.uptime_secs",
            now.signed_duration_since(self.init_time).num_seconds(),
        );
        insert_load(&mut ops, "instance.policy_count", &self.policy_count);

        MetricsSnapshot {
            policy_stats,
            error_counters,
            operational_stats: ops,
            interval_secs,
        }
    }
}

/// Inserts a counter value by swapping the atomic to 0.
fn insert_swap(map: &mut HashMap<String, i64>, key: &str, atomic: &AtomicI64) {
    map.insert(key.to_string(), atomic.swap(0, Ordering::Relaxed));
}

/// Inserts a gauge value by loading (not resetting) the atomic.
fn insert_load(map: &mut HashMap<String, i64>, key: &str, atomic: &AtomicI64) {
    map.insert(key.to_string(), atomic.load(Ordering::Relaxed));
}

/// Inserts a pre-computed value.
fn insert_val(map: &mut HashMap<String, i64>, key: &str, value: i64) {
    map.insert(key.to_string(), value);
}

/// Computes percentiles from a mutable slice using nearest-rank method.
/// Sorts in place. Returns `(p50, p95, p99, max)`. All zeros if empty.
fn compute_percentiles(times: Vec<i64>) -> (i64, i64, i64, i64) {
    if times.is_empty() {
        return (0, 0, 0, 0);
    }
    let mut times = times;
    times.sort_unstable();
    let len = times.len();
    let idx = |pct: usize| ((len - 1) * pct / 100).min(len - 1);
    (
        times[idx(50)],
        times[idx(95)],
        times[idx(99)],
        times[len - 1],
    )
}

fn saturating_usize_to_i64(value: usize) -> i64 {
    i64::try_from(value).unwrap_or(i64::MAX)
}
