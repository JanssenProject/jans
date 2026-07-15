// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;
use std::time::Duration;

use chrono::Utc;
use tokio::time::{MissedTickBehavior, interval};
use tokio_util::sync::CancellationToken;
use url::Url;

use super::health_registry::HealthRegistry;
use super::transport::mapping::LockServerHealthEntry;
use super::transport::{AuditItem, AuditKind, AuditPayload, AuditTransport};
use crate::app_types::{ApplicationName, PdpID};
use crate::http::{JoinHandle, spawn_task};
use crate::lock::LockLogEntry;
use crate::log::{LogWriter, LoggerWeak};

pub(super) struct HealthTickerParams {
    pub(super) health_url: url::Url,
    pub(super) pdp_id: PdpID,
    pub(super) app_name: Option<ApplicationName>,
    pub(super) health_interval: Duration,
    pub(super) logger: Option<LoggerWeak>,
    pub(super) registry: HealthRegistry,
}

pub(super) struct HealthTicker<T: AuditTransport> {
    transport: Arc<T>,
    health_url: Url,
    pdp_id: PdpID,
    app_name: Option<ApplicationName>,
    interval: Duration,
    logger: Option<LoggerWeak>,
    registry: HealthRegistry,
}

impl<T: AuditTransport + 'static> HealthTicker<T> {
    pub(super) fn spawn(
        transport: Arc<T>,
        params: HealthTickerParams,
    ) -> (CancellationToken, JoinHandle<()>) {
        let cancel_tkn = CancellationToken::new();
        let ticker = Self {
            transport,
            health_url: params.health_url,
            pdp_id: params.pdp_id,
            app_name: params.app_name,
            interval: params.health_interval,
            logger: params.logger,
            registry: params.registry,
        };
        let handle = spawn_task(ticker.run(cancel_tkn.clone()));
        (cancel_tkn, handle)
    }

    async fn run(self, cancel_tkn: CancellationToken) {
        let mut ticker = interval(self.interval);
        ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);
        ticker.tick().await;
        loop {
            tokio::select! {
                _ = ticker.tick() => {
                    self.send_health().await;
                }
                () = cancel_tkn.cancelled() => {
                    return;
                }
            }
        }
    }

    async fn send_health(&self) {
        let entry = self.build_health_entry();
        let logger = self.logger.as_ref().and_then(std::sync::Weak::upgrade);

        let item = AuditItem {
            payload: AuditPayload::Health(Box::new(entry)),
            pdp_id: self.pdp_id,
            app_name: self.app_name.clone(),
            status: None,
        };

        let result = self
            .transport
            .send(&[item], &AuditKind::Health(self.health_url.clone()))
            .await;

        if let Err(e) = result {
            logger.log_any(LockLogEntry::error(format!(
                "failed to send health check to lock server: {e}"
            )));
        }
    }

    fn build_health_entry(&self) -> LockServerHealthEntry {
        let now = Utc::now().to_rfc3339();
        let engine_status = self.registry.collect();

        let overall_status = self.registry.compute_status();

        LockServerHealthEntry {
            creation_date: now.clone(),
            event_time: now,
            service: self
                .app_name
                .as_ref()
                .map_or_else(String::new, |n| n.0.to_string()),
            node_name: self.pdp_id.to_string(),
            status: overall_status.to_string(),
            engine_status,
        }
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::lock::{health_registry::HealthStatus, transport::TransportResult};
    use std::sync::Arc;
    use url::Url;

    /// No-op transport that does nothing on send.
    struct NoopTransport;

    #[async_trait::async_trait]
    impl AuditTransport for NoopTransport {
        async fn send(
            &self,
            _entries: &[AuditItem],
            _audit_kind: &AuditKind,
        ) -> TransportResult<()> {
            Ok(())
        }
    }

    /// Build a minimal [`HealthTicker`] with the given registry and a noop transport.
    /// The other fields use dummy values since `build_health_entry` ignores them.
    fn ticker_with_registry(registry: HealthRegistry) -> HealthTicker<NoopTransport> {
        HealthTicker {
            transport: Arc::new(NoopTransport),
            health_url: Url::parse("http://localhost/health").unwrap(),
            pdp_id: PdpID::new(),
            app_name: None,
            interval: Duration::from_secs(10),
            logger: None,
            registry,
        }
    }

    #[test]
    fn test_build_health_entry_empty() {
        let registry = HealthRegistry::new();
        let ticker = ticker_with_registry(registry);
        let entry = ticker.build_health_entry();
        assert_eq!(
            entry.status, "unknown",
            "empty registry should report 'unknown'"
        );
        assert!(
            entry.engine_status.is_empty(),
            "empty registry should have empty engine_status"
        );
    }

    #[test]
    fn test_build_health_entry_all_success() {
        let registry = HealthRegistry::new();
        registry.register("core", || HealthStatus::Success);
        registry.register("data", || HealthStatus::Success);
        let ticker = ticker_with_registry(registry);
        let entry = ticker.build_health_entry();
        assert_eq!(
            entry.status, "running",
            "all success should report 'running'"
        );
        assert_eq!(entry.engine_status.len(), 2);
    }

    #[test]
    fn test_build_health_entry_any_failure() {
        let registry = HealthRegistry::new();
        registry.register("core", || HealthStatus::Success);
        registry.register("data", || HealthStatus::Failure);
        let ticker = ticker_with_registry(registry);
        let entry = ticker.build_health_entry();
        assert_eq!(
            entry.status, "degraded",
            "any failure should report 'degraded'"
        );
    }

    #[test]
    fn test_build_health_entry_all_failure() {
        let registry = HealthRegistry::new();
        registry.register("core", || HealthStatus::Failure);
        registry.register("data", || HealthStatus::Failure);
        let ticker = ticker_with_registry(registry);
        let entry = ticker.build_health_entry();
        assert_eq!(
            entry.status, "degraded",
            "all failure should report 'degraded'"
        );
    }

    #[test]
    fn test_build_health_entry_mixed() {
        let registry = HealthRegistry::new();
        registry.register("core", || HealthStatus::Success);
        registry.register("policy_store", || HealthStatus::Success);
        registry.register("trusted_issuers", || HealthStatus::Failure);
        registry.register("data", || HealthStatus::Success);
        let ticker = ticker_with_registry(registry);
        let entry = ticker.build_health_entry();
        assert_eq!(
            entry.status, "degraded",
            "mixed status with any failure should report 'degraded'"
        );
        assert_eq!(entry.engine_status.len(), 4);
        assert_eq!(
            entry.engine_status.get("core"),
            Some(&HealthStatus::Success)
        );
        assert_eq!(
            entry.engine_status.get("trusted_issuers"),
            Some(&HealthStatus::Failure)
        );
    }
}
