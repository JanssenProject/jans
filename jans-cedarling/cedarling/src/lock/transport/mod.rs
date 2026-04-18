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

#[cfg(feature = "grpc")]
pub(super) mod grpc;
pub(super) mod mapping;
pub(super) mod rest;

/// Audit log entry to be sent to the Lock Server.
///
/// This is a serialized JSON string representation of the log entry.
pub(super) type SerializedAuditEntry = Box<str>;

/// Result type for transport operations.
pub(super) type TransportResult<T> = Result<T, TransportError>;

/// Discriminates between audit channels and carries the target URL.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(super) enum AuditKind {
    Log(Url),
    Telemetry(Url),
}

impl AuditKind {
    pub(super) fn url(&self) -> &Url {
        match self {
            AuditKind::Log(url) | AuditKind::Telemetry(url) => url,
        }
    }
}

impl Display for AuditKind {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            AuditKind::Log(_) => write!(f, "log"),
            AuditKind::Telemetry(_) => write!(f, "telemetry"),
        }
    }
}

/// Trait for transports that can send audit logs to the Lock Server.
#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
pub(super) trait AuditTransport: Send + Sync {
    /// Send a batch of serialized entries to the given audit endpoint using [`AuditKind`].
    async fn send(
        &self,
        entries: &[SerializedAuditEntry],
        audit_kind: &AuditKind,
    ) -> TransportResult<()>;
}

pub(super) fn deserialize_entries<T, S>(
    entries: &[SerializedAuditEntry],
    label: &str,
    log_warn: impl Fn(String),
) -> Result<Vec<T>, TransportError>
where
    T: TryFrom<S>,
    S: serde::de::DeserializeOwned,
    <T as TryFrom<S>>::Error: std::fmt::Display,
{
    if entries.is_empty() {
        return Ok(Vec::new());
    }

    let parsed: Vec<T> = entries
        .iter()
        .enumerate()
        .filter_map(|(idx, v)| match serde_json::from_str::<S>(v) {
            Ok(s) => match T::try_from(s) {
                Ok(t) => Some(t),
                Err(e) => {
                    log_warn(format!("failed to convert {label} entry[{idx}]: {e}"));
                    None
                },
            },
            Err(e) => {
                log_warn(format!("failed to parse {label} entry[{idx}]: {e}"));
                None
            },
        })
        .collect();

    if parsed.is_empty() {
        return Err(TransportError::Serialization(format!(
            "all {label} entries were malformed, nothing to send"
        )));
    }

    Ok(parsed)
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
