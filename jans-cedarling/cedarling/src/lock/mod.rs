// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Lock Server Integration Module
//!
//! This module provides integration with the Lock Server for centralized logging and monitoring.
//! It includes support for Software Statement Assertion (SSA) JWT validation for secure client registration.
//! The module supports both REST and gRPC transport protocols for communicating with the Lock Server.
//!
//! ## Overview
//!
//! The Lock Server integration allows Cedarling to:
//! - Send authorization decision logs to a centralized Lock Server
//! - Register as a client using Dynamic Client Registration (DCR)
//! - Validate SSA JWTs for enhanced security
//! - Handle authentication and authorization with the Lock Server
//! - Choose between REST and gRPC transport protocols
//!
//! ## Architecture
//!
//! ### Components
//!
//! - **[`LockService`]**: Main service that manages communication with the Lock Server
//! - **[`LogWorker`]**: Background worker that sends logs to the Lock Server
//! - **[`LockTransport`]**: Transport protocol selection (REST or gRPC)
//! - **Transport Layer**: Abstraction for sending logs via [`RestTransport`] or [`GrpcTransport`]
//! - **SSA Validation**: Validates Software Statement Assertion JWTs
//! - **Client Registration**: Handles Dynamic Client Registration with the IDP
//!
//! ### Transport Protocols
//!
//! The module supports two transport protocols:
//!
//! - **REST**: HTTP-based transport using JSON payloads
//! - **gRPC**: Protocol Buffers-based transport (requires `grpc` feature)
//!
//! The transport is selected via [`LockServiceConfig.transport`](crate::LockServiceConfig::transport).
//!
//! ### Flow
//!
//! On initialization, [`LockService`] fetches the Lock Server's well-known configuration
//! and then acquires an access token using **one of two mechanisms**:
//!
//! #### Path A – Direct access token (`CEDARLING_LOCK_ACCESS_TOKEN_JWT`)
//!
//! If a pre-issued access token is supplied in `LockServiceConfig::access_token_jwt`,
//! Cedarling uses it directly. The SSA validation and DCR steps are skipped entirely.
//! Primarily intended for testing and local development to simplify the bootstrap
//! flow; may also be used in deployments where token issuance is managed externally.
//! Not available on WASM builds.
//!
//! 1. **Initialization**: [`LockService`] is created with configuration
//! 2. **Lock Config**: Fetch `.well-known/lock-server-configuration`
//! 3. **Access Token**: Use the provided `access_token_jwt` directly
//! 4. **Transport Selection**: Based on `LockTransport` config, either REST or gRPC transport is created
//! 5. **Logging**: Authorization decisions are sent to the Lock Server's audit endpoint
//!
//! #### Path B – SSA → DCR flow (`CEDARLING_LOCK_SSA_JWT`, default)
//!
//! 1. **Initialization**: [`LockService`] is created with configuration
//! 2. **Lock Config**: Fetch `.well-known/lock-server-configuration`
//! 3. **SSA Validation**: If SSA JWT is provided, it's validated against the IDP's JWKS
//! 4. **Client Registration**: DCR request is sent to the IDP (with SSA JWT if available)
//! 5. **Access Token**: Client credentials are obtained for Lock Server communication
//! 6. **Transport Selection**: Based on `LockTransport` config, either REST or gRPC transport is created
//! 7. **Logging**: Authorization decisions are sent to the Lock Server's audit endpoint
//!
//! When both `access_token_jwt` and `ssa_jwt` are set, Path A takes precedence and a warning is logged.
//!
//! ## SSA JWT Validation
//!
//! Software Statement Assertion (SSA) JWTs provide a secure way to register clients
//! with the Identity Provider. The SSA JWT contains claims about the software's
//! identity, permissions, and configuration.
//!
//! ### SSA JWT Structure
//!
//! The SSA JWT must contain the following claims:
//! - `software_id`: Unique identifier for the software
//! - `grant_types`: Array of `OAuth2` grant types the software can use
//! - `org_id`: Organization identifier
//! - `iss`: Issuer of the SSA JWT
//! - `software_roles`: Array of roles/permissions for the software
//! - `exp`: Expiration time
//! - `iat`: Issued at time
//! - `jti`: JWT ID (unique identifier)
//!
//! ### Validation Process
//!
//! 1. **Decode JWT**: Parse and decode the JWT structure
//! 2. **Validate Structure**: Check for required claims and correct data types
//! 3. **Fetch JWKS**: Retrieve JSON Web Key Set from the IDP
//! 4. **Find Key**: Locate the appropriate key using the JWT's `kid` header
//! 5. **Verify Signature**: Validate the JWT signature using the public key
//! 6. **Validate Claims**: Check expiration and other claims
//!
//! ## Error Handling
//!
//! The module provides comprehensive error handling for various scenarios:
//!
//! - **`InvalidSsaJwt`**: SSA JWT validation failed
//! - **`GetLockConfig`**: Failed to retrieve Lock Server configuration
//! - **`ClientRegistration`**: Failed to register client with IDP
//! - **`InitHttpClient`**: Failed to initialize HTTP client
//! - **`TransportError`**: Transport layer error (REST or gRPC)
//! - **`MissingGrpcEndpoint`**: gRPC endpoint not configured when using gRPC transport
//! - **`InvalidAccessToken`**: Failed to parse access token
//!
//! ## Integration with IDP
//!
//! The Lock Server integration works with Identity Providers that support:
//!
//! - Dynamic Client Registration (RFC 7591)
//! - `OAuth2` Client Credentials flow
//! - JSON Web Key Set (JWKS) endpoint
//! - Software Statement Assertion (SSA) validation
//!
//! ### IDP Configuration
//!
//! The IDP must be configured to:
//! - Accept DCR requests with SSA JWTs
//! - Validate SSA JWT signatures and claims
//! - Issue access tokens for Lock Server communication
//! - Provide JWKS endpoint for key validation

