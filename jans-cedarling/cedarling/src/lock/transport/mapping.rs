// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::lock::health_registry::{HealthStatus, SystemHealth};
use crate::lock::transport::{AuditItem, AuditPayload, TransportError};
use crate::log::DecisionLogEntry;

#[derive(Debug, thiserror::Error)]
pub(crate) enum MappingValidationError {
    #[error("missing required field")]
    MissingField,
    #[error("expected a {expected} entry, got a {got} entry")]
    UnexpectedKind {
        expected: &'static str,
        got: &'static str,
    },
    #[error("failed to serialize entry context: {0}")]
    Context(String),
}

impl AuditPayload {
    fn kind(&self) -> &'static str {
        match self {
            AuditPayload::Decision(_) => "decision",
            AuditPayload::Metric(_) => "metric",
            AuditPayload::Health(_) => "health",
        }
    }
}

/// Serializes into the lock server's expected format
#[derive(Debug, Serialize)]
pub(super) struct LockServerLogEntry {
    pub creation_date: String,
    pub event_time: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub service: Option<String>,
    pub node_name: String,
    pub event_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub severity_level: Option<String>,
    pub action: String,
    pub decision_result: String,
    pub requested_resource: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub principal_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_information: Option<Value>,
}

/// Serializes into the lock server's expected health format.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct LockServerHealthEntry {
    pub creation_date: String,
    pub event_time: String,
    pub service: String,
    pub node_name: String,
    pub status: String,
    #[serde(default, skip_serializing_if = "HashMap::is_empty")]
    pub engine_status: HashMap<String, HealthStatus>,
}

/// Serializes into the lock server's expected telemetry format (3-map model).
#[derive(Debug, Serialize)]
pub(super) struct LockServerMetricsEntry {
    pub creation_date: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub service: Option<String>,
    pub node_name: String,
    pub status: String,
    pub interval_secs: i64,
    pub policy_stats: HashMap<String, i64>,
    pub error_counters: HashMap<String, i64>,
    pub operational_stats: HashMap<String, i64>,
}

impl TryFrom<&AuditItem> for LockServerLogEntry {
    type Error = MappingValidationError;

    fn try_from(item: &AuditItem) -> Result<Self, Self::Error> {
        let AuditPayload::Decision(entry) = &item.payload else {
            return Err(MappingValidationError::UnexpectedKind {
                expected: "decision",
                got: item.payload.kind(),
            });
        };

        let timestamp = entry
            .base
            .timestamp
            .clone()
            .ok_or(MappingValidationError::MissingField)?;
        if entry.action.is_empty() || entry.resource.is_empty() {
            return Err(MappingValidationError::MissingField);
        }

        Ok(Self {
            creation_date: timestamp.clone(),
            event_time: timestamp,
            service: item.app_name.as_ref().map(|n| n.0.to_string()),
            node_name: item.pdp_id.to_string(),
            event_type: entry.base.log_kind.to_string(),
            severity_level: entry.base.level.map(|level| level.to_string()),
            action: entry.action.clone(),
            decision_result: entry.decision.to_string(),
            requested_resource: entry.resource.clone(),
            principal_id: (!entry.principal.is_empty()).then(|| {
                entry
                    .principal
                    .iter()
                    .map(ToString::to_string)
                    .collect::<Vec<_>>()
                    .join(", ")
            }),
            client_id: entry.lock_client_id.clone(),
            context_information: decision_context_information(entry)?,
        })
    }
}

fn decision_context_information(
    entry: &DecisionLogEntry,
) -> Result<Option<Value>, MappingValidationError> {
    let value =
        serde_json::to_value(entry).map_err(|e| MappingValidationError::Context(e.to_string()))?;
    let Value::Object(mut map) = value else {
        return Ok(None);
    };

    for key in [
        "timestamp",
        "log_kind",
        "level",
        "decision",
        "action",
        "resource",
        "principal",
        "lock_client_id",
    ] {
        map.remove(key);
    }

    Ok((!map.is_empty()).then(|| Value::Object(map)))
}

