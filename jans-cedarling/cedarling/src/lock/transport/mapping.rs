// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::log::MetricsLogEntry;

#[derive(Debug, thiserror::Error)]
pub(super) enum MappingValidationError {
    #[error("missing required field")]
    MissingField,
}

#[derive(Debug, Deserialize)]
pub(super) struct CedarlingLogEntry {
    pub timestamp: String,
    pub log_kind: String,
    pub decision: String,
    pub action: String,
    pub level: Option<String>,
    // Cedarling emits principal as an array of entity strings
    #[serde(default)]
    pub principal: Vec<String>,
    pub resource: String,
    #[serde(default)]
    pub application_id: String,
    pub pdp_id: String,
    // Catch everything else for context_information
    #[serde(flatten)]
    pub extra: HashMap<String, Value>,
}

#[derive(Debug, Deserialize)]
pub(super) struct CedarlingMetricsEntry {
    #[serde(flatten)]
    pub metric: MetricsLogEntry,
    pub pdp_id: String,
    #[serde(default)]
    pub application_id: String,
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

/// Serializes into the lock server's expected metrics format
#[derive(Debug, Serialize)]
pub(super) struct LockServerMetricsEntry {
    pub creation_date: String,
    pub event_time: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub service: Option<String>,
    pub node_name: String,
    pub status: String,
    pub last_policy_load_size: i64,
    pub policy_success_load_counter: i64,
    pub policy_failed_load_counter: i64,
    pub last_policy_evaluation_time_ns: i64,
    pub avg_policy_evaluation_time_ns: i64,
    pub memory_usage: i64,
    pub evaluation_requests_count: i64,
    pub policy_stats: HashMap<String, i64>,
}

impl TryFrom<CedarlingLogEntry> for LockServerLogEntry {
    type Error = MappingValidationError;

    fn try_from(value: CedarlingLogEntry) -> Result<Self, Self::Error> {
        if value.log_kind.is_empty()
            || value.decision.is_empty()
            || value.action.is_empty()
            || value.resource.is_empty()
        {
            return Err(MappingValidationError::MissingField);
        }

        let mut extra = value.extra;
        let client_id = extra
            .remove("lock_client_id")
            .and_then(|v| v.as_str().map(String::from));
        let context_information = if extra.is_empty() {
            None
        } else {
            Some(Value::Object(extra.into_iter().collect()))
        };

        Ok(Self {
            creation_date: value.timestamp.clone(),
            event_time: value.timestamp,
            service: Some(value.application_id),
            node_name: value.pdp_id,
            event_type: value.log_kind,
            severity_level: value.level,
            action: value.action,
            decision_result: value.decision,
            requested_resource: value.resource,
            principal_id: if value.principal.is_empty() {
                None
            } else {
                Some(value.principal.join(", "))
            },
            client_id,
            context_information,
        })
    }
}

impl TryFrom<CedarlingMetricsEntry> for LockServerMetricsEntry {
    type Error = MappingValidationError;

    fn try_from(value: CedarlingMetricsEntry) -> Result<Self, Self::Error> {
        let timestamp = value.metric.base.timestamp.unwrap_or_default();

        Ok(Self {
            creation_date: timestamp.clone(),
            event_time: timestamp,
            service: (!value.application_id.is_empty()).then_some(value.application_id),
            node_name: value.pdp_id,
            status: "ok".to_string(),
            last_policy_load_size: value.metric.loaded_policies,
            policy_success_load_counter: value.metric.total_allows,
            policy_failed_load_counter: value.metric.total_denies,
            last_policy_evaluation_time_ns: value.metric.last_decision_time,
            avg_policy_evaluation_time_ns: value.metric.average_decision_time,
            memory_usage: value.metric.memory_usage,
            evaluation_requests_count: value.metric.evaluation_requests,
            policy_stats: value.metric.policy_stats,
        })
    }
}

#[cfg(test)]
mod test {
    use std::collections::HashSet;

    use super::*;
    use crate::common::app_types::{ApplicationName, PdpID};
    use crate::log::log_strategy::LogEntryWithClientInfo;
    use crate::log::{
        BaseLogEntry, Decision, DecisionLogEntry, DiagnosticsSummary, LogTokensInfo, LogType,
        MetricsLogEntry, PushedDataInfo,
    };