pub(crate) mod health_registry;
mod health_ticker;
mod lock_config;
mod log_entry;
mod log_worker;
mod register_client;
pub(crate) mod ssa_validation;
mod telemetry_ticker;
mod transport;
pub(crate) use transport::{AuditItem, AuditPayload};

#[cfg(feature = "grpc")]
mod proto {
    // These lints are disabled because they are triggered by the generated code
    #![allow(unreachable_pub)]
    #![allow(clippy::pedantic)]
    tonic::include_proto!("io.jans.lock.audit");
}

use crate::app_types::{ApplicationName, PdpID};
use crate::authz::metrics::MetricsCollector;
use crate::common::issuer_utils::IssClaim;
use crate::http::{HttpClient, InitializeHttpClientError};
use crate::lock::health_registry::HealthRegistry;
use crate::lock::health_ticker::{HealthTicker, HealthTickerParams};
use crate::lock::lock_config::GetLockConfigError;
use crate::lock::telemetry_ticker::TelemetryTicker;
use crate::log::{LogWriter, LoggerWeak};
use crate::{HttpClientConfig, LockServiceConfig, LockTransport};
use lock_config::LockConfig;
use log_entry::LockLogEntry;
use log_worker::AuditWorker;
use register_client::{ClientRegistrationError, register_client};
use reqwest::Client;
use reqwest::header::{HeaderMap, HeaderValue};
use ssa_validation::validate_ssa_jwt;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;
use transport::{AuditKind, rest::RestTransport};

/// The base duration to wait for if an http request fails for workers.
pub(crate) const WORKER_HTTP_RETRY_DUR: Duration = Duration::from_secs(10);

#[derive(Debug)]
struct WorkerSenderAndHandle {
    tx: mpsc::Sender<AuditItem>,
    handle: crate::http::JoinHandle<()>,
}

/// Stores logs in a buffer then sends them to the lock server in the background
#[derive(Debug)]
pub(crate) struct LockService {
    log_worker: Option<WorkerSenderAndHandle>,
    telemetry_worker: Option<WorkerSenderAndHandle>,
    telemetry_ticker: Option<(CancellationToken, crate::http::JoinHandle<()>)>,
    health_ticker: Option<(CancellationToken, crate::http::JoinHandle<()>)>,
    health_registry: Option<HealthRegistry>,
    logger: Option<LoggerWeak>,
    cancel_tkn: CancellationToken,
}

impl LockService {
    /// Returns the shared health registry for registering subsystem health checks.
    pub(crate) fn health_registry(&self) -> Option<&HealthRegistry> {
        self.health_registry.as_ref()
    }
}

pub(crate) fn init_http_client(
    default_headers: Option<HeaderMap>,
    accept_invalid_certs: bool,
    conf: HttpClientConfig,
) -> Result<HttpClient, InitializeHttpClientError> {
    let headers = default_headers.unwrap_or_default();
    let builder = Client::builder();

    let builder = if accept_invalid_certs {
        // NOTE: WASM builds fail when calling .danger_accept_invalid_certs
        #[cfg(any(target_arch = "wasm32", target_arch = "wasm64"))]
        {
            builder.default_headers(headers)
        }

        #[cfg(not(any(target_arch = "wasm32", target_arch = "wasm64")))]
        {
            builder
                .default_headers(headers)
                .danger_accept_invalid_certs(true)
        }
    } else {
        Client::builder().default_headers(headers)
    };

    HttpClient::new_with_builder(builder, conf)
}

