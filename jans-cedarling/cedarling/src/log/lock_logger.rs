// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod lock_config;
mod log_worker;
mod register_client;

use super::interface::Loggable;
use crate::app_types::PdpID;
use crate::{LockLogConfig, LogWriter};
use lock_config::*;
use log_worker::*;
use register_client::*;
use reqwest::Client;
use reqwest::header::{HeaderMap, HeaderValue};
use std::sync::Arc;
use std::time::Duration;
use thiserror::Error;
use tokio::sync::mpsc;

/// The base duration to wait for if an http request fails for workers.
pub const WORKER_HTTP_RETRY_DUR: Duration = Duration::from_secs(10);

/// Stores logs in a buffer then sends them to the lock server in the background
pub(crate) struct LockLogger {
    log_worker_tx: Option<mpsc::Sender<SerializedLogEntry>>,
}

impl LockLogger {
    pub async fn new(
        pdp_id: PdpID,
        bootstrap_conf: &LockLogConfig,
    ) -> Result<Self, InitLockLoggerError> {
        // TODO: validate SSA JWT
        // Validating the SSA JWT involves getting the keys from the IDP however, slotting in the
        // JWT validator module here is a bit tricky. It might be better implement it in a
        // separate PR then move the JWT validator to it's own crate.

        // Get lock config from the config uri endpoint
        let lock_config = LockConfig::get(&bootstrap_conf.config_uri).await?;

        // Register client
        let client_creds = register_client(
            pdp_id,
            &lock_config.issuer_oidc_url,
            &bootstrap_conf.ssa_jwt,
        )
        .await?;

        // Build a default http client that already has the access_token in the headers
        let mut headers = HeaderMap::new();
        let mut auth_header =
            HeaderValue::from_str(&format!("Bearer {}", client_creds.access_token))
                .expect("build header");
        auth_header.set_sensitive(true);
        headers.insert("Authorization", auth_header);
        let http_client = Arc::new(Client::builder().default_headers(headers).build()?);

        let log_worker_tx = match (bootstrap_conf.log_interval, lock_config.audit_endpoints.log) {
            // Case where Cedarling's config enables sending logs but the lock server
            // does not have a log endpoint
            (Some(_), None) => None,
            (Some(log_interval), Some(log_endpoint)) => {
                let (log_tx, log_rx) = mpsc::channel::<SerializedLogEntry>(100);

                // Spawn log worker
                let mut log_worker =
                    LogWorker::new(log_interval, http_client.clone(), (*log_endpoint).clone());
                tokio::spawn(async move { log_worker.run(log_rx).await });

                Some(log_tx)
            },
            // Other cases that will always resolve to Cedarling not having to send logs
            _ => None,
        };

        Ok(Self { log_worker_tx })
    }
}

impl LogWriter for LockLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        // TODO: we could probably just generalize the fallback logger then log there.
        let entry = serde_json::to_string(&entry)
            .expect("entry should serialize successfully")
            .into_boxed_str();
        if let Some(log_tx) = self.log_worker_tx.as_ref() {
            if let Err(err) = log_tx.try_send(entry) {
                eprintln!(
                    "failed to send log entry to LogWorker, the thread may have unexpectedly closed or is full: {err}"
                );
            }
        }
    }
}

#[derive(Debug, Error)]
pub enum InitLockLoggerError {
    #[error("the provided CEDARLING_LOCK_SSA_JWT is either malformed or expired")]
    InvalidSsaJwt,
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
    use crate::LogLevel;
    use crate::log::interface::Indexed;
    use mockito::{Mock, Server, ServerGuard};
    use serde::Serialize;
    use serde_json::json;
    use std::time::Duration;
    use tokio::time::sleep;
    use uuid7::Uuid;

    #[tokio::test]
    async fn test_lock_logger() {
        let pdp_id = PdpID::new();
        let ssa_jwt: Box<str> = "eyJraWQiOiJzc2FfOTgwYTQ0ZDQtZWE3OS00YTM1LThlNjMtNzlhNzg4NTNmYzUwX3NpZ19yczI1NiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzb2Z0d2FyZV9pZCI6IkNlZGFybGluZ1Rlc3QiLCJncmFudF90eXBlcyI6WyJhdXRob3JpemF0aW9uX2NvZGUiLCJyZWZyZXNoX3Rva2VuIl0sIm9yZ19pZCI6InRlc3QiLCJpc3MiOiJodHRwczovL2RlbW9leGFtcGxlLmphbnMuaW8iLCJzb2Z0d2FyZV9yb2xlcyI6WyJjZWRhcmxpbmciXSwiZXhwIjozMzE5NzE3ODEyLCJpYXQiOjE3NDI5MTc4MTMsImp0aSI6IjM5NTA0NTRlLTM5MWMtNDlhOS05YzYxLTY4MGMyNWE4MDk0ZCJ9.INA5qvpheWvJe6DJaeLkOYt1YH3W9gJQ3yy5Cr5G9_QbzazV23FMJDH2Rbysauk4YNC0oIsTL4MBQ_dRn3YaPLapOhizIlxZQF_uHBpYnopsk6KxgiRQTotg1Kw7Kwsi1RHtfHXpplSS15Dc-9QrOIGbNu44zEt1F5FYV5feW2c0u5HIRISoMNPutOYfMH18bZaBM28N8BssuqLv5X_Bc8EuSkmNTERP5L4khv6Mi3uVItkgK9xTbMKCpUstH_LchT1BKD_pTTMAQx6g6TOf3gnwKYQcmQhjJWFUbXnKCjghExV4PrYc6P8YaXdFnPBYoovd8FxS5qrX8trkh6pxeQ".into();

        let mut mock_idp_server = Server::new_async().await;
        let mut mock_lock_server = Server::new_async().await;

        let (lock_config_uri, lock_config_endpoint) =
            mock_lock_config_endpoint(&mut mock_lock_server, &mock_idp_server);
        let oidc_endpoint = mock_oidc_endpoint(&mut mock_idp_server);
        let dcr_endpoint = mock_dcr_endpoint(&mut mock_idp_server, pdp_id, &ssa_jwt);
        let token_endpoint = mock_token_endpoint(&mut mock_idp_server);
        let log_endpoint = mock_log_endpoint(&mut mock_lock_server);

        let config = LockLogConfig {
            config_uri: lock_config_uri,
            dynamic_config: false,
            ssa_jwt,
            log_interval: Some(Duration::from_millis(100)),
            health_interval: None,
            telemetry_interval: None,
            listen_sse: false,
        };

        // Test startup
        let logger = LockLogger::new(pdp_id, &config)
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

    /// Mocks the dynamic client registration endpoint
    fn mock_dcr_endpoint(server: &mut ServerGuard, pdp_id: PdpID, ssa_jwt: &str) -> Mock {
        let dcr_path = "/jans-auth/restv1/register";

        server
            .mock("POST", dcr_path)
            .match_body(mockito::Matcher::PartialJson(json!({
                "token_endpoint_auth_method": "client_secret_basic",
                "grant_types": ["client_credentials"],
                "client_name": format!("cedarling-{}", pdp_id),
                "scope": CEDARLING_SCOPES,
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

    #[derive(Serialize)]
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
