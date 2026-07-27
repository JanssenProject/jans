// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Shared test utilities for transport tests.

#![cfg(test)]

use std::collections::{HashMap, HashSet};

use crate::common::app_types::{ApplicationName, PdpID};
use crate::lock::health_registry::HealthStatus;
use crate::lock::transport::mapping::LockServerHealthEntry;
use crate::lock::transport::{AuditItem, AuditPayload};
use crate::log::{
    BaseLogEntry, Decision, DecisionLogEntry, DiagnosticsSummary, LogTokensInfo, MetricsLogEntry,
};

/// Wrap a [`DecisionLogEntry`] into an [`AuditItem`].
pub(crate) fn decision_audit_item(
    entry: DecisionLogEntry,
    pdp_id: PdpID,
    app_name: Option<ApplicationName>,
) -> AuditItem {
    AuditItem {
        payload: AuditPayload::Decision(Box::new(entry)),
        pdp_id,
        app_name,
        status: None,
    }
}

/// Wrap a [`MetricsLogEntry`] into an [`AuditItem`].
pub(crate) fn metric_audit_item(
    entry: MetricsLogEntry,
    pdp_id: PdpID,
    app_name: Option<ApplicationName>,
) -> AuditItem {
    AuditItem {
        payload: AuditPayload::Metric(Box::new(entry)),
        pdp_id,
        app_name,
        status: None,
    }
}

/// Build a sample log [`AuditItem`] for transport tests.
pub(crate) fn sample_log_item() -> AuditItem {
    let mut base = BaseLogEntry::new_decision(crate::log::gen_uuid7());
    base.timestamp = Some("2026-03-23T11:50:37.504Z".to_string());
    let entry = DecisionLogEntry {
        base,
        policystore_id: "store".into(),
        policystore_version: "1.0".into(),
        principal: vec!["Jans::User".into()],
        lock_client_id: None,
        action: "Test".to_string(),
        resource: "Jans::Issue".to_string(),
        decision: Decision::Allow,
        tokens: LogTokensInfo::empty(),
        decision_time_micro_sec: 1,
        diagnostics: DiagnosticsSummary {
            reason: HashSet::default(),
            errors: Vec::new(),
        },
        pushed_data: None,
    };
    decision_audit_item(
        entry,
        PdpID::new(),
        Some(ApplicationName::from("test_app".to_string())),
    )
}

/// Build a sample metric [`AuditItem`] for transport tests.
pub(crate) fn sample_metric_item() -> AuditItem {
    let mut base = BaseLogEntry::new_metric(crate::log::gen_uuid7());
    base.timestamp = Some("2026-04-07T17:04:39.162Z".to_string());
    let entry = MetricsLogEntry {
        base,
        policy_stats: HashMap::new(),
        error_counters: HashMap::new(),
        operational_stats: HashMap::new(),
        interval_secs: 60,
    };
    metric_audit_item(
        entry,
        PdpID::new(),
        Some(ApplicationName::from("test_app".to_string())),
    )
}

/// Like [`sample_log_item`], but with an empty `action` so that mapping into the
/// Lock Server shape fails validation.
pub(crate) fn malformed_log_item() -> AuditItem {
    let mut base = BaseLogEntry::new_decision(crate::log::gen_uuid7());
    base.timestamp = Some("2026-03-23T11:50:37.504Z".to_string());
    let entry = DecisionLogEntry {
        base,
        policystore_id: "store".into(),
        policystore_version: "1.0".into(),
        principal: vec!["Jans::User".into()],
        lock_client_id: None,
        action: String::new(),
        resource: "Jans::Issue".to_string(),
        decision: Decision::Allow,
        tokens: LogTokensInfo::empty(),
        decision_time_micro_sec: 1,
        diagnostics: DiagnosticsSummary {
            reason: HashSet::default(),
            errors: Vec::new(),
        },
        pushed_data: None,
    };
    decision_audit_item(
        entry,
        PdpID::new(),
        Some(ApplicationName::from("test_app".to_string())),
    )
}

/// Build a sample health [`AuditItem`] for transport tests.
pub(crate) fn sample_health_item() -> AuditItem {
    AuditItem {
        payload: AuditPayload::Health(Box::new(LockServerHealthEntry {
            creation_date: "2026-03-23T11:50:37.504Z".to_string(),
            event_time: "2026-03-23T11:50:37.504Z".to_string(),
            service: "test_app".to_string(),
            node_name: "test-pdp".to_string(),
            status: "running".to_string(),
            engine_status: [("core".to_string(), HealthStatus::Success)]
                .into_iter()
                .collect(),
        })),
        pdp_id: PdpID::new(),
        app_name: None,
        status: None,
    }
}