impl LockService {
    pub(crate) async fn new(
        pdp_id: PdpID,
        bootstrap_conf: &LockServiceConfig,
        logger: Option<LoggerWeak>,
        metrics: Arc<MetricsCollector>,
        app_name: Option<ApplicationName>,
        http_conf: HttpClientConfig,
    ) -> Result<Self, InitLockServiceError> {
        let http_client = init_http_client(None, bootstrap_conf.accept_invalid_certs, http_conf)?;

        // Get lock config from the config uri endpoint first
        let lock_config = LockConfig::get(&bootstrap_conf.config_uri, &http_client).await?;

        let access_token = Self::acquire_access_token(
            pdp_id,
            bootstrap_conf,
            &lock_config,
            #[cfg(not(target_arch = "wasm32"))]
            logger.as_ref(),
            http_conf,
            &http_client,
        )
        .await?;

        let cancel_tkn = CancellationToken::new();
        let log_worker = match (bootstrap_conf.log_interval, lock_config.audit_endpoints.log) {
            (Some(log_interval), Some(log_endpoint)) => {
                let worker = create_worker(
                    bootstrap_conf,
                    AuditKind::Log(log_endpoint.0),
                    &access_token,
                    log_interval,
                    logger.clone(),
                    cancel_tkn.clone(),
                    http_conf,
                )?;
                Some(worker)
            },
            _ => None,
        };

        let telemetry_worker = match (
            bootstrap_conf.telemetry_interval,
            lock_config.audit_endpoints.telemetry,
        ) {
            (Some(telemetry_interval), Some(telemetry_endpoint)) => {
                let worker = create_worker(
                    bootstrap_conf,
                    AuditKind::Telemetry(telemetry_endpoint.0),
                    &access_token,
                    telemetry_interval,
                    logger.clone(),
                    cancel_tkn.clone(),
                    http_conf,
                )?;

                Some(worker)
            },
            _ => None,
        };

        let telemetry_ticker = bootstrap_conf.telemetry_interval.map(|telemetry_interval| {
            TelemetryTicker::spawn(metrics, logger.clone(), telemetry_interval)
        });

        let (health_ticker, health_registry) = match (
            bootstrap_conf.health_interval,
            lock_config.audit_endpoints.health,
        ) {
            (Some(health_interval), Some(health_endpoint)) => {
                let registry = HealthRegistry::new();

                let ticker = create_health_ticker(
                    HealthTickerParams {
                        health_url: health_endpoint.0,
                        pdp_id,
                        app_name,
                        health_interval,
                        logger: logger.clone(),
                        registry: registry.clone(),
                    },
                    bootstrap_conf,
                    &access_token,
                    http_conf,
                )?;

                (Some(ticker), Some(registry))
            },
            _ => (None, None),
        };

        Ok(Self {
            log_worker,
            telemetry_worker,
            telemetry_ticker,
            health_ticker,
            health_registry,
            logger,
            cancel_tkn,
        })
    }

    /// Acquire the access token used to authenticate with Lock Server endpoints.
    ///
    /// Two paths are supported:
    ///
    /// 1. **Direct token** (`access_token_jwt` is set): use it as-is, skipping SSA
    ///    validation and Dynamic Client Registration (DCR) entirely. Useful when the
    ///    token is provisioned externally.
    ///
    /// 2. **SSA → DCR flow** (default): optionally validate the SSA JWT, then perform
    ///    DCR to obtain credentials, and exchange them for an access token via the
    ///    client credentials grant.
    ///
    /// If both are set, `access_token_jwt` takes precedence and a warning is emitted.
    async fn acquire_access_token(
        pdp_id: PdpID,
        bootstrap_conf: &LockServiceConfig,
        lock_config: &LockConfig,
        #[cfg(not(target_arch = "wasm32"))] logger: Option<&LoggerWeak>,
        http_conf: HttpClientConfig,
        http_client: &HttpClient,
    ) -> Result<String, InitLockServiceError> {
        // Direct pre-issued access token path is not supported on WASM builds.
        #[cfg(not(target_arch = "wasm32"))]
        if let Some(direct_token) = &bootstrap_conf.access_token_jwt {
            if bootstrap_conf.ssa_jwt.is_some() {
                logger.cloned().log_any(LockLogEntry::warn(
                    "Both CEDARLING_LOCK_ACCESS_TOKEN_JWT and CEDARLING_LOCK_SSA_JWT are set. \
                     CEDARLING_LOCK_ACCESS_TOKEN_JWT takes precedence; the SSA → DCR flow will \
                     be skipped."
                        .to_string(),
                ));
            }
            return Ok(direct_token.clone());
        }

        // Validate SSA JWT if provided
        if let Some(ssa_jwt) = &bootstrap_conf.ssa_jwt {
            let jwks_uri = format!(
                "{}/jans-auth/restv1/jwks",
                IssClaim::new(&lock_config.issuer_oidc_url.0.origin().ascii_serialization())
                    .as_str()
            );
            validate_ssa_jwt(ssa_jwt, &jwks_uri, http_client)
                .await
                .map_err(|e| {
                    InitLockServiceError::InvalidSsaJwt(format!("SSA JWT validation failed: {e}"))
                })?;
        }

        // Register client via DCR, obtaining an access token
        let client_creds = register_client(
            pdp_id,
            &lock_config.issuer_oidc_url,
            bootstrap_conf.ssa_jwt.as_ref(),
            bootstrap_conf.accept_invalid_certs,
            http_conf,
        )
        .await?;

        Ok(client_creds.access_token)
    }

