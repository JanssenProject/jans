// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Metrics collection for telemetry logging

use std::collections::HashMap;
use std::sync::RwLock;
use std::sync::atomic::{AtomicI64, Ordering};

use crate::log::Decision;

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

/// Thread-safe metrics collector using atomic operations for counters
///
/// Lock-free for most operations; only `policy_stats` requires a read-write lock
/// for `HashMap` access
#[derive(Debug, Default)]
pub(crate) struct MetricsCollector {
    /// Total number of authorization evaluation requests
    evaluation_requests_count: AtomicI64,
    /// Last policy evaluation time in nanoseconds
    last_policy_evaluation_time_ns: AtomicI64,
    /// Running total of all evaluation times for computing average
    total_policy_evaluation_time_ns: AtomicI64,
    /// Total number of allowed requests
    total_allows: AtomicI64,
    /// Total number of denied requests
    total_denies: AtomicI64,
    /// Last loaded policy set size
    last_policy_load_size: AtomicI64,
    /// Memory usage in bytes
    memory_usage: AtomicI64,
    /// Per-policy statistics
    policy_stats: RwLock<HashMap<String, PolicyStats>>,
}

/// Immutable snapshot of all metrics at a point in time
#[derive(Debug, Clone)]
pub(crate) struct MetricsSnapshot {
    pub evaluation_requests_count: i64,
    pub last_policy_evaluation_time_ns: i64,
    pub avg_policy_evaluation_time_ns: i64,
    pub total_allows: i64,
    pub total_denies: i64,
    pub last_policy_load_size: i64,
    pub memory_usage: i64,
    pub policy_stats: HashMap<String, i64>,
}

impl MetricsCollector {
    /// Creates a new metrics collector with the given initial policy load size
    pub(crate) fn new(initial_policy_count: usize) -> Self {
        Self {
            last_policy_load_size: AtomicI64::new(saturating_usize_to_i64(initial_policy_count)),
            ..Self::default()
        }
    }

    /// Records a policy evaluation with its timing and evaluated policies
    pub(crate) fn record_evaluation<'a>(
        &self,
        decision_time_micro_sec: i64,
        decision: Decision,
        evaluated_policies: impl Iterator<Item = (&'a str, Decision)>,
    ) {
        let decision_time_ns = decision_time_micro_sec.saturating_mul(1_000);

        self.last_policy_evaluation_time_ns
            .store(decision_time_ns, Ordering::Relaxed);
        self.total_policy_evaluation_time_ns
            .fetch_add(decision_time_ns, Ordering::Relaxed);
        self.evaluation_requests_count
            .fetch_add(1, Ordering::Relaxed);

        match decision {
            Decision::Allow => self.total_allows.fetch_add(1, Ordering::Relaxed),
            Decision::Deny => self.total_denies.fetch_add(1, Ordering::Relaxed),
        };

        // Record per-policy stats
        let mut stats = self
            .policy_stats
            .write()
            .expect("policy_stats lock should not be poisoned");

        for (policy_id, decision) in evaluated_policies {
            stats
                .entry(policy_id.to_string())
                .or_default()
                .record(decision);
        }
    }

    /// Captures a snapshot of all current metrics
    ///
    /// Policy stats are flattened into a `HashMap` with keys in the format:
    /// - `{policy_id}` - evaluation count
    /// - `{policy_id}_allow` - allow count  
    /// - `{policy_id}_deny` - deny count
    pub(crate) fn snapshot(&self) -> MetricsSnapshot {
        let evaluation_count = self.evaluation_requests_count.load(Ordering::Relaxed);
        let total_time = self.total_policy_evaluation_time_ns.load(Ordering::Relaxed);

        let avg_time = if evaluation_count > 0 {
            total_time.saturating_div(evaluation_count)
        } else {
            0
        };

        let policy_stats = self
            .policy_stats
            .read()
            .expect("policy_stats lock should not be poisoned")
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

        MetricsSnapshot {
            evaluation_requests_count: evaluation_count,
            last_policy_evaluation_time_ns: self
                .last_policy_evaluation_time_ns
                .load(Ordering::Relaxed),
            avg_policy_evaluation_time_ns: avg_time,
            total_allows: self.total_allows.load(Ordering::Relaxed),
            total_denies: self.total_denies.load(Ordering::Relaxed),
            last_policy_load_size: self.last_policy_load_size.load(Ordering::Relaxed),
            memory_usage: self.memory_usage.load(Ordering::Relaxed),
            policy_stats,
        }
    }
}