impl TryFrom<&AuditItem> for LockServerMetricsEntry {
    type Error = MappingValidationError;

    fn try_from(item: &AuditItem) -> Result<Self, Self::Error> {
        let AuditPayload::Metric(entry) = &item.payload else {
            return Err(MappingValidationError::UnexpectedKind {
                expected: "metric",
                got: item.payload.kind(),
            });
        };

        Ok(Self {
            creation_date: entry
                .base
                .timestamp
                .clone()
                .ok_or(MappingValidationError::MissingField)?,
            service: item.app_name.as_ref().map(|n| n.0.to_string()),
            node_name: item.pdp_id.to_string(),
            status: item
                .status
                .map_or("unknown", SystemHealth::as_str)
                .to_string(),
            interval_secs: entry.interval_secs,
            policy_stats: entry.policy_stats.clone(),
            error_counters: entry.error_counters.clone(),
            operational_stats: entry.operational_stats.clone(),
        })
    }
}

impl TryFrom<&AuditItem> for LockServerHealthEntry {
    type Error = MappingValidationError;

    fn try_from(item: &AuditItem) -> Result<Self, Self::Error> {
        let AuditPayload::Health(entry) = &item.payload else {
            return Err(MappingValidationError::UnexpectedKind {
                expected: "health",
                got: item.payload.kind(),
            });
        };

        Ok(entry.as_ref().clone())
    }
}

/// Map a batch of typed audit items into the Lock Server wire format
pub(super) fn map_entries<'a, T>(
    entries: &'a [AuditItem],
    log_warn: impl Fn(String),
) -> Result<Vec<T>, TransportError>
where
    T: TryFrom<&'a AuditItem>,
    <T as TryFrom<&'a AuditItem>>::Error: std::fmt::Display,
{
    if entries.is_empty() {
        return Ok(Vec::new());
    }

    let mapped: Vec<T> = entries
        .iter()
        .enumerate()
        .filter_map(|(idx, item)| match T::try_from(item) {
            Ok(t) => Some(t),
            Err(e) => {
                let kind = item.payload.kind();
                log_warn(format!("failed to convert {kind} entry[{idx}]: {e}"));
                None
            },
        })
        .collect();

    if mapped.is_empty() {
        return Err(TransportError::Serialization(format!(
            "all {} entries were malformed, nothing to send",
            entries.len(),
        )));
    }

    Ok(mapped)
}

#[cfg(test)]
mod test {
    use std::collections::HashSet;

    use super::*;
    use crate::common::app_types::{ApplicationName, PdpID};
    use crate::lock::health_registry::SystemHealth;
    use crate::lock::transport::test_utils::{decision_audit_item, metric_audit_item};
    use crate::log::{
        BaseLogEntry, Decision, DecisionLogEntry, DiagnosticsSummary, LogTokensInfo,
        MetricsLogEntry, PushedDataInfo,
    };

    fn test_decision_entry() -> DecisionLogEntry {
        let request_id = crate::log::gen_uuid7();
        let mut base = BaseLogEntry::new_decision(request_id);
        base.timestamp = Some("2026-03-23T11:50:37.504Z".to_string());

        DecisionLogEntry {
            base,
            policystore_id: "test-store".into(),
            policystore_version: "1.0.0".into(),
            principal: vec!["Jans::User".into(), "Jans::Workload".into()],
            lock_client_id: Some("lock-client-123".to_string()),
            action: "Jans::Action::\"Update\"".to_string(),
            resource: "Jans::Issue::\"random_id\"".to_string(),
            decision: Decision::Allow,
            tokens: LogTokensInfo::empty(),
            decision_time_micro_sec: 42,
            diagnostics: DiagnosticsSummary {
                reason: HashSet::default(),
                errors: Vec::new(),
            },
            pushed_data: Some(PushedDataInfo {
                keys: vec!["extra_context".into()],
            }),
            batch_id: None,
        }
    }