    pub(crate) async fn shut_down(&mut self) {
        if let Some((cancel_tkn, handle)) = self.telemetry_ticker.take() {
            cancel_tkn.cancel();
            () = handle.await_result().await;
        }
        if let Some((cancel_tkn, handle)) = self.health_ticker.take() {
            cancel_tkn.cancel();
            () = handle.await_result().await;
        }

        self.cancel_tkn.cancel();
        if let Some(worker) = self.log_worker.take() {
            () = worker.handle.await_result().await;
        }
        if let Some(worker) = self.telemetry_worker.take() {
            () = worker.handle.await_result().await;
        }
    }
}

impl LockService {
    /// Queue a typed audit item for delivery to the Lock Server
    pub(crate) fn dispatch_audit(&self, item: AuditItem) {
        let (worker, worker_name) = match &item.payload {
            AuditPayload::Decision(_) => (self.log_worker.as_ref(), "log"),
            AuditPayload::Metric(_) => (self.telemetry_worker.as_ref(), "telemetry"),
            AuditPayload::Health(_) => return,
        };

        let Some(WorkerSenderAndHandle { tx, .. }) = worker else {
            return;
        };

        if let Err(err) = tx.try_send(item) {
            self.logger.log_any(LockLogEntry::error(format!(
                "{worker_name} channel send failed (full or closed): {err}"
            )));
        }
    }
}

fn create_worker(
    bootstrap_conf: &LockServiceConfig,
    audit_kind: AuditKind,
    access_token: &str,
    audit_interval: Duration,
    logger: Option<LoggerWeak>,
    cancel_tkn: CancellationToken,
    http_conf: HttpClientConfig,
) -> Result<WorkerSenderAndHandle, InitLockServiceError> {
    let (tx, rx) = mpsc::channel::<AuditItem>(bootstrap_conf.log_channel_capacity.max(1));

    match bootstrap_conf.transport {
        LockTransport::Rest => {
            // Build a default http client that already has the access_token in the headers
            let mut headers = HeaderMap::new();
            let mut auth_header = HeaderValue::from_str(&format!("Bearer {access_token}"))
                .map_err(|e| InitLockServiceError::InvalidAccessToken(e.to_string()))?;
            auth_header.set_sensitive(true);
            headers.insert("Authorization", auth_header);

            let http_client = init_http_client(
                Some(headers),
                bootstrap_conf.accept_invalid_certs,
                http_conf,
            )?;

            let transport = Arc::new(RestTransport::new(
                http_client,
                logger.as_ref().and_then(std::sync::Weak::upgrade),
            ));

            let mut worker = AuditWorker::new(
                audit_interval,
                transport,
                audit_kind,
                logger,
                bootstrap_conf.log_max_retries,
            );
            let handle = crate::http::spawn_task(async move { worker.run(rx, cancel_tkn).await });

            Ok(WorkerSenderAndHandle { tx, handle })
        },
        #[cfg(feature = "grpc")]
        LockTransport::Grpc => {
            use transport::grpc::GrpcTransport;

            let transport = Arc::new(GrpcTransport::new(
                audit_kind.url().origin().ascii_serialization(),
                access_token,
                logger.as_ref().and_then(std::sync::Weak::upgrade),
            )?);

            let mut worker = AuditWorker::new(
                audit_interval,
                transport,
                audit_kind,
                logger,
                bootstrap_conf.log_max_retries,
            );

            let handle = crate::http::spawn_task(async move { worker.run(rx, cancel_tkn).await });

            Ok(WorkerSenderAndHandle { tx, handle })
        },
    }
}

fn create_health_ticker(
    params: HealthTickerParams,
    bootstrap_conf: &LockServiceConfig,
    access_token: &str,
    http_conf: HttpClientConfig,
) -> Result<(CancellationToken, crate::http::JoinHandle<()>), InitLockServiceError> {
    match bootstrap_conf.transport {
        LockTransport::Rest => {
            let mut headers = HeaderMap::new();
            let mut auth_header = HeaderValue::from_str(&format!("Bearer {access_token}"))
                .map_err(|e| InitLockServiceError::InvalidAccessToken(e.to_string()))?;
            auth_header.set_sensitive(true);
            headers.insert("Authorization", auth_header);

            let http_client = init_http_client(
                Some(headers),
                bootstrap_conf.accept_invalid_certs,
                http_conf,
            )?;

            let transport = Arc::new(RestTransport::new(
                http_client,
                params.logger.as_ref().and_then(std::sync::Weak::upgrade),
            ));

            Ok(HealthTicker::spawn(transport, params))
        },
        #[cfg(feature = "grpc")]
        LockTransport::Grpc => {
            use transport::grpc::GrpcTransport;

            let transport = Arc::new(GrpcTransport::new(
                params.health_url.origin().ascii_serialization(),
                access_token,
                params.logger.as_ref().and_then(std::sync::Weak::upgrade),
            )?);

            Ok(HealthTicker::spawn(transport, params))
        },
    }
}

