// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Lock Server Integration Module
//!
//! This module provides integration with the Lock Server for centralized logging and monitoring.
//! It includes support for Software Statement Assertion (SSA) JWT validation for secure client registration.
//!
//! ## Overview
//!
//! The Lock Server integration allows Cedarling to:
//! - Send authorization decision logs to a centralized Lock Server
//! - Register as a client using Dynamic Client Registration (DCR)
//! - Validate SSA JWTs for enhanced security
//! - Handle authentication and authorization with the Lock Server
//!
//! ## Architecture
//!
//! ### Components
//!
//! - **LockService**: Main service that manages communication with the Lock Server
//! - **LogWorker**: Background worker that sends logs to the Lock Server
//! - **SSA Validation**: Validates Software Statement Assertion JWTs
//! - **Client Registration**: Handles Dynamic Client Registration with the IDP
//!
//! ### Flow
//!
//! 1. **Initialization**: LockService is created with configuration
//! 2. **SSA Validation**: If SSA JWT is provided, it's validated against the IDP's JWKS
//! 3. **Client Registration**: DCR request is sent to the IDP (with SSA JWT if available)
//! 4. **Access Token**: Client credentials are obtained for Lock Server communication
//! 5. **Logging**: Authorization decisions are sent to the Lock Server's audit endpoint
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
//! - `grant_types`: Array of OAuth2 grant types the software can use
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
//! - **InvalidSsaJwt**: SSA JWT validation failed
//! - **GetLockConfig**: Failed to retrieve Lock Server configuration
//! - **ClientRegistration**: Failed to register client with IDP
//! - **InitHttpClient**: Failed to initialize HTTP client
//!
//! ## Integration with IDP
//!
//! The Lock Server integration works with Identity Providers that support:
//!
//! - Dynamic Client Registration (RFC 7591)
//! - OAuth2 Client Credentials flow
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

mod lock_config;
mod log_entry;
mod log_worker;
mod register_client;
pub mod ssa_validation;

use crate::app_types::PdpID;
use crate::common::issuer_utils::normalize_issuer;
use crate::log::LoggerWeak;
use crate::log::interface::Loggable;
use crate::{LockServiceConfig, LogWriter};
use futures::channel::mpsc;
use lock_config::*;
use log_entry::LockLogEntry;
use log_worker::*;
use register_client::{ClientRegistrationError, register_client};
use reqwest::Client;
use reqwest::header::{HeaderMap, HeaderValue};
use ssa_validation::validate_ssa_jwt;
use std::sync::{Arc, RwLock};
use std::time::Duration;
use tokio_util::sync::CancellationToken;

/// The base duration to wait for if an http request fails for workers.
pub const WORKER_HTTP_RETRY_DUR: Duration = Duration::from_secs(10);

#[derive(Debug)]
struct WorkerSenderAndHandle {
    tx: RwLock<mpsc::Sender<SerializedLogEntry>>,
    handle: crate::http::JoinHandle<()>,
}

/// Stores logs in a buffer then sends them to the lock server in the background
#[derive(Debug)]
pub(crate) struct LockService {
    log_worker: Option<WorkerSenderAndHandle>,
    logger: Option<LoggerWeak>,
    cancel_tkn: CancellationToken,
}

pub fn init_http_client(
    default_headers: Option<HeaderMap>,
    accept_invalid_certs: bool,
) -> Result<Client, reqwest::Error> {
    let headers = default_headers.unwrap_or_default();

    if accept_invalid_certs {
        // NOTE: WASM builds fail when calling .danger_accept_invalid_certs
        #[cfg(any(target_arch = "wasm32", target_arch = "wasm64"))]
        {
            Client::builder().default_headers(headers).build()
        }

        #[cfg(not(any(target_arch = "wasm32", target_arch = "wasm64")))]
        {
            Client::builder()
                .default_headers(headers)
                .danger_accept_invalid_certs(true)
                .build()
        }
    } else {
        Client::builder().default_headers(headers).build()
    }
}

impl LockService {
    pub async fn new(
        pdp_id: PdpID,
        bootstrap_conf: &LockServiceConfig,
        logger: Option<LoggerWeak>,
    ) -> Result<Self, InitLockServiceError> {
        // Get lock config from the config uri endpoint first
        let lock_config = LockConfig::get(
            &bootstrap_conf.config_uri,
            bootstrap_conf.accept_invalid_certs,
        )
        .await?;

        // Validate SSA JWT if provided
        if let Some(ssa_jwt) = &bootstrap_conf.ssa_jwt {
            // Get the JWKS URI from the IDP URL (not the lock server URL)
            let jwks_uri = format!(
                "{}/jans-auth/restv1/jwks",
                normalize_issuer(&lock_config.issuer_oidc_url.0.origin().ascii_serialization())
            );

            // Validate the SSA JWT
            validate_ssa_jwt(ssa_jwt, &jwks_uri, bootstrap_conf.accept_invalid_certs)
                .await
                .map_err(|e| {
                    InitLockServiceError::InvalidSsaJwt(format!("SSA JWT validation failed: {}", e))
                })?;
        }

        // Register client
        let client_creds = register_client(
            pdp_id,
            &lock_config.issuer_oidc_url,
            bootstrap_conf.ssa_jwt.as_ref(),
            bootstrap_conf.accept_invalid_certs,
        )
        .await?;

        // Build a default http client that already has the access_token in the headers
        let mut headers = HeaderMap::new();
        let mut auth_header =
            HeaderValue::from_str(&format!("Bearer {}", client_creds.access_token))
                .expect("build header");
        auth_header.set_sensitive(true);
        headers.insert("Authorization", auth_header);
        let http_client = Arc::new(init_http_client(
            Some(headers),
            bootstrap_conf.accept_invalid_certs,
        )?);

        let cancel_tkn = CancellationToken::new();
        let log_worker = match (bootstrap_conf.log_interval, lock_config.audit_endpoints.log) {
            // Case where Cedarling's config enables sending logs but the lock server
            // does not have a log endpoint
            (Some(_), None) => None,
            (Some(log_interval), Some(log_endpoint)) => {
                let (log_tx, log_rx) = mpsc::channel::<SerializedLogEntry>(100);

                // Spawn log worker
                let mut log_worker = LogWorker::new(
                    log_interval,
                    http_client.clone(),
                    log_endpoint.0.clone(),
                    logger.clone(),
                );

                let cancel_tkn = cancel_tkn.clone();

                let handle =
                    crate::http::spawn_task(
                        async move { log_worker.run(log_rx, cancel_tkn).await },
                    );

                Some(WorkerSenderAndHandle {
                    tx: log_tx.into(),
                    handle,
                })
            },
            // Other cases that will always resolve to Cedarling not having to send logs
            _ => None,
        };

        Ok(Self {
            log_worker,
            logger,
            cancel_tkn,
        })
    }

