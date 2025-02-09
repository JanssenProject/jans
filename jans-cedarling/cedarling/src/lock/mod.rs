// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Lock Engine
//!
//! Janssen Lock (or just "Lock") provides a centralized control plane for domains to use Cedar to secure a network of distributed applications and audit the activity of both people and software.
//! [link to the documentation](https://docs.jans.io/head/admin/lock/)

mod audit;
mod telemetry;

pub(crate) use audit::AuditIntervals;
use tokio_util::sync::CancellationToken;
use tokio_util::task::TaskTracker;

use crate::init::service_config::LockConfig;
use crate::log::Logger;

pub(crate) struct LockService {
    audit_tasks_tracker: TaskTracker,
    cancellation_tkn: CancellationToken,
}

impl LockService {
    pub fn new(config: LockConfig, audit_intervals: AuditIntervals, logger: Logger) -> Self {
        let cancellation_tkn = CancellationToken::new();

        let _telemetry_tracker = Self::init_sse_handler(config.sse_uri.clone());

        let audit_tasks_tracker =
            Self::init_audit(config, audit_intervals, logger, cancellation_tkn.clone());

        Self {
            audit_tasks_tracker,
            cancellation_tkn,
        }
    }

    /// Closes connections to the lock server
    pub async fn close(&self) {
        self.cancellation_tkn.cancel();
        self.audit_tasks_tracker.wait().await;
    }
}

#[derive(Debug, thiserror::Error)]
pub enum LockError {
    #[error("failed to send HTTP request to lock server: {0}")]
    HttpRequestFailed(#[source] reqwest::Error),
    #[error("received an HTTP error response from the lock server: {0}")]
    HttpErrResponse(#[source] reqwest::Error),
    #[error("failed to serialize audit message to a JSON string: {0}")]
    SerializeAuditMsg(#[from] serde_json::Error),
}
