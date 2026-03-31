// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Transport abstraction for Lock Server communication.
//!
//! This module provides a unified interface for sending audit data to the Lock Server,
//! supporting both REST and gRPC transports.

use async_trait::async_trait;

#[cfg(feature = "grpc")]
pub(super) mod grpc;
pub(super) mod mapping;
pub(super) mod rest;

/// Audit log entry to be sent to the Lock Server.
///
/// This is a serialized JSON string representation of the log entry.
pub(super) type SerializedLogEntry = Box<str>;

/// Result type for transport operations.
pub(super) type TransportResult<T> = Result<T, TransportError>;

/// Trait for transports that can send audit logs to the Lock Server.
#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
pub(super) trait AuditTransport: Send + Sync {
    /// Sends a batch of serialized log entries to the Lock Server.
    ///
    /// The entries are JSON-serialized strings that will be sent as a batch.
    /// Returns `Ok(())` on success, or a `TransportError` on failure.
    async fn send_logs(&self, entries: &[SerializedLogEntry]) -> TransportResult<()>;
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