#[derive(Debug, thiserror::Error)]
pub enum InitLockServiceError {
    #[error("init http lock client: {0}")]
    InitHttpClient(#[from] InitializeHttpClientError),
    #[error("the provided CEDARLING_LOCK_SSA_JWT is either malformed or expired: {0}")]
    InvalidSsaJwt(String),
    #[error(
        "failed to GET lock server config from the `.well-known/lock-server-configuration` endpoint: {0}"
    )]
    GetLockConfig(#[from] GetLockConfigError),
    #[error("failed to dynamically register client for the Lock server's auth: {0}")]
    ClientRegistration(#[from] ClientRegistrationError),
    #[error("transport error: {0}")]
    TransportError(#[from] transport::TransportError),
    #[error("failed to parse access token: {0}")]
    InvalidAccessToken(String),
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::{
        LogLevel,
        lock::{register_client::DCR_SCOPE, transport::test_utils::sample_log_item},
    };
    use jsonwebtoken::{EncodingKey, Header, encode};
    use mockito::{Mock, Server, ServerGuard};
    use serde_json::json;
    use std::time::Duration;
    use tokio::time::sleep;

    fn generate_test_jwt() -> String {
        let mut header = Header::new(jsonwebtoken::Algorithm::HS256);
        header.kid = Some("test-kid".to_string());
        let claims = json!({
            "software_id": "test_software",
            "grant_types": ["client_credentials"],
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": ["cedarling"],
            "exp": 9_999_999_999_u64,
            "iat": 1_111_111_111_u64,
            "jti": "test-jti-123"
        });
        encode(&header, &claims, &EncodingKey::from_secret(b"test-key")).unwrap()
    }

    #[tokio::test]
    async fn test_lock_service() {
        let pdp_id = PdpID::new();
        let ssa_jwt = generate_test_jwt();

        let mut mock_idp_server = Server::new_async().await;
        let mut mock_lock_server = Server::new_async().await;

        let (lock_config_uri, lock_config_endpoint) =
            mock_lock_config_endpoint(&mut mock_lock_server, &mock_idp_server);
        let oidc_endpoint = mock_oidc_endpoint(&mut mock_idp_server);
        let jwks_endpoint = mock_jwks_endpoint(&mut mock_idp_server);
        let dcr_endpoint = mock_dcr_endpoint(&mut mock_idp_server, pdp_id, &ssa_jwt);
        let token_endpoint = mock_token_endpoint(&mut mock_idp_server);
        let log_endpoint = mock_log_endpoint(&mut mock_lock_server);

        let config = LockServiceConfig {
            config_uri: lock_config_uri,
            dynamic_config: false,
            ssa_jwt: Some(ssa_jwt),
            log_interval: Some(Duration::from_millis(100)),
            health_interval: None,
            telemetry_interval: None,
            listen_sse: false,
            log_level: LogLevel::TRACE,
            accept_invalid_certs: true, // Allow invalid certs for testing
            transport: LockTransport::Rest,
            ..Default::default()
        };

        // Test startup
        let metrics = Arc::new(MetricsCollector::new(0));
        let logger = LockService::new(
            pdp_id,
            &config,
            None,
            metrics,
            None,
            HttpClientConfig::default(),
        )
        .await
        .expect("build lock logger");
        lock_config_endpoint.assert();
        oidc_endpoint.assert();
        jwks_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();

        // Test if logs are getting sent
        logger.dispatch_audit(sample_log_item());

        // Sleep until the logs are sent
        sleep(Duration::from_secs(1)).await;

        // Check if logs were sent
        log_endpoint.assert();
    }

    #[tokio::test]
    async fn test_lock_service_without_ssa() {
        let pdp_id = PdpID::new();

        let mut mock_idp_server = Server::new_async().await;
        let mut mock_lock_server = Server::new_async().await;

        let (lock_config_uri, lock_config_endpoint) =
            mock_lock_config_endpoint(&mut mock_lock_server, &mock_idp_server);
        let oidc_endpoint = mock_oidc_endpoint(&mut mock_idp_server);
        let dcr_endpoint = mock_dcr_endpoint_without_ssa(&mut mock_idp_server, pdp_id);
        let token_endpoint = mock_token_endpoint(&mut mock_idp_server);
        let log_endpoint = mock_log_endpoint(&mut mock_lock_server);

        let config = LockServiceConfig {
            config_uri: lock_config_uri,
            dynamic_config: false,
            ssa_jwt: None,
            log_interval: Some(Duration::from_millis(100)),
            health_interval: None,
            telemetry_interval: None,
            listen_sse: false,
            log_level: LogLevel::TRACE,
            accept_invalid_certs: false,
            transport: LockTransport::Rest,
            ..Default::default()
        };

        // Test startup without SSA
        let metrics = Arc::new(MetricsCollector::new(0));
        let logger = LockService::new(
            pdp_id,
            &config,
            None,
            metrics,
            None,
            HttpClientConfig::default(),
        )
        .await
        .expect("build lock logger");
        lock_config_endpoint.assert();
        oidc_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();

        // Test if logs are getting sent
        logger.dispatch_audit(sample_log_item());

        // Sleep until the logs are sent
        sleep(Duration::from_secs(1)).await;

        // Check if logs were sent
        log_endpoint.assert();
    }

    #[tokio::test]
    async fn test_lock_service_invalid_ssa() {
        let pdp_id = PdpID::new();
        let invalid_ssa_jwt = "invalid.jwt.token";

        let mock_idp_server = Server::new_async().await;
        let mut mock_lock_server = Server::new_async().await;

        let (lock_config_uri, _) =
            mock_lock_config_endpoint(&mut mock_lock_server, &mock_idp_server);

        let config = LockServiceConfig {
            config_uri: lock_config_uri,
            dynamic_config: false,
            ssa_jwt: Some(invalid_ssa_jwt.to_string()),
            log_interval: Some(Duration::from_millis(100)),
            health_interval: None,
            telemetry_interval: None,
            listen_sse: false,
            log_level: LogLevel::TRACE,
            accept_invalid_certs: false,
            transport: LockTransport::Rest,
            ..Default::default()
        };

        // Test startup with invalid SSA should fail
        let metrics = Arc::new(MetricsCollector::new(0));
        let result = LockService::new(
            pdp_id,
            &config,
            None,
            metrics,
            None,
            HttpClientConfig::default(),
        )
        .await;
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            InitLockServiceError::InvalidSsaJwt(_)
        ));
    }

    /// Tests that when `access_token_jwt` is set the SSA → DCR flow is entirely bypassed:
    /// the IDP's JWKS, OIDC, DCR, and token endpoints must NOT be called.
    #[cfg(not(target_arch = "wasm32"))]
    #[tokio::test]
    async fn test_lock_service_with_direct_access_token() {
        let pdp_id = PdpID::new();
        let direct_access_token = "some.pre.issued.access.token"; // gitleaks:allow

        let mut mock_idp_server = Server::new_async().await;
        let mut mock_lock_server = Server::new_async().await;

        let (lock_config_uri, lock_config_endpoint) =
            mock_lock_config_endpoint(&mut mock_lock_server, &mock_idp_server);
        let log_endpoint = mock_log_endpoint(&mut mock_lock_server);

        // These endpoints must NOT be called when access_token_jwt is provided
        let oidc_endpoint = mock_idp_server
            .mock("GET", "/.well-known/openid-configuration")
            .expect(0)
            .create();
        let dcr_endpoint = mock_idp_server
            .mock("POST", "/jans-auth/restv1/register")
            .expect(0)
            .create();
        let token_endpoint = mock_idp_server
            .mock("POST", "/jans-auth/restv1/token")
            .expect(0)
            .create();

        let config = LockServiceConfig {
            config_uri: lock_config_uri,
            dynamic_config: false,
            ssa_jwt: None,
            access_token_jwt: Some(direct_access_token.to_string()),
            log_interval: Some(Duration::from_millis(100)),
            health_interval: None,
            telemetry_interval: None,
            listen_sse: false,
            log_level: LogLevel::TRACE,
            accept_invalid_certs: false,
            transport: LockTransport::Rest,
            ..Default::default()
        };

        let metrics = Arc::new(MetricsCollector::new(0));
        let lock_svc = LockService::new(
            pdp_id,
            &config,
            None,
            metrics,
            None,
            HttpClientConfig::default(),
        )
        .await
        .expect("build lock service with direct access token");

        lock_config_endpoint.assert();
        // DCR/token flow must not have been triggered
        oidc_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();

        // Send a log entry and confirm it reaches the Lock Server
        lock_svc.dispatch_audit(sample_log_item());

        sleep(Duration::from_secs(1)).await;
        log_endpoint.assert();
    }

    /// Tests that when both `access_token_jwt` and `ssa_jwt` are set,
    /// `access_token_jwt` wins and the SSA/DCR flow is skipped.
    #[cfg(not(target_arch = "wasm32"))]
    #[tokio::test]
    async fn test_lock_service_access_token_takes_precedence_over_ssa() {
        let pdp_id = PdpID::new();
        let direct_access_token = "some.pre.issued.access.token"; // gitleaks:allow
        // An SSA JWT is provided but should be ignored
        let ssa_jwt = "some.ssa.jwt.that.should.be.ignored";

        let mut mock_idp_server = Server::new_async().await;
        let mut mock_lock_server = Server::new_async().await;

        let (lock_config_uri, lock_config_endpoint) =
            mock_lock_config_endpoint(&mut mock_lock_server, &mock_idp_server);

        // DCR / token endpoints must NOT be called
        let oidc_endpoint = mock_idp_server
            .mock("GET", "/.well-known/openid-configuration")
            .expect(0)
            .create();
        let dcr_endpoint = mock_idp_server
            .mock("POST", "/jans-auth/restv1/register")
            .expect(0)
            .create();
        let token_endpoint = mock_idp_server
            .mock("POST", "/jans-auth/restv1/token")
            .expect(0)
            .create();

        let config = LockServiceConfig {
            config_uri: lock_config_uri,
            dynamic_config: false,
            ssa_jwt: Some(ssa_jwt.to_string()),
            access_token_jwt: Some(direct_access_token.to_string()),
            log_interval: None,
            health_interval: None,
            telemetry_interval: None,
            listen_sse: false,
            log_level: LogLevel::TRACE,
            accept_invalid_certs: false,
            transport: LockTransport::Rest,
            ..Default::default()
        };

        let metrics = Arc::new(MetricsCollector::new(0));
        let _lock_svc = LockService::new(
            pdp_id,
            &config,
            None,
            metrics,
            None,
            HttpClientConfig::default(),
        )
        .await
        .expect("build lock service: access_token_jwt should win over ssa_jwt");

        lock_config_endpoint.assert();
        oidc_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();
    }

    /// Mocks the `.well-known/lock-server-configuration` endpoint
    fn mock_lock_config_endpoint(
        lock_server: &mut ServerGuard,
        idp_server: &ServerGuard,
    ) -> (url::Url, Mock) {
        let lock_config_path = "/.well-known/lock-server-configuration";
        let lock_config_uri: url::Url = format!("{}{}", lock_server.url(), lock_config_path)
            .parse()
            .expect("a valid Url for the lock config endpoint");

        let health_endpoint = format!("{}/jans-auth/v1/audit/health", lock_server.url());
        let log_endpoint = format!("{}/jans-auth/v1/audit/log", lock_server.url());
        let telemetry_endpoint = format!("{}/jans-auth/v1/audit/telemetry", lock_server.url());
        let config_endpoint = format!("{}/jans-auth/v1/config", lock_server.url());
        let issuers_endpoint = format!("{}/jans-auth/v1/config/issuers", lock_server.url());
        let policy_endpoint = format!("{}/jans-auth/v1/config/policy", lock_server.url());
        let schema_endpoint = format!("{}/jans-auth/v1/config/schema", lock_server.url());
        let sse_endpoint = format!("{}/jans-auth/v1/sse", lock_server.url());

        let idp_config_uri: url::Url = idp_server
            .url()
            .parse()
            .expect("a valid Url for the idp oidc endpoint");
        let issuer = format!(
            "{}:{}",
            idp_config_uri.host().unwrap(),
            idp_config_uri.port().unwrap(),
        );
        let config_endpoint = lock_server
            .mock("GET", lock_config_path)
            .with_body(
                json!({
                "version": "1.0",
                "issuer": issuer,
                "audit": {
                    "log_endpoint": log_endpoint,
                    "health_endpoint": health_endpoint,
                    "telemetry_endpoint": telemetry_endpoint,
                },
                "config": {
                    "config_endpoint": config_endpoint,
                    "issuers_endpoint": issuers_endpoint,
                    "policy_endpoint": policy_endpoint,
                    "schema_endpoint": schema_endpoint,
                    "sse_endpoint": sse_endpoint
                    }
                })
                .to_string(),
            )
            .expect(1)
            .create();

        (lock_config_uri, config_endpoint)
    }

    /// Mocks the `.well-known/openid-configuration` endpoint
    fn mock_oidc_endpoint(server: &mut ServerGuard) -> Mock {
        let oidc_path = "/.well-known/openid-configuration";

        let registration_endpoint = format!("{}/jans-auth/restv1/register", server.url());
        let token_endpoint = format!("{}/jans-auth/restv1/token", server.url());
        server
            .mock("GET", oidc_path)
            .with_body(
                json!({
                    "registration_endpoint": registration_endpoint,
                    "token_endpoint": token_endpoint,
                })
                .to_string(),
            )
            .expect(1)
            .create()
    }

    /// Mocks the JWKS endpoint for SSA validation
    fn mock_jwks_endpoint(server: &mut ServerGuard) -> Mock {
        let jwks_path = "/jans-auth/restv1/jwks";

        server
            .mock("GET", jwks_path)
            .with_body(
                json!({
                    "keys": [
                        {
                            "kid": "test-kid",
                            "alg": "HS256",
                            "k": "dGVzdC1rZXk" // URL-safe base64 for "test-key" (no padding)
                        }
                    ]
                })
                .to_string(),
            )
            .expect(1)
            .create()
    }

    /// Mocks the dynamic client registration endpoint
    fn mock_dcr_endpoint(server: &mut ServerGuard, pdp_id: PdpID, ssa_jwt: &str) -> Mock {
        let dcr_path = "/jans-auth/restv1/register";

        server
            .mock("POST", dcr_path)
            .match_body(mockito::Matcher::PartialJson(json!({
                "token_endpoint_auth_method": "client_secret_basic",
                "grant_types": ["client_credentials"],
                "client_name": format!("cedarling-{}", pdp_id),
                "scope": DCR_SCOPE,
                "access_token_as_jwt": true,
                "software_statement": ssa_jwt,
            })))
            .with_body(
                json!({
                    "client_id": "some_client_id",
                    "client_secret": "some_client_secret",
                })
                .to_string(),
            )
            .expect(1)
            .create()
    }

    /// Mocks the `/token` endpoint
    fn mock_token_endpoint(server: &mut ServerGuard) -> Mock {
        let dcr_path = "/jans-auth/restv1/token";

        server
            .mock("POST", dcr_path)
            .with_body(
                json!({
                    "access_token": "some.access.token",
                })
                .to_string(),
            )
            .expect_at_least(1)
            .create()
    }

    /// Mocks the `/audit/log` endpoint
    fn mock_log_endpoint(server: &mut ServerGuard) -> Mock {
        let log_path = "/jans-auth/v1/audit/log/bulk";

        server
            .mock("POST", log_path)
            .match_body(mockito::Matcher::PartialJson(json!([{
                "creation_date": "2026-03-23T11:50:37.504Z",
                "event_time": "2026-03-23T11:50:37.504Z",
                "service": "test_app",
                "action": "Test",
                "decision_result": "ALLOW",
                "requested_resource": "Jans::Issue",
                "principal_id": "Jans::User",
            }])))
            .expect(1)
            .create()
    }

    /// Mocks the dynamic client registration endpoint without SSA
    fn mock_dcr_endpoint_without_ssa(server: &mut ServerGuard, pdp_id: PdpID) -> Mock {
        let dcr_path = "/jans-auth/restv1/register";

        server
            .mock("POST", dcr_path)
            .match_body(mockito::Matcher::PartialJson(json!({
                "token_endpoint_auth_method": "client_secret_basic",
                "grant_types": ["client_credentials"],
                "client_name": format!("cedarling-{}", pdp_id),
                "scope": DCR_SCOPE,
                "access_token_as_jwt": true,
            })))
            .with_body(
                json!({
                    "client_id": "some_client_id",
                    "client_secret": "some_client_secret",
                })
                .to_string(),
            )
            .expect(1)
            .create()
    }

    fn mock_health_endpoint(server: &mut ServerGuard) -> Mock {
        server
            .mock("POST", "/jans-auth/v1/audit/health/bulk")
            .match_body(mockito::Matcher::Any)
            .with_status(200)
            .expect_at_least(2)
            .create()
    }

    #[tokio::test]
    async fn test_lock_service_health_checks() {
        let pdp_id = PdpID::new();

        let mut mock_idp_server = Server::new_async().await;
        let mut mock_lock_server = Server::new_async().await;

        let (lock_config_uri, lock_config_endpoint) =
            mock_lock_config_endpoint(&mut mock_lock_server, &mock_idp_server);
        let oidc_endpoint = mock_oidc_endpoint(&mut mock_idp_server);
        let dcr_endpoint = mock_dcr_endpoint_without_ssa(&mut mock_idp_server, pdp_id);
        let token_endpoint = mock_token_endpoint(&mut mock_idp_server);
        let health_endpoint = mock_health_endpoint(&mut mock_lock_server);

        let config = LockServiceConfig {
            config_uri: lock_config_uri,
            dynamic_config: false,
            ssa_jwt: None,
            log_interval: None,
            health_interval: Some(Duration::from_millis(100)),
            telemetry_interval: None,
            listen_sse: false,
            log_level: LogLevel::TRACE,
            accept_invalid_certs: false,
            transport: LockTransport::Rest,
            ..Default::default()
        };

        let metrics = Arc::new(MetricsCollector::new(0));
        let _lock = LockService::new(
            pdp_id,
            &config,
            None,
            metrics,
            None,
            HttpClientConfig::default(),
        )
        .await
        .expect("build lock logger");
        lock_config_endpoint.assert();
        oidc_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();

        // Wait for health checks to be sent
        sleep(Duration::from_secs(1)).await;

        health_endpoint.assert();
    }
}