    /// Verifies that a real `DecisionLogEntry` (wrapped in `LogEntryWithClientInfo`)
    /// is correctly serialized, deserialized into `CedarlingLogEntry`, and then mapped
    /// to the Lock server's `LockServerLogEntry` format with all fields in the right places.
    #[test]
    fn decision_log_entry_maps_to_lock_server_format() {
        let request_id = crate::log::gen_uuid7();
        let base = BaseLogEntry::new_decision(request_id);

        let decision_entry = DecisionLogEntry {
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
        };

        let pdp_id = PdpID::new();
        let app_name = ApplicationName::from("my_test_app".to_string());

        // Wrap in LogEntryWithClientInfo — this is what the LockService actually receives
        let wrapped = LogEntryWithClientInfo::from_loggable(decision_entry, pdp_id, Some(app_name));

        // Serialize exactly as LockService.log_any() does
        let json_str = serde_json::to_string(&wrapped).expect("serialize wrapped entry");

        // Deserialize into the intermediate CedarlingLogEntry
        let cedarling_entry: CedarlingLogEntry =
            serde_json::from_str(&json_str).expect("deserialize into CedarlingLogEntry");

        // Verify key fields are correctly extracted
        assert_eq!(cedarling_entry.log_kind, "Decision");
        assert_eq!(cedarling_entry.decision, "ALLOW");
        assert_eq!(cedarling_entry.action, "Jans::Action::\"Update\"");
        assert_eq!(cedarling_entry.resource, "Jans::Issue::\"random_id\"");
        assert_eq!(cedarling_entry.application_id, "my_test_app");
        assert_eq!(
            cedarling_entry.principal,
            vec!["Jans::User", "Jans::Workload"]
        );
        assert!(
            !cedarling_entry.pdp_id.is_empty(),
            "pdp_id should not be empty"
        );

        // The extra fields should contain diagnostics, decision_time, etc.
        assert!(
            cedarling_entry.extra.contains_key("diagnostics"),
            "extra should contain diagnostics"
        );
        assert!(
            cedarling_entry
                .extra
                .contains_key("decision_time_micro_sec"),
            "extra should contain decision_time_micro_sec"
        );
        assert!(
            cedarling_entry.extra.contains_key("lock_client_id"),
            "extra should contain lock_client_id"
        );

        // Map to LockServerLogEntry
        let lock_entry =
            LockServerLogEntry::try_from(cedarling_entry).expect("map to LockServerLogEntry");

        // Verify all Lock server fields
        assert!(!lock_entry.creation_date.is_empty());
        assert!(!lock_entry.event_time.is_empty());
        assert_eq!(lock_entry.service.as_deref(), Some("my_test_app"));
        assert!(!lock_entry.node_name.is_empty()); // pdp_id
        assert_eq!(lock_entry.event_type, "Decision");
        assert_eq!(lock_entry.action, "Jans::Action::\"Update\"");
        assert_eq!(lock_entry.decision_result, "ALLOW");
        assert_eq!(lock_entry.requested_resource, "Jans::Issue::\"random_id\"");
        assert_eq!(
            lock_entry.principal_id.as_deref(),
            Some("Jans::User, Jans::Workload")
        );
        // lock_client_id should be extracted from extra into client_id
        assert_eq!(lock_entry.client_id.as_deref(), Some("lock-client-123"));
        // Remaining extra fields (diagnostics, decision_time, etc.) go into context_information
        let ctx = lock_entry
            .context_information
            .expect("context_information should be present");
        assert!(ctx.get("diagnostics").is_some());
        assert!(ctx.get("decision_time_micro_sec").is_some());
        assert!(ctx.get("policystore_id").is_some());
        assert!(ctx.get("pushed_data").is_some());
    }

    /// Verifies that a [`DecisionLogEntry`] with a DENY decision maps correctly.
    #[test]
    fn deny_decision_maps_correctly() {
        let base = BaseLogEntry::new_decision(crate::log::gen_uuid7());
        let entry = DecisionLogEntry {
            base,
            policystore_id: "store".into(),
            policystore_version: "1.0".into(),
            principal: vec![],
            lock_client_id: None,
            action: "Jans::Action::\"Read\"".to_string(),
            resource: "Jans::Resource::\"doc\"".to_string(),
            decision: Decision::Deny,
            tokens: LogTokensInfo::empty(),
            decision_time_micro_sec: 100,
            diagnostics: DiagnosticsSummary {
                reason: HashSet::default(),
                errors: Vec::new(),
            },
            pushed_data: None,
        };

        let pdp_id = PdpID::new();
        let wrapped = LogEntryWithClientInfo::from_loggable(entry, pdp_id, None);
        let json_str = serde_json::to_string(&wrapped).expect("serialize wrapped DecisionLogEntry");
        let cedarling: CedarlingLogEntry =
            serde_json::from_str(&json_str).expect("deserialize to CedarlingLogEntry");
        let lock_entry = LockServerLogEntry::try_from(cedarling)
            .expect("convert CedarlingLogEntry to LockServerLogEntry");

        assert_eq!(lock_entry.decision_result, "DENY");
        assert_eq!(lock_entry.principal_id, None); // empty principal list
        assert_eq!(lock_entry.client_id, None); // no lock_client_id
        assert_eq!(lock_entry.service, Some(String::new())); // no app_name → empty string
        assert_eq!(lock_entry.severity_level, None); // Decision logs have no level
    }

