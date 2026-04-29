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
use hdrhistogram::Histogram;
use std::{
    collections::HashMap,
    sync::{
        Mutex, RwLock,
        atomic::{AtomicI64, Ordering},
    },
};

use crate::{authz::error_metrics::ErrorMetricKey, log::Decision};

const INTERVAL_LOCK_POISONED: &str = "interval lock poisoned";
const TIMING_LOCK_POISONED: &str = "timing recorder lock poisoned";
const POLICY_STATS_READ_LOCK_POISONED: &str = "policy_stats read lock poisoned";
const POLICY_STATS_WRITE_LOCK_POISONED: &str = "policy_stats write lock poisoned";
const ERROR_COUNTERS_LOCK_POISONED: &str = "error_counters lock poisoned";

/// Timing statistics using HDR histogram
#[derive(Debug)]
struct TimingRecorder {
    histogram: Histogram<u64>,
}

impl TimingRecorder {
    fn new() -> Self {
        Self {
            // 3 significant figures gives <0.1% error across the full range of
            // representable values. At microsecond granularity this is more than enough
            histogram: Histogram::new(3).expect("HDR histogram config valid"),
        }
    }

    /// Record eval time in microseconds
    fn record(&mut self, value: i64) {
        if value > 0 {
            let _ = self.histogram.record(value.cast_unsigned());
        }
    }

    /// Drain and compute percentiles, reset for next interval
    fn drain_and_compute(&mut self) -> (i64, i64, i64, i64) {
        if self.histogram.is_empty() {
            (0, 0, 0, 0)
        } else {
            let result = (
                self.histogram.value_at_quantile(0.50).cast_signed(),
                self.histogram.value_at_quantile(0.95).cast_signed(),
                self.histogram.value_at_quantile(0.99).cast_signed(),
                self.histogram.max().cast_signed(),
            );
            self.histogram.clear();
            result
        }
    }
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

/// All state that resets at each telemetry interval.
///
/// Swapped atomically in [`MetricsCollector::snapshot_and_reset`], so any new
/// counter added here is guaranteed to be reset — no manual zeroing required.
#[derive(Debug)]
struct IntervalState {
    start: DateTime<Utc>,

    authz_requests_total: AtomicI64,
    authz_requests_unsigned: AtomicI64,
    authz_requests_multi_issuer: AtomicI64,
    authz_decision_allow: AtomicI64,
    authz_decision_deny: AtomicI64,
    authz_errors_total: AtomicI64,

    timing_recorder: Mutex<TimingRecorder>,
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