    #[test]
    fn decision_log_entry_maps_to_lock_server_format() {
        let pdp_id = PdpID::new();
        let app_name = ApplicationName::from("my_test_app".to_string());
        let item = decision_audit_item(test_decision_entry(), pdp_id, Some(app_name));

        let lock_entry = LockServerLogEntry::try_from(&item).expect("map to LockServerLogEntry");

        assert_eq!(lock_entry.creation_date, "2026-03-23T11:50:37.504Z");
        assert_eq!(lock_entry.event_time, "2026-03-23T11:50:37.504Z");
        assert_eq!(lock_entry.service.as_deref(), Some("my_test_app"));
        assert_eq!(lock_entry.node_name, pdp_id.to_string());
        assert_eq!(lock_entry.event_type, "Decision");
        assert_eq!(lock_entry.action, "Jans::Action::\"Update\"");
        assert_eq!(lock_entry.decision_result, "ALLOW");
        assert_eq!(lock_entry.requested_resource, "Jans::Issue::\"random_id\"");
        assert_eq!(
            lock_entry.principal_id.as_deref(),
            Some("Jans::User, Jans::Workload")
        );
        assert_eq!(lock_entry.client_id.as_deref(), Some("lock-client-123"));
        let ctx = lock_entry
            .context_information
            .expect("context_information should be present");
        assert!(ctx.get("diagnostics").is_some());
        assert!(ctx.get("decision_time_micro_sec").is_some());
        assert!(ctx.get("policystore_id").is_some());
        assert!(ctx.get("pushed_data").is_some());
        assert!(ctx.get("action").is_none());
        assert!(ctx.get("decision").is_none());
        assert!(ctx.get("lock_client_id").is_none());
    }

    #[test]
    fn deny_decision_maps_correctly() {
        let mut entry = test_decision_entry();
        entry.principal = vec![];
        entry.lock_client_id = None;
        entry.decision = Decision::Deny;

        let item = decision_audit_item(entry, PdpID::new(), None);
        let lock_entry = LockServerLogEntry::try_from(&item).expect("map to LockServerLogEntry");

        assert_eq!(lock_entry.decision_result, "DENY");
        assert_eq!(lock_entry.principal_id, None);
        assert_eq!(lock_entry.client_id, None);
        assert_eq!(lock_entry.service, None);
        assert_eq!(lock_entry.severity_level, None);
    }

    #[test]
    fn mapping_fails_when_required_field_is_empty() {
        let mut entry = test_decision_entry();
        entry.action = String::new();

        let item = decision_audit_item(entry, PdpID::new(), None);
        let err = LockServerLogEntry::try_from(&item)
            .expect_err("empty action must fail LockServerLogEntry mapping");
        assert!(
            matches!(err, MappingValidationError::MissingField),
            "expected MissingField when a required input field is empty"
        );
    }

    #[test]
    fn mapping_fails_when_timestamp_is_missing() {
        let mut entry = test_decision_entry();
        entry.base.timestamp = None;

        let item = decision_audit_item(entry, PdpID::new(), None);
        let err = LockServerLogEntry::try_from(&item)
            .expect_err("missing timestamp must fail LockServerLogEntry mapping");
        assert!(matches!(err, MappingValidationError::MissingField));
    }

    #[test]
    fn mapping_fails_on_kind_mismatch() {
        let base = BaseLogEntry::new_metric(crate::log::gen_uuid7());
        let metric = MetricsLogEntry {
            base,
            policy_stats: HashMap::new(),
            error_counters: HashMap::new(),
            operational_stats: HashMap::new(),
            interval_secs: 60,
        };

        let item = metric_audit_item(metric, PdpID::new(), None);
        let err =
            LockServerLogEntry::try_from(&item).expect_err("metric payload must fail log mapping");
        assert!(matches!(err, MappingValidationError::UnexpectedKind { .. }));
    }