    #[test]
    fn mapping_fails_when_required_field_is_empty() {
        let cedarling = CedarlingLogEntry {
            timestamp: "2026-03-23T12:00:00Z".to_string(),
            log_kind: "Decision".to_string(),
            decision: "ALLOW".to_string(),
            action: String::new(),
            level: None,
            principal: vec![],
            resource: "Jans::Resource::\"doc\"".to_string(),
            application_id: "my_test_app".to_string(),
            pdp_id: "pdp-1".to_string(),
            extra: HashMap::new(),
        };

        let err = LockServerLogEntry::try_from(cedarling)
            .expect_err("empty action must fail LockServerLogEntry mapping");
        assert!(
            matches!(err, MappingValidationError::MissingField),
            "expected MissingField when a required input field is empty"
        );
    }

    #[test]
    fn metrics_log_entry_maps_to_lock_server_format() {
        let request_id = crate::log::gen_uuid7();
        let base = BaseLogEntry::new_metric(request_id);

        let mut policy_stats = std::collections::HashMap::new();
        policy_stats.insert("stat_1".to_string(), 100);
        policy_stats.insert("stat_2".to_string(), 3);

        let metrics_entry = MetricsLogEntry {
            base,
            loaded_policies: 1024,
            total_allows: 100,
            total_denies: 3,
            last_decision_time: 100_000,
            average_decision_time: 75_000,
            evaluation_requests: 103,
            memory_usage: 2_097_152,
            policy_stats,
        };

        let pdp_id = PdpID::new();
        let app_name = ApplicationName::from("jans-auth".to_string());

        let wrapped = LogEntryWithClientInfo::from_loggable(metrics_entry, pdp_id, Some(app_name));

        let json_str = serde_json::to_string(&wrapped).expect("serialize wrapped metrics entry");

        let cedarling_entry: CedarlingMetricsEntry =
            serde_json::from_str(&json_str).expect("deserialize into CedarlingMetricsEntry");

        assert_eq!(cedarling_entry.metric.base.log_kind, LogType::Metric);
        assert_eq!(cedarling_entry.metric.loaded_policies, 1024);
        assert_eq!(cedarling_entry.metric.total_allows, 100);
        assert_eq!(cedarling_entry.metric.total_denies, 3);
        assert_eq!(cedarling_entry.metric.last_decision_time, 100_000);
        assert_eq!(cedarling_entry.metric.average_decision_time, 75_000);
        assert_eq!(cedarling_entry.metric.evaluation_requests, 103);
        assert_eq!(cedarling_entry.metric.memory_usage, 2_097_152);
        assert_eq!(cedarling_entry.application_id, "jans-auth");
        assert_eq!(
            cedarling_entry.metric.policy_stats.get("stat_1"),
            Some(&100)
        );
        assert_eq!(cedarling_entry.metric.policy_stats.get("stat_2"), Some(&3));

        let lock_entry = LockServerMetricsEntry::try_from(cedarling_entry)
            .expect("map to LockServerMetricsEntry");

        assert_eq!(lock_entry.service.as_deref(), Some("jans-auth"));
        assert_eq!(lock_entry.node_name, pdp_id.to_string());
        assert_eq!(lock_entry.status, "ok");
        assert_eq!(lock_entry.last_policy_load_size, 1024);
        assert_eq!(lock_entry.policy_success_load_counter, 100);
        assert_eq!(lock_entry.policy_failed_load_counter, 3);
        assert_eq!(lock_entry.last_policy_evaluation_time_ns, 100_000);
        assert_eq!(lock_entry.avg_policy_evaluation_time_ns, 75_000);
        assert_eq!(lock_entry.memory_usage, 2_097_152);
        assert_eq!(lock_entry.evaluation_requests_count, 103);
        assert_eq!(lock_entry.policy_stats.get("stat_1"), Some(&100));
        assert_eq!(lock_entry.policy_stats.get("stat_2"), Some(&3));
    }

    #[test]
    fn metrics_log_entry_without_app_name_maps_correctly() {
        let base = BaseLogEntry::new_metric(crate::log::gen_uuid7());

        let metrics_entry = MetricsLogEntry {
            base,
            loaded_policies: 2048,
            total_allows: 250,
            total_denies: 1,
            last_decision_time: 85_000,
            average_decision_time: 92_000,
            evaluation_requests: 420,
            memory_usage: 4_194_304,
            policy_stats: std::collections::HashMap::new(),
        };

        let pdp_id = PdpID::new();
        let wrapped = LogEntryWithClientInfo::from_loggable(metrics_entry, pdp_id, None);
        let json_str = serde_json::to_string(&wrapped).expect("serialize wrapped MetricsLogEntry");
        let cedarling: CedarlingMetricsEntry =
            serde_json::from_str(&json_str).expect("deserialize to CedarlingMetricsEntry");
        let lock_entry = LockServerMetricsEntry::try_from(cedarling)
            .expect("convert CedarlingMetricsEntry to LockServerMetricsEntry");

        assert_eq!(lock_entry.service, None);
        assert_eq!(lock_entry.status, "ok");
        assert_eq!(lock_entry.last_policy_load_size, 2048);
        assert_eq!(lock_entry.policy_stats.len(), 0);
    }
}
