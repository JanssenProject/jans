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
use crate::http_utils::{AttemptFailure, FinalFailure, RetryableFailure};
#[cfg(feature = "grpc")]
use crate::lock::grpc_failure::{GrpcFailure, GrpcRefusal};
use crate::lock::health_registry::SystemHealth;
use crate::log::{DecisionLogEntry, MetricsLogEntry};
use mapping::LockServerHealthEntry;

#[cfg(feature = "grpc")]
pub(super) mod grpc;
pub(super) mod mapping;
pub(super) mod rest;
#[cfg(test)]
pub(super) mod test_utils;

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
    pub status: Option<SystemHealth>,
}

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
    /// A batch in the wire form of one endpoint, built once and sent on every attempt.
    type Batch: Send + Sync;
    type PrepareError: std::error::Error + Send;

    /// Maps `entries` into the Lock Server wire shape for `audit_kind`, off the
    /// authorization hot path. Each entry that cannot be mapped is logged once, here.
    fn prepare(
        &self,
        entries: &[AuditItem],
        audit_kind: &AuditKind,
    ) -> Result<Self::Batch, Self::PrepareError>;

    /// Sends a prepared batch to the endpoint it was prepared for.
    async fn send(&self, batch: &Self::Batch) -> Result<(), SendFailure>;
}

/// One audit attempt that failed, sorted once by whether repeating it can help.
#[derive(Debug, thiserror::Error)]
pub(super) enum SendFailure {
    #[error(transparent)]
    Retryable(RetryableSend),
    #[error(transparent)]
    Final(FinalSend),
}

/// What the network or a failing Lock may answer differently next time.
#[derive(Debug, thiserror::Error)]
pub(super) enum RetryableSend {
    #[error(transparent)]
    Http(RetryableFailure),
    #[cfg(feature = "grpc")]
    #[error("gRPC {:?}: {}", .0.code(), .0.message())]
    Grpc(tonic::Status),
}

/// A refusal the same batch gets on every attempt. `401` is one: the token is
/// fixed when the transport is built, so each attempt would send the same one.
#[derive(Debug, thiserror::Error)]
pub(super) enum FinalSend {
    #[error(transparent)]
    Http(FinalFailure),
    #[cfg(feature = "grpc")]
    #[error(transparent)]
    Grpc(GrpcRefusal),
    /// Lock answered the gRPC call with `success = false`.
    #[cfg(feature = "grpc")]
    #[error("Lock reported failure: {message}")]
    Unsuccessful { message: String },
}

impl From<AttemptFailure> for SendFailure {
    fn from(failure: AttemptFailure) -> Self {
        match failure {
            AttemptFailure::Retryable(failure) => Self::Retryable(RetryableSend::Http(failure)),
            AttemptFailure::Final(failure) => Self::Final(FinalSend::Http(failure)),
        }
    }
}

#[cfg(feature = "grpc")]
impl From<GrpcFailure> for SendFailure {
    fn from(failure: GrpcFailure) -> Self {
        match failure {
            GrpcFailure::Retryable(status) => Self::Retryable(RetryableSend::Grpc(status)),
            GrpcFailure::Final(refusal) => Self::Final(FinalSend::Grpc(refusal)),
        }
    }
}

/// Why a gRPC transport could not be built.
#[derive(Debug, thiserror::Error)]
pub enum TransportError {
    #[cfg(not(target_arch = "wasm32"))]
    #[cfg(feature = "grpc")]
    #[error("gRPC connection error: {0}")]
    GrpcConnection(#[from] tonic::transport::Error),

    #[error("invalid gRPC endpoint URL: {0}")]
    #[cfg(feature = "grpc")]
    InvalidUri(String),

    #[error("failed to parse access token: {0}")]
    #[cfg(feature = "grpc")]
    InvalidAccessToken(#[from] tonic::metadata::errors::InvalidMetadataValue),
}