    #[test]
    fn metrics_log_entry_maps_to_lock_server_format() {
        let request_id = crate::log::gen_uuid7();
        let base = BaseLogEntry::new_metric(request_id);

        let mut policy_stats = HashMap::new();
        policy_stats.insert("allow_read_docs".to_string(), 340);
        policy_stats.insert("deny_admin".to_string(), 12);

        let mut operational_stats = HashMap::new();
        operational_stats.insert("authz.requests_total".to_string(), 1240);
        operational_stats.insert("authz.decision_allow".to_string(), 1218);

        let mut error_counters = HashMap::new();
        error_counters.insert("jwt.validation_failed".to_string(), 8);

        let metrics_entry = MetricsLogEntry {
            base,
            policy_stats,
            error_counters,
            operational_stats,
            interval_secs: 60,
        };

        let pdp_id = PdpID::new();
        let app_name = ApplicationName::from("jans-auth".to_string());
        let item = metric_audit_item(metrics_entry, pdp_id, Some(app_name));

        let lock_entry =
            LockServerMetricsEntry::try_from(&item).expect("map to LockServerMetricsEntry");

        assert_eq!(lock_entry.service.as_deref(), Some("jans-auth"));
        assert_eq!(lock_entry.node_name, pdp_id.to_string());
        assert_eq!(lock_entry.status, SystemHealth::Unknown.as_str());
        assert_eq!(lock_entry.interval_secs, 60);
        assert_eq!(lock_entry.policy_stats.get("allow_read_docs"), Some(&340));
        assert_eq!(
            lock_entry.error_counters.get("jwt.validation_failed"),
            Some(&8)
        );
        assert_eq!(
            lock_entry.operational_stats.get("authz.requests_total"),
            Some(&1240)
        );
    }

    #[test]
    fn metrics_log_entry_without_app_name_maps_correctly() {
        let base = BaseLogEntry::new_metric(crate::log::gen_uuid7());

        let metrics_entry = MetricsLogEntry {
            base,
            policy_stats: HashMap::new(),
            error_counters: HashMap::new(),
            operational_stats: HashMap::new(),
            interval_secs: 30,
        };

        let item = metric_audit_item(metrics_entry, PdpID::new(), None);
        let lock_entry =
            LockServerMetricsEntry::try_from(&item).expect("map to LockServerMetricsEntry");

        assert_eq!(lock_entry.service, None);
        assert_eq!(lock_entry.status, SystemHealth::Unknown.as_str());
        assert_eq!(lock_entry.interval_secs, 30);
        assert!(lock_entry.policy_stats.is_empty());
        assert!(lock_entry.error_counters.is_empty());
    }

    #[test]
    fn metrics_log_entry_passes_through_degraded_status() {
        let base = BaseLogEntry::new_metric(crate::log::gen_uuid7());

        let metrics_entry = MetricsLogEntry {
            base,
            policy_stats: HashMap::new(),
            error_counters: HashMap::new(),
            operational_stats: HashMap::new(),
            interval_secs: 60,
        };

        let mut item = metric_audit_item(
            metrics_entry,
            PdpID::new(),
            Some(ApplicationName::from("svc".to_string())),
        );
        item.status = Some(SystemHealth::Degraded);

        let lock_entry =
            LockServerMetricsEntry::try_from(&item).expect("map to LockServerMetricsEntry");

        assert_eq!(lock_entry.status, SystemHealth::Degraded.as_str());
    }

    #[test]
    fn map_entries_skips_bad_entries_and_keeps_good_ones() {
        let mut bad = test_decision_entry();
        bad.action = String::new();

        let items = vec![
            decision_audit_item(test_decision_entry(), PdpID::new(), None),
            decision_audit_item(bad, PdpID::new(), None),
        ];

        let mapped: Vec<LockServerLogEntry> =
            map_entries(&items, |_| {}).expect("one valid entry should map");
        assert_eq!(mapped.len(), 1, "only the valid entry should be mapped");
    }

    #[test]
    fn map_entries_fails_when_all_entries_are_malformed() {
        let mut bad = test_decision_entry();
        bad.action = String::new();

        let items = vec![decision_audit_item(bad, PdpID::new(), None)];

        let err = map_entries::<LockServerLogEntry>(&items, |_| {})
            .expect_err("all-malformed batch must fail");
        assert!(matches!(err, TransportError::Serialization(_)));
    }
}
