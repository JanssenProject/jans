// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Transport abstraction for Lock Server communication.
//!
//! This module provides a unified interface for sending audit data to the Lock Server,
//! supporting both REST and gRPC transports.

use std::fmt::Display;

use async_trait::async_trait;
use url::Url;

use crate::app_types::{ApplicationName, PdpID};
use crate::log::{DecisionLogEntry, MetricsLogEntry};
use mapping::LockServerHealthEntry;

#[cfg(feature = "grpc")]
pub(super) mod grpc;
pub(super) mod mapping;
pub(super) mod rest;

/// Typed audit payload dispatched to the Lock Server
#[derive(Clone)]
pub(crate) enum AuditPayload {
    Decision(Box<DecisionLogEntry>),
    Metric(Box<MetricsLogEntry>),
    Health(Box<LockServerHealthEntry>),
}

/// A typed audit item queued for delivery to the Lock Server.
///
/// The authorization hot path pushes the concrete log entry through the channel
/// without any JSON work. All serialization happens later in the transport,
/// off the hot path (see [`AuditTransport::send`])
#[derive(Clone)]
pub(crate) struct AuditItem {
    pub payload: AuditPayload,
    pub pdp_id: PdpID,
    pub app_name: Option<ApplicationName>,
}

/// Result type for transport operations.
pub(super) type TransportResult<T> = Result<T, TransportError>;

/// Discriminates between audit channels and carries the target URL.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(super) enum AuditKind {
    Log(Url),
    Telemetry(Url),
    Health(Url),
}

impl AuditKind {
    pub(super) fn url(&self) -> &Url {
        match self {
            AuditKind::Log(url) | AuditKind::Telemetry(url) | AuditKind::Health(url) => url,
        }
    }
}

impl Display for AuditKind {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            AuditKind::Log(_) => write!(f, "log"),
            AuditKind::Telemetry(_) => write!(f, "telemetry"),
            AuditKind::Health(_) => write!(f, "health"),
        }
    }
}

/// Trait for transports that can send audit logs to the Lock Server.
#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
pub(super) trait AuditTransport: Send + Sync {
    /// Send a batch of typed audit items to the given audit endpoint using [`AuditKind`].
    ///
    /// Mapping into the Lock Server wire shape and JSON/protobuf serialization both
    /// happen here, off the authorization hot path.
    async fn send(&self, entries: &[AuditItem], audit_kind: &AuditKind) -> TransportResult<()>;
}

/// Errors that can occur during transport operations.
#[derive(Debug, thiserror::Error)]
pub enum TransportError {
    #[error("REST transport error: {0}")]
    Rest(#[from] reqwest::Error),

    #[error("gRPC transport error: {0}")]
    #[cfg(feature = "grpc")]
    Grpc(#[from] tonic::Status),

    #[cfg(not(target_arch = "wasm32"))]
    #[cfg(feature = "grpc")]
    #[error("gRPC connection error: {0}")]
    GrpcConnection(#[from] tonic::transport::Error),

    #[error("gRPC server responded with an error: {0}")]
    #[cfg(feature = "grpc")]
    GrpcServer(String),

    #[error("invalid gRPC endpoint URL: {0}")]
    #[cfg(feature = "grpc")]
    InvalidUri(String),

    #[error("failed to parse access token: {0}")]
    #[cfg(feature = "grpc")]
    InvalidAccessToken(#[from] tonic::metadata::errors::InvalidMetadataValue),

    #[error("serialization error: {0}")]
    Serialization(String),
}