    pub async fn shut_down(&mut self) {
        self.cancel_tkn.cancel();
        if let Some(log_worker) = self.log_worker.take() {
            _ = log_worker.handle.await_result().await;
        }
    }
}

impl LogWriter for LockService {
    fn log_any<T: Loggable>(&self, entry: T) {
        let Some(WorkerSenderAndHandle { tx: tx_lock, .. }) = self.log_worker.as_ref() else {
            return;
        };

        let entry = serde_json::to_string(&entry)
            .expect("entry should serialize successfully")
            .into_boxed_str();

        let mut log_tx = match tx_lock.write() {
            Ok(log_tx) => log_tx,
            Err(err) => {
                self.logger.log_any(LockLogEntry::error(format!(
                    "failed to acquire write lock for the LockLogSender. cedarling will not be able to send this entry to the lock server: {}",
                    err
                )));
                return;
            },
        };

        if let Err(err) = log_tx.try_send(entry) {
            self.logger.log_any(LockLogEntry::error(format!(
                "failed to send log entry to LogWorker, the thread may have unexpectedly closed or is full: {}",
                err
            )));
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum InitLockServiceError {
    #[error("the provided CEDARLING_LOCK_SSA_JWT is either malformed or expired: {0}")]
    InvalidSsaJwt(String),
    #[error(
        "failed to GET lock server config from the `.well-known/lock-server-configuration` endpoint: {0}"
    )]
    GetLockConfig(#[from] http_utils::HttpRequestError),
    #[error("failed to dynamically register client for the Lock server's auth: {0}")]
    ClientRegistration(#[from] ClientRegistrationError),
    #[error("failed to initialize the Lock logger's HttpClient: {0}")]
    InitHttpClient(#[from] reqwest::Error),
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::log::interface::Indexed;
    use crate::{LogLevel, lock::register_client::DCR_SCOPE};
    use jsonwebtoken::{EncodingKey, Header, encode};
    use mockito::{Mock, Server, ServerGuard};
    use serde::Serialize;
    use serde_json::json;
    use std::time::Duration;
    use tokio::time::sleep;
    use uuid7::Uuid;

    fn generate_test_jwt() -> String {
        let mut header = Header::new(jsonwebtoken::Algorithm::HS256);
        header.kid = Some("test-kid".to_string());
        let claims = json!({
            "software_id": "test_software",
            "grant_types": ["client_credentials"],
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": ["cedarling"],
            "exp": 9999999999u64,
            "iat": 1111111111u64,
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
        };

        // Test startup
        let logger = LockService::new(pdp_id, &config, None)
            .await
            .expect("build lock logger");
        lock_config_endpoint.assert();
        oidc_endpoint.assert();
        jwks_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();

        // Test if logs are getting sent
        let log_entry = MockLogEntry {
            level: LogLevel::TRACE,
            id: "some_uuid_string".into(),
            msg: "this is a test log entry".into(),
        };
        logger.log_any(log_entry);

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
        };

        // Test startup without SSA
        let logger = LockService::new(pdp_id, &config, None)
            .await
            .expect("build lock logger");
        lock_config_endpoint.assert();
        oidc_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();

        // Test if logs are getting sent
        let log_entry = MockLogEntry {
            level: LogLevel::TRACE,
            id: "some_uuid_string".into(),
            msg: "this is a test log entry".into(),
        };
        logger.log_any(log_entry);

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
        };

        // Test startup with invalid SSA should fail
        let result = LockService::new(pdp_id, &config, None).await;
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            InitLockServiceError::InvalidSsaJwt(_)
        ));
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
        // we should be sending to the `/bulk` endpoint instead even if it's not defined
        // in the `.well-known/lock-server-configuration` response
        let log_path = "/jans-auth/v1/audit/log/bulk";

        server
            .mock("POST", log_path)
            .match_body(mockito::Matcher::PartialJson(json!([{
                "level": "TRACE",
                "id": "some_uuid_string",
                "msg": "this is a test log entry",
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

    #[derive(Serialize, Clone)]
    struct MockLogEntry {
        level: LogLevel,
        id: String,
        msg: String,
    }

    impl Loggable for MockLogEntry {
        fn get_log_level(&self) -> Option<crate::LogLevel> {
            Some(self.level)
        }
    }

    impl Indexed for MockLogEntry {
        fn get_id(&self) -> Uuid {
            unimplemented!()
        }

        fn get_additional_ids(&self) -> Vec<Uuid> {
            Vec::new()
        }

        fn get_tags(&self) -> Vec<&str> {
            Vec::new()
        }
    }
}