fn saturating_usize_to_i64(value: usize) -> i64 {
    i64::try_from(value).unwrap_or(i64::MAX)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn record_evaluation_updates_counters() {
        let collector = MetricsCollector::new(10);

        collector.record_evaluation(100, Decision::Allow, std::iter::empty());

        let snapshot = collector.snapshot();
        assert_eq!(snapshot.evaluation_requests_count, 1);
        assert_eq!(snapshot.last_policy_evaluation_time_ns, 100_000); // 100 µs = 100_000 ns
    }

    #[test]
    fn record_evaluation_computes_running_average() {
        let collector = MetricsCollector::new(10);

        collector.record_evaluation(200, Decision::Allow, std::iter::empty());
        collector.record_evaluation(300, Decision::Allow, std::iter::empty());
        collector.record_evaluation(400, Decision::Deny, std::iter::empty());

        let snapshot = collector.snapshot();
        assert_eq!(snapshot.evaluation_requests_count, 3);
        // Average: (200_000 + 300_000 + 400_000) / 3 = 300_000 ns
        assert_eq!(snapshot.avg_policy_evaluation_time_ns, 300_000);
        assert_eq!(snapshot.last_policy_evaluation_time_ns, 400_000);
    }

    #[test]
    fn record_evaluation_tracks_policy_allow_deny() {
        let collector = MetricsCollector::new(10);

        // Record evaluations with policy decisions
        collector.record_evaluation(
            50,
            Decision::Allow,
            [("policy_1", Decision::Allow), ("policy_2", Decision::Allow)].into_iter(),
        );
        collector.record_evaluation(
            50,
            Decision::Allow,
            [("policy_1", Decision::Deny), ("policy_3", Decision::Deny)].into_iter(),
        );

        let snapshot = collector.snapshot();

        // policy_1: 2 evaluations, 1 allow, 1 deny
        assert_eq!(
            snapshot.policy_stats.get("policy_1"),
            Some(&2),
            "policy_1 should have 2 evaluations"
        );
        assert_eq!(
            snapshot.policy_stats.get("policy_1_allow"),
            Some(&1),
            "policy_1 should have 1 allow"
        );
        assert_eq!(
            snapshot.policy_stats.get("policy_1_deny"),
            Some(&1),
            "policy_1 should have 1 deny"
        );

        // policy_2: 1 evaluation, 1 allow, 0 deny
        assert_eq!(snapshot.policy_stats.get("policy_2"), Some(&1));
        assert_eq!(snapshot.policy_stats.get("policy_2_allow"), Some(&1));
        assert_eq!(snapshot.policy_stats.get("policy_2_deny"), Some(&0));

        // policy_3: 1 evaluation, 0 allow, 1 deny
        assert_eq!(snapshot.policy_stats.get("policy_3"), Some(&1));
        assert_eq!(snapshot.policy_stats.get("policy_3_allow"), Some(&0));
        assert_eq!(snapshot.policy_stats.get("policy_3_deny"), Some(&1));
    }

    #[test]
    fn average_with_zero_evaluations_is_zero() {
        let collector = MetricsCollector::new(10);

        let snapshot = collector.snapshot();
        assert_eq!(
            snapshot.avg_policy_evaluation_time_ns, 0,
            "average should be 0 when no evaluations"
        );
    }
}
