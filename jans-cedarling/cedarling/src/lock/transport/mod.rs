// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Transport abstraction for Lock Server communication.
//!
//! This module provides a unified interface for sending audit data to the Lock Server,
//! supporting both REST and gRPC transports.

use std::{fmt::Debug, future::Future};

#[cfg(feature = "grpc")]
pub(super) mod grpc;
pub(super) mod rest;

/// Audit log entry to be sent to the Lock Server.
///
/// This is a serialized JSON string representation of the log entry.
pub(super) type SerializedLogEntry = Box<str>;

/// Result type for transport operations.
pub(super) type TransportResult<T> = Result<T, TransportError>;

/// Trait for transports that can send audit logs to the Lock Server.
pub(super) trait AuditTransport: Send + Sync + Debug {
    /// Sends a batch of serialized log entries to the Lock Server.
    ///
    /// The entries are JSON-serialized strings that will be sent as a batch.
    /// Returns `Ok(())` on success, or a `TransportError` on failure.
    fn send_logs(
        &self,
        entries: &[SerializedLogEntry],
    ) -> impl Future<Output = TransportResult<()>>;
}

/// Errors that can occur during transport operations.
#[derive(Debug, thiserror::Error)]
pub enum TransportError {
    #[error("REST transport error: {0}")]
    Rest(#[from] reqwest::Error),

    #[error("gRPC transport error: {0}")]
    #[cfg(feature = "grpc")]
    Grpc(#[from] tonic::Status),

    #[error("gRPC connection error: {0}")]
    #[cfg(feature = "grpc")]
    GrpcConnection(#[from] tonic::transport::Error),

    #[error("gRPC server responded with an error: {0}")]
    #[cfg(feature = "grpc")]
    GrpcServer(String),

    #[error("infalid gRPC endpoint URL")]
    #[cfg(feature = "grpc")]
    InvalidUri,

    #[error("serialization error: {0}")]
    Serialization(String),
}
