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
use super::transport::{AuditKind, AuditTransport};
use crate::app_types::{ApplicationName, PdpID};
use crate::http::{JoinHandle, spawn_task};
use crate::lock::LockLogEntry;
use crate::log::{LogWriter, LoggerWeak};

pub(crate) struct HealthTicker<T: AuditTransport> {
    transport: Arc<T>,
    health_url: Url,
    pdp_id: String,
    app_name: String,
    interval: Duration,
    logger: Option<LoggerWeak>,
    registry: HealthRegistry,
}

impl<T: AuditTransport + 'static> HealthTicker<T> {
    pub(crate) fn spawn(
        transport: Arc<T>,
        health_url: Url,
        pdp_id: PdpID,
        app_name: Option<ApplicationName>,
        interval: Duration,
        logger: Option<LoggerWeak>,
        registry: HealthRegistry,
    ) -> (CancellationToken, JoinHandle<()>) {
        let cancel_tkn = CancellationToken::new();
        let ticker = Self {
            transport,
            health_url,
            pdp_id: pdp_id.to_string(),
            app_name: app_name.map_or_else(String::new, |n| n.0),
            interval,
            logger,
            registry,
        };
        let handle = spawn_task(ticker.run(cancel_tkn.clone()));
        (cancel_tkn, handle)
    }

    pub(crate) async fn run(self, cancel_tkn: CancellationToken) {
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

        let serialized = match serde_json::to_string(&entry) {
            Ok(s) => s.into_boxed_str(),
            Err(e) => {
                logger.log_any(LockLogEntry::error(format!(
                    "failed to serialize health entry: {e}"
                )));
                return;
            },
        };

        let result = self
            .transport
            .send(&[serialized], &AuditKind::Health(self.health_url.clone()))
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

        let overall_status = if engine_status.is_empty() {
            "unknown"
        } else if engine_status.values().all(|s| s == "success") {
            "running"
        } else {
            "degraded"
        }
        .to_string();

        LockServerHealthEntry {
            creation_date: now.clone(),
            event_time: now,
            service: self.app_name.clone(),
            node_name: self.pdp_id.clone(),
            status: overall_status,
            engine_status,
        }
    }
}
