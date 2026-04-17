// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;
use std::time::Duration;

use tokio::time::interval;
use tokio_util::sync::CancellationToken;

use crate::authz::metrics::MetricsCollector;
use crate::http::{JoinHandle, spawn_task};
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LoggerWeak, MetricsLogEntry};

pub(crate) struct TelemetryTicker {
    metrics: Arc<MetricsCollector>,
    logger: Option<LoggerWeak>,
    interval: Duration,
}

impl TelemetryTicker {
    pub(crate) fn spawn(
        metrics: Arc<MetricsCollector>,
        logger: Option<LoggerWeak>,
        interval: Duration,
        cancel_tkn: &CancellationToken,
    ) -> JoinHandle<()> {
        let ticker = Self {
            metrics,
            logger,
            interval,
        };

        spawn_task(ticker.run(cancel_tkn.clone()))
    }

    pub(crate) async fn run(self, cancel_tkn: CancellationToken) {
        let mut ticker = interval(self.interval);
        ticker.tick().await;
        loop {
            tokio::select! {
                _ = ticker.tick() => {
                    self.emit_snapshot();
                }
                () = cancel_tkn.cancelled() => {
                    self.emit_snapshot();
                    return;
                }
            }
        }
    }

    fn emit_snapshot(&self) {
        let snapshot = self.metrics.snapshot_and_reset();
        let entry = MetricsLogEntry {
            base: BaseLogEntry::new_metric_opt_request_id(None),
            policy_stats: snapshot.policy_stats,
            error_counters: snapshot.error_counters,
            operational_stats: snapshot.operational_stats,
            interval_secs: snapshot.interval_secs,
        };
        self.logger.log_any(entry);
    }
}
