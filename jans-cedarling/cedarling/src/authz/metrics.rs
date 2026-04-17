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
                .expect("eval_times_us lock poisoned");
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
            .expect("error_counters lock poisoned");
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
    ///
    /// # Consistency
    ///
    /// [`MetricsCollector::snapshot_and_reset`] swaps/resets atomics one at a time without a
    /// global lock. A concurrent [`MetricsCollector::record_evaluation`] that lands between two
    /// swaps can cause derived invariants to be off by one within a single snapshot
    /// (e.g. `authz.requests_total` may not equal `authz.decision_allow + authz.decision_deny`).
    pub(crate) fn snapshot_and_reset(&self) -> MetricsSnapshot {
        // Compute interval duration and reset start time
        let now = Utc::now();
        let interval_secs = {
            let mut start = self
                .interval_start
                .lock()
                .expect("interval_start lock poisoned");
            let duration = now.signed_duration_since(*start);
            *start = now;
            duration.num_seconds()
        };

        let policy_stats = {
            let mut map = self
                .policy_stats
                .write()
                .expect("policy_stats lock poisoned");
            let snapshot = map
                .iter()
                .flat_map(|(id, stats)| {
                    let snap = stats.snapshot();
                    [
                        (id.clone(), snap.evaluations),
                        (format!("{id}.allow"), snap.allow_count),
                        (format!("{id}.deny"), snap.deny_count),
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
                .expect("error_counters lock poisoned");
            std::mem::take(&mut *counters)
        };

        // Compute percentiles from eval times, then clear buffer
        let times_snapshot = {
            let mut times = self.eval_times_us.write().expect("eval_time lock poisoned");
            std::mem::take(&mut *times)
        };
        let (p50, p95, p99, max_time) = compute_percentiles(times_snapshot);

        // Build operational_stats map from atomic fields
        let mut ops = HashMap::new();

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
    let idx = |pct: usize| (len - 1) * pct / 100;
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

#[cfg(test)]
mod tests {
    use super::*;

    struct TestError(&'static str);
    impl ErrorMetricKey for TestError {
        fn metric_key(&self) -> &'static str {
            self.0
        }
    }

    #[test]
    fn record_evaluation_increments_authz_counters() {
        let collector = MetricsCollector::new(5);

        collector.record_evaluation(100, Decision::Allow, false, std::iter::empty());
        collector.record_evaluation(200, Decision::Deny, true, std::iter::empty());
        collector.record_evaluation(300, Decision::Allow, false, std::iter::empty());

        let snap = collector.snapshot_and_reset();

        assert_eq!(
            snap.operational_stats.get("authz.requests_total"),
            Some(&3),
            "total requests must be 3"
        );
        assert_eq!(
            snap.operational_stats.get("authz.requests_unsigned"),
            Some(&1),
            "unsigned requests must be 1"
        );
        assert_eq!(
            snap.operational_stats.get("authz.requests_multi_issuer"),
            Some(&2),
            "multi-issuer requests must be 2"
        );
        assert_eq!(
            snap.operational_stats.get("authz.decision_allow"),
            Some(&2),
            "allow decisions must be 2"
        );
        assert_eq!(
            snap.operational_stats.get("authz.decision_deny"),
            Some(&1),
            "deny decisions must be 1"
        );
    }

    #[test]
    fn record_evaluation_updates_policy_stats() {
        let collector = MetricsCollector::new(3);

        collector.record_evaluation(
            50,
            Decision::Allow,
            false,
            vec![("policy_a", Decision::Allow), ("policy_b", Decision::Deny)].into_iter(),
        );
        collector.record_evaluation(
            60,
            Decision::Deny,
            false,
            vec![("policy_a", Decision::Allow), ("policy_b", Decision::Deny)].into_iter(),
        );

        let snap = collector.snapshot_and_reset();

        assert_eq!(
            snap.policy_stats.get("policy_a"),
            Some(&2),
            "policy_a evaluations must be 2"
        );
        assert_eq!(
            snap.policy_stats.get("policy_a.allow"),
            Some(&2),
            "policy_a allow count must be 2"
        );
        assert_eq!(
            snap.policy_stats.get("policy_b"),
            Some(&2),
            "policy_b evaluations must be 2"
        );
        assert_eq!(
            snap.policy_stats.get("policy_b.deny"),
            Some(&2),
            "policy_b deny count must be 2"
        );
    }

    #[test]
    fn record_error_aggregates_by_metric_key() {
        use crate::authz::MultiIssuerValidationError;

        let collector = MetricsCollector::new(0);

        collector.record_error(&TestError("jwt.decode_failed"));
        collector.record_error(&TestError("jwt.decode_failed"));
        collector.record_error(&TestError("data.invalid_key"));
        collector.record_error(&MultiIssuerValidationError::EmptyTokenArray);
        collector.record_error(&MultiIssuerValidationError::MissingIssuer);
        collector.increment_error("data.invalid_key");

        let snap = collector.snapshot_and_reset();

        assert_eq!(
            snap.error_counters.get("jwt.decode_failed"),
            Some(&2),
            "jwt.decode_failed count must be 2"
        );
        assert_eq!(
            snap.error_counters.get("data.invalid_key"),
            Some(&2),
            "data.invalid_key count must be 2"
        );
        assert_eq!(
            snap.error_counters.get("multi_issuer.empty_token_array"),
            Some(&1),
            "multi_issuer.empty_token_array count must be 1"
        );
        assert_eq!(
            snap.error_counters.get("multi_issuer.missing_issuer"),
            Some(&1),
            "multi_issuer.missing_issuer count must be 1"
        );
    }

    #[test]
    fn snapshot_and_reset_zeros_counters_preserves_gauges() {
        let collector = MetricsCollector::new(10);
        collector.record_evaluation(500, Decision::Allow, false, std::iter::empty());
        collector.record_cache_hit();
        collector.record_jwt_validation(true);

        let snap1 = collector.snapshot_and_reset();

        assert_eq!(
            snap1.operational_stats.get("authz.requests_total"),
            Some(&1),
            "first snapshot has 1 request"
        );
        assert_eq!(
            snap1.operational_stats.get("token_cache.hits"),
            Some(&1),
            "token_cache hit must be 1"
        );
        assert_eq!(
            snap1.operational_stats.get("instance.policy_count"),
            Some(&10),
            "policy_count gauge must be 10"
        );

        let snap2 = collector.snapshot_and_reset();
        assert_eq!(
            snap2.operational_stats.get("authz.requests_total"),
            Some(&0),
            "counters zeroed after reset"
        );
        assert_eq!(
            snap2.operational_stats.get("token_cache.hits"),
            Some(&0),
            "token_cache hits zeroed"
        );
        assert_eq!(
            snap2.operational_stats.get("instance.policy_count"),
            Some(&10),
            "policy_count gauge preserved"
        );
    }

    #[test]
    fn compute_percentiles_empty_returns_zeros() {
        let (p50, p95, p99, max) = compute_percentiles(vec![]);
        assert_eq!(p50, 0, "p50 must be 0 for empty");
        assert_eq!(p95, 0, "p95 must be 0 for empty");
        assert_eq!(p99, 0, "p99 must be 0 for empty");
        assert_eq!(max, 0, "max must be 0 for empty");
    }

    #[test]
    fn compute_percentiles_single_element() {
        let (p50, p95, p99, max) = compute_percentiles(vec![42]);
        assert_eq!(p50, 42, "p50 must be 42");
        assert_eq!(p95, 42, "p95 must be 42");
        assert_eq!(p99, 42, "p99 must be 42");
        assert_eq!(max, 42, "max must be 42");
    }

    #[test]
    fn compute_percentiles_multiple_elements() {
        let times = vec![10, 20, 30, 33, 40, 70, 49, 55, 55, 90, 110];
        let (p50, p95, p99, max) = compute_percentiles(times);
        assert_eq!(p50, 49, "p50 must be median");
        assert_eq!(p95, 90, "p95 index (10*95/100)=9");
        assert_eq!(p99, 90, "p99 index (10*99/100)=9");
        assert_eq!(max, 110, "max must be 110");
    }

    #[test]
    fn saturating_usize_to_i64_normal_value() {
        assert_eq!(
            saturating_usize_to_i64(0),
            0,
            "values must pass through unchanged"
        );
        assert_eq!(
            saturating_usize_to_i64(100),
            100,
            "values must pass through unchanged"
        );
        assert_eq!(
            saturating_usize_to_i64(usize::MAX),
            i64::MAX,
            "usize::MAX must saturate to i64::MAX",
        );
    }

    #[test]
    fn policy_stats_snapshot_fields() {
        let stats = PolicyStats::default();
        stats.record(Decision::Allow);
        stats.record(Decision::Allow);
        stats.record(Decision::Deny);

        let snap = stats.snapshot();
        assert_eq!(snap.evaluations, 3, "evaluations must be 3");
        assert_eq!(snap.allow_count, 2, "allow_count must be 2");
        assert_eq!(snap.deny_count, 1, "deny_count must be 1");
    }

    #[test]
    fn record_evaluation_clears_eval_times_after_snapshot() {
        let collector = MetricsCollector::new(0);
        collector.record_evaluation(100, Decision::Allow, false, std::iter::empty());
        collector.record_evaluation(200, Decision::Allow, false, std::iter::empty());

        let snap1 = collector.snapshot_and_reset();
        assert_eq!(
            snap1.operational_stats.get("authz.eval_time_p50_us"),
            Some(&100),
            "p50 must be computed from collected times"
        );

        let snap2 = collector.snapshot_and_reset();
        assert_eq!(
            snap2.operational_stats.get("authz.eval_time_p50_us"),
            Some(&0),
            "eval times must be cleared after snapshot"
        );
    }

    #[test]
    fn record_cache_operations() {
        let collector = MetricsCollector::new(0);
        collector.record_cache_hit();
        collector.record_cache_hit();
        collector.record_cache_miss();
        collector.record_cache_eviction(5);

        let snap = collector.snapshot_and_reset();
        assert_eq!(
            snap.operational_stats.get("token_cache.hits"),
            Some(&2),
            "cache hits must be 2"
        );
        assert_eq!(
            snap.operational_stats.get("token_cache.misses"),
            Some(&1),
            "cache misses must be 1"
        );
        assert_eq!(
            snap.operational_stats.get("token_cache.evictions"),
            Some(&5),
            "cache evictions must be 5"
        );
    }

    #[test]
    fn record_jwt_validations() {
        let collector = MetricsCollector::new(0);
        collector.record_jwt_validation(true);
        collector.record_jwt_validation(true);
        collector.record_jwt_validation(false);

        let snap = collector.snapshot_and_reset();
        assert_eq!(
            snap.operational_stats.get("jwt.validations_total"),
            Some(&3),
            "total validations must be 3"
        );
        assert_eq!(
            snap.operational_stats.get("jwt.validations_success"),
            Some(&2),
            "successful validations must be 2"
        );
        assert_eq!(
            snap.operational_stats.get("jwt.validations_failed"),
            Some(&1),
            "failed validations must be 1"
        );
    }

    #[test]
    fn record_data_operations() {
        let collector = MetricsCollector::new(0);
        collector.record_data_push();
        collector.record_data_get();
        collector.record_data_get();
        collector.record_data_remove();

        let snap = collector.snapshot_and_reset();
        assert_eq!(
            snap.operational_stats.get("data.push_ops"),
            Some(&1),
            "push ops must be 1"
        );
        assert_eq!(
            snap.operational_stats.get("data.get_ops"),
            Some(&2),
            "get ops must be 2"
        );
        assert_eq!(
            snap.operational_stats.get("data.remove_ops"),
            Some(&1),
            "remove ops must be 1"
        );
    }
}