    policy_stats: RwLock<HashMap<String, PolicyStats>>,
    error_counters: RwLock<HashMap<String, i64>>,
}

impl IntervalState {
    fn new(start: DateTime<Utc>) -> Self {
        Self {
            start,
            authz_requests_total: AtomicI64::new(0),
            authz_requests_unsigned: AtomicI64::new(0),
            authz_requests_multi_issuer: AtomicI64::new(0),
            authz_decision_allow: AtomicI64::new(0),
            authz_decision_deny: AtomicI64::new(0),
            authz_errors_total: AtomicI64::new(0),
            timing_recorder: Mutex::new(TimingRecorder::new()),
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

    /// Builds the `operational_stats` map from all counters in this interval.
    ///
    /// Accepts pre-computed values that require context outside `IntervalState`
    /// (eval-time percentiles, uptime, policy count).
    fn to_operational_stats(
        &self,
        now: DateTime<Utc>,
        init_time: DateTime<Utc>,
        policy_count_atomic: &AtomicI64,
    ) -> HashMap<String, i64> {
        let (p50, p95, p99, max_time) = self
            .timing_recorder
            .lock()
            .expect(TIMING_LOCK_POISONED)
            .drain_and_compute();
        let uptime_secs = now.signed_duration_since(init_time).num_seconds();
        let policy_count = policy_count_atomic.load(Ordering::Relaxed);

        let load = |a: &AtomicI64| a.load(Ordering::Relaxed);
        HashMap::from([
            (
                "authz.requests_total".to_string(),
                load(&self.authz_requests_total),
            ),
            (
                "authz.requests_unsigned".to_string(),
                load(&self.authz_requests_unsigned),
            ),
            (
                "authz.requests_multi_issuer".to_string(),
                load(&self.authz_requests_multi_issuer),
            ),
            (
                "authz.decision_allow".to_string(),
                load(&self.authz_decision_allow),
            ),
            (
                "authz.decision_deny".to_string(),
                load(&self.authz_decision_deny),
            ),
            (
                "authz.errors_total".to_string(),
                load(&self.authz_errors_total),
            ),
            (
                "authz.last_eval_time_us".to_string(),
                load(&self.last_eval_time_us),
            ),
            ("authz.eval_time_p50_us".to_string(), p50),
            ("authz.eval_time_p95_us".to_string(), p95),
            ("authz.eval_time_p99_us".to_string(), p99),
            ("authz.eval_time_max_us".to_string(), max_time),
            ("token_cache.hits".to_string(), load(&self.token_cache_hits)),
            (
                "token_cache.misses".to_string(),
                load(&self.token_cache_misses),
            ),
            (
                "token_cache.evictions".to_string(),
                load(&self.token_cache_evictions),
            ),
            (
                "jwt.validations_total".to_string(),
                load(&self.jwt_validations_total),
            ),
            (
                "jwt.validations_success".to_string(),
                load(&self.jwt_validations_success),
            ),
            (
                "jwt.validations_failed".to_string(),
                load(&self.jwt_validations_failed),
            ),
            ("data.push_ops".to_string(), load(&self.data_push_ops)),
            ("data.get_ops".to_string(), load(&self.data_get_ops)),
            ("data.remove_ops".to_string(), load(&self.data_remove_ops)),
            ("instance.uptime_secs".to_string(), uptime_secs),
            ("instance.policy_count".to_string(), policy_count),
        ])
    }
}

/// Thread-safe telemetry metrics collector.
///
/// Uses atomic operations for hot-path counters (lock-free within each interval).
/// `record_*` methods acquire a read lock on the current [`IntervalState`] so
/// they can proceed concurrently. `snapshot_and_reset` acquires a write lock
/// once per interval and swaps the entire state for a fresh instance, guaranteeing
/// every counter is reset by construction.
#[derive(Debug)]
pub(crate) struct MetricsCollector {
    enabled: bool,
    /// State that persists across intervals
    init_time: DateTime<Utc>,
    policy_count: AtomicI64,

    /// Swapped wholesale on each snapshot; read lock for record_*, write lock for snapshot
    interval: RwLock<Box<IntervalState>>,
}

impl MetricsCollector {
    pub(crate) fn new(initial_policy_count: usize) -> Self {
        let now = Utc::now();
        Self {
            enabled: true,
            init_time: now,
            policy_count: AtomicI64::new(saturating_usize_to_i64(initial_policy_count)),
            interval: RwLock::new(Box::new(IntervalState::new(now))),
        }
    }

    pub(crate) fn disabled() -> Self {
        Self {
            enabled: false,
            init_time: Utc::now(),
            policy_count: AtomicI64::new(0),
            interval: RwLock::new(Box::new(IntervalState::new(Utc::now()))),
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
        if !self.enabled {
            return;
        }

        let interval = self.interval.read().expect(INTERVAL_LOCK_POISONED);

        interval
            .authz_requests_total
            .fetch_add(1, Ordering::Relaxed);

        if is_unsigned {
            interval
                .authz_requests_unsigned
                .fetch_add(1, Ordering::Relaxed);
        } else {
            interval
                .authz_requests_multi_issuer
                .fetch_add(1, Ordering::Relaxed);
        }

        match decision {
            Decision::Allow => interval
                .authz_decision_allow
                .fetch_add(1, Ordering::Relaxed),
            Decision::Deny => interval.authz_decision_deny.fetch_add(1, Ordering::Relaxed),
        };

        interval
            .last_eval_time_us
            .store(decision_time_us, Ordering::Relaxed);

        interval
            .timing_recorder
            .lock()
            .expect(TIMING_LOCK_POISONED)
            .record(decision_time_us);

        let policies: Vec<(&str, Decision)> = evaluated_policies.collect();
        let untracked_policies: Vec<(&str, Decision)> = {
            let stats = interval
                .policy_stats
                .read()
                .expect(POLICY_STATS_READ_LOCK_POISONED);
            policies
                .iter()
                .filter_map(|(id, pol_decision)| {
                    if let Some(entry) = stats.get(*id) {
                        entry.record(*pol_decision);
                        None
                    } else {
                        Some((*id, *pol_decision))
                    }
                })
                .collect()
        };

        if !untracked_policies.is_empty() {
            let mut stats = interval
                .policy_stats
                .write()
                .expect(POLICY_STATS_WRITE_LOCK_POISONED);
            for (id, pol_decision) in untracked_policies {
                stats
                    .entry(id.to_string())
                    .or_default()
                    .record(pol_decision);
            }
        }
    }

    /// Increments `authz.errors_total` counter.
    pub(crate) fn record_authz_error(&self) {
        if !self.enabled {
            return;
        }

        self.interval
            .read()
            .expect(INTERVAL_LOCK_POISONED)
            .authz_errors_total
            .fetch_add(1, Ordering::Relaxed);
    }

    /// Increments a classified error counter using a typed error that implements [`ErrorMetricKey`]
    pub(crate) fn record_error(&self, err: &impl ErrorMetricKey) {
        if !self.enabled {
            return;
        }
        self.increment_error(err.metric_key());
    }

    /// Increments a classified error counter by raw key string.
    pub(crate) fn increment_error(&self, key: &str) {
        if !self.enabled {
            return;
        }

        let interval = self.interval.read().expect(INTERVAL_LOCK_POISONED);
        let mut counters = interval
            .error_counters
            .write()
            .expect(ERROR_COUNTERS_LOCK_POISONED);
        *counters.entry(key.to_string()).or_insert(0) += 1;
    }

    pub(crate) fn record_cache_hit(&self) {
        if !self.enabled {
            return;
        }

        self.interval
            .read()
            .expect(INTERVAL_LOCK_POISONED)
            .token_cache_hits
            .fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn record_cache_miss(&self) {
        if !self.enabled {
            return;
        }

        self.interval
            .read()
            .expect(INTERVAL_LOCK_POISONED)
            .token_cache_misses
            .fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn record_cache_eviction(&self, count: usize) {
        if !self.enabled {
            return;
        }

        self.interval
            .read()
            .expect(INTERVAL_LOCK_POISONED)
            .token_cache_evictions
            .fetch_add(saturating_usize_to_i64(count), Ordering::Relaxed);
    }

    pub(crate) fn record_jwt_validation(&self, success: bool) {
        if !self.enabled {
            return;
        }

        let interval = self.interval.read().expect(INTERVAL_LOCK_POISONED);
        interval
            .jwt_validations_total
            .fetch_add(1, Ordering::Relaxed);
        if success {
            interval
                .jwt_validations_success
                .fetch_add(1, Ordering::Relaxed);
        } else {
            interval
                .jwt_validations_failed
                .fetch_add(1, Ordering::Relaxed);
        }
    }

    pub(crate) fn record_data_push(&self) {
        if !self.enabled {
            return;
        }

        self.interval
            .read()
            .expect(INTERVAL_LOCK_POISONED)
            .data_push_ops
            .fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn record_data_get(&self) {
        if !self.enabled {
            return;
        }

        self.interval
            .read()
            .expect(INTERVAL_LOCK_POISONED)
            .data_get_ops
            .fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn record_data_remove(&self) {
        if !self.enabled {
            return;
        }

        self.interval
            .read()
            .expect(INTERVAL_LOCK_POISONED)
            .data_remove_ops
            .fetch_add(1, Ordering::Relaxed);
    }

    pub(crate) fn set_policy_count(&self, count: usize) {
        if !self.enabled {
            return;
        }

        self.policy_count
            .store(saturating_usize_to_i64(count), Ordering::Relaxed);
    }

    /// Captures a snapshot of all metrics and resets counters for the next interval.
    ///
    /// The entire [`IntervalState`] is swapped for a fresh instance under a write lock,
    /// guaranteeing every counter is reset by construction.
    ///
    /// # Consistency
    ///
    /// A concurrent [`MetricsCollector::record_evaluation`] that lands between acquiring
    /// the write lock and completing the swap will block until the swap finishes, then
    /// record into the new interval. Derived invariants (e.g. `requests_total ==
    /// decision_allow + decision_deny`) may still be off by one across a snapshot boundary.
    pub(crate) fn snapshot_and_reset(&self) -> MetricsSnapshot {
        let now = Utc::now();

        let old = {
            let mut guard = self.interval.write().expect(INTERVAL_LOCK_POISONED);
            std::mem::replace(&mut *guard, Box::new(IntervalState::new(now)))
        };

        let interval_secs = now.signed_duration_since(old.start).num_seconds();

        let policy_stats = {
            let map = old
                .policy_stats
                .read()
                .expect(POLICY_STATS_READ_LOCK_POISONED);
            map.iter()
                .flat_map(|(id, stats)| {
                    let snap = stats.snapshot();
                    [
                        (id.clone(), snap.evaluations),
                        (format!("{id}.allow"), snap.allow_count),
                        (format!("{id}.deny"), snap.deny_count),
                    ]
                })
                .collect()
        };

        let error_counters = old
            .error_counters
            .read()
            .expect(ERROR_COUNTERS_LOCK_POISONED)
            .clone();

        let ops = old.to_operational_stats(now, self.init_time, &self.policy_count);

        MetricsSnapshot {
            policy_stats,
            error_counters,
            operational_stats: ops,
            interval_secs,
        }
    }
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
    fn timing_recorder_empty_returns_zeros() {
        let mut recorder = TimingRecorder::new();
        let (p50, p95, p99, max) = recorder.drain_and_compute();
        assert_eq!(p50, 0, "p50 must be 0 for empty");
        assert_eq!(p95, 0, "p95 must be 0 for empty");
        assert_eq!(p99, 0, "p99 must be 0 for empty");
        assert_eq!(max, 0, "max must be 0 for empty");
    }

    #[test]
    fn timing_recorder_single_element() {
        let mut recorder = TimingRecorder::new();
        recorder.record(42);
        let (p50, p95, p99, max) = recorder.drain_and_compute();
        assert_eq!(p50, 42, "p50 must be 42");
        assert_eq!(p95, 42, "p95 must be 42");
        assert_eq!(p99, 42, "p99 must be 42");
        assert_eq!(max, 42, "max must be 42");
    }

    #[test]
    fn timing_recorder_multiple_elements() {
        let mut recorder = TimingRecorder::new();
        let times = vec![10, 20, 30, 33, 40, 70, 49, 55, 55, 90, 110];
        for time in times {
            recorder.record(time);
        }
        let (p50, p95, p99, max) = recorder.drain_and_compute();
        assert_eq!(p50, 49, "p50 must be median");
        assert_eq!(p95, 110, "p95 must be 110");
        assert_eq!(p99, 110, "p99 must be 110");
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

    #[test]
    fn disabled_collector_noops() {
        let collector = MetricsCollector::disabled();

        collector.record_evaluation(100, Decision::Allow, false, std::iter::empty());
        collector.record_authz_error();
        collector.record_cache_hit();
        collector.record_cache_miss();
        collector.record_cache_eviction(5);
        collector.record_jwt_validation(true);
        collector.record_data_push();
        collector.record_data_get();
        collector.record_data_remove();
        collector.set_policy_count(10);

        let snap = collector.snapshot_and_reset();
        assert!(
            snap.operational_stats.values().all(|&v| v == 0),
            "all counters must be 0 when disabled"
        );
        assert!(
            snap.policy_stats.is_empty(),
            "policy_stats must be empty when disabled"
        );
        assert!(
            snap.error_counters.is_empty(),
            "error_counters must be empty when disabled"
        );
    }
}
