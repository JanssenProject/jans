// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module is responsible for Cedarling's Dynamic Client Registration (DCR) at startup.

use super::{init_http_client, lock_config::Url};
use crate::http::HttpClient;
use crate::{HttpClientConfig, app_types::PdpID, http::InitializeHttpClientError};
use serde::Deserialize;
use serde_json::json;
use thiserror::Error;

pub(super) const DCR_SCOPE: &str = "cedarling";
pub(super) const ACCESS_TKN_SCOPE: &str = "https://jans.io/oauth/lock/log.write https://jans.io/oauth/lock/health.write https://jans.io/oauth/lock/telemetry.write https://jans.io/oauth/lock/policy.readonly";
/// Lock's TRACE ingestion endpoints require this scope, not `log.write`.
const TRACE_WRITE_SCOPE: &str = "https://jans.io/oauth/lock/trace.write";

/// Scopes of the Lock access token; `trace.write` only when TRACE is enabled, so
/// a client without TRACE asks for nothing new.
pub(super) fn access_token_scope(trace: bool) -> String {
    if trace {
        format!("{ACCESS_TKN_SCOPE} {TRACE_WRITE_SCOPE}")
    } else {
        ACCESS_TKN_SCOPE.to_string()
    }
}

pub(super) async fn register_client(
    pdp_id: PdpID,
    oidc_endpoint: &Url,
    ssa_jwt: Option<&String>,
    accept_invalid_certs: bool,
    http_conf: HttpClientConfig,
    scope: String,
) -> Result<ClientCredentials, ClientRegistrationError> {
    let client = init_http_client(None, accept_invalid_certs, http_conf)?;

    // Get openid config
    let oidc: OpenidConfig = client
        .get_json(oidc_endpoint.0.as_str())
        .await
        .map_err(ClientRegistrationError::GetOpenidConfig)?;

    // Register client with SSA JWT if provided
    let mut dcr_body = json!({
        "token_endpoint_auth_method": "client_secret_basic",
        "grant_types": ["client_credentials"],
        "client_name": format!("cedarling-{}", pdp_id),
        "scope": DCR_SCOPE,
        "access_token_as_jwt": true,
    });

    // Add SSA JWT to the DCR request if provided
    if let Some(ssa_jwt) = ssa_jwt {
        dcr_body["software_statement"] = json!(ssa_jwt);
    }

    let ClientIdAndSecret {
        client_id,
        client_secret,
    } = client
        .post_json(|client| {
            client
                .post(&oidc.registration_endpoint)
                .header("Content-Type", "application/json")
                .body(dcr_body.to_string())
        })
        .await
        .map_err(ClientRegistrationError::RegisterLockClient)?;

    let source = TokenSource {
        token_endpoint: oidc.token_endpoint,
        client_id,
        client_secret,
        scope,
    };
    let access_token = request_access_token(&client, &source).await?;

    Ok(ClientCredentials {
        access_token,
        source,
    })
}

/// Gets an access token with the client credentials grant. Used at registration
/// and again whenever a token must be reissued, so there is one way to get it.
pub(super) async fn request_access_token(
    http: &HttpClient,
    source: &TokenSource,
) -> Result<String, ClientRegistrationError> {
    let form_data = [
        ("grant_type", "client_credentials"),
        ("scope", source.scope.as_str()),
    ];
    let AccessToken { access_token } = http
        .post_json(|client| {
            client
                .post(&source.token_endpoint)
                .basic_auth(&source.client_id, Some(&source.client_secret))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(&form_data)
        })
        .await
        .map_err(ClientRegistrationError::GetAccessToken)?;
    Ok(access_token)
}

#[derive(Debug, PartialEq, Clone)]
pub(super) struct ClientCredentials {
    pub access_token: String,
    /// What it takes to get the next token for the same client.
    pub source: TokenSource,
}

/// The registered client and the token endpoint, kept so a token can be reissued.
#[derive(PartialEq, Clone)]
pub(super) struct TokenSource {
    token_endpoint: String,
    client_id: String,
    client_secret: String,
    scope: String,
}

impl std::fmt::Debug for TokenSource {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("TokenSource")
            .field("token_endpoint", &self.token_endpoint)
            .field("client_id", &self.client_id)
            .field("client_secret", &"[REDACTED]")
            .field("scope", &self.scope)
            .finish()
    }
}

#[derive(Debug, Error)]
pub enum ClientRegistrationError {
    #[error("failed to get openid config: {0}")]
    GetOpenidConfig(#[source] crate::http_utils::HttpRequestError),
    #[error("failed to register lock client: {0}")]
    RegisterLockClient(#[source] crate::http_utils::HttpRequestError),
    #[error("failed to get access token: {0}")]
    GetAccessToken(#[source] crate::http_utils::HttpRequestError),
    #[error("failed to initialize HTTP client: {0}")]
    InitializeHttpClient(#[from] InitializeHttpClientError),
}

#[derive(Debug, Deserialize)]
struct OpenidConfig {
    registration_endpoint: String,
    token_endpoint: String,
}

#[derive(Debug, Deserialize)]
struct ClientIdAndSecret {
    client_id: String,
    client_secret: String,
}

#[derive(Debug, Deserialize)]
struct AccessToken {
    #[serde(rename = "access_token")]
    access_token: String,
}

#[cfg(test)]
mod tests {
    use super::*;
    use mockito::{Mock, Server, ServerGuard};
    use serde_json::json;

    #[tokio::test]
    async fn test_register_client_with_ssa() {
        let pdp_id = PdpID::new();
        let ssa_jwt = "eyJraWQiOiJzc2FfOTgwYTQ0ZDQtZWE3OS00YTM1LThlNjMtNzlhNzg4NTNmYzUwX3NpZ19yczI1NiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzb2Z0d2FyZV9pZCI6IkNlZGFybGluZ1Rlc3QiLCJncmFudF90eXBlcyI6WyJhdXRob3JpemF0aW9uX2NvZGUiLCJyZWZyZXNoX3Rva2VuIl0sIm9yZ19pZCI6InRlc3QiLCJpc3MiOiJodHRwczovL2RlbW9leGFtcGxlLmphbnMuaW8iLCJzb2Z0d2FyZV9yb2xlcyI6WyJjZWRhcmxpbmciXSwiZXhwIjozMzE5NzE3ODEyLCJpYXQiOjE3NDI5MTc4MTMsImp0aSI6IjM5NTA0NTRlLTM5MWMtNDlhOS05YzYxLTY4MGMyNWE4MDk0ZCJ9.INA5qvpheWvJe6DJaeLkOYt1YH3W9gJQ3yy5Cr5G9_QbzazV23FMJDH2Rbysauk4YNC0oIsTL4MBQ_dRn3YaPLapOhizIlxZQF_uHBpYnopsk6KxgiRQTotg1Kw7Kwsi1RHtfHXpplSS15Dc-9QrOIGbNu44zEt1F5FYV5feW2c0u5HIRISoMNPutOYfMH18bZaBM28N8BssuqLv5X_Bc8EuSkmNTERP5L4khv6Mi3uVItkgK9xTbMKCpUstH_LchT1BKD_pTTMAQx6g6TOf3gnwKYQcmQhjJWFUbXnKCjghExV4PrYc6P8YaXdFnPBYoovd8FxS5qrX8trkh6pxeQ";

        let mut mock_server = Server::new_async().await;

        let oidc_endpoint = mock_oidc_endpoint(&mut mock_server);
        let dcr_endpoint = mock_dcr_endpoint(&mut mock_server, pdp_id, ssa_jwt);
        let token_endpoint = mock_token_endpoint(&mut mock_server);

        let oidc_url: url::Url = format!("{}/.well-known/openid-configuration", mock_server.url())
            .parse()
            .expect("valid URL");

        let result = register_client(
            pdp_id,
            &super::super::lock_config::Url(oidc_url),
            Some(&ssa_jwt.to_string()),
            false,
            crate::http::HttpClientConfig::default(),
            access_token_scope(false),
        )
        .await;

        assert!(result.is_ok());
        oidc_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();
    }

    #[tokio::test]
    async fn test_register_client_without_ssa() {
        let pdp_id = PdpID::new();

        let mut mock_server = Server::new_async().await;

        let oidc_endpoint = mock_oidc_endpoint(&mut mock_server);
        let dcr_endpoint = mock_dcr_endpoint_without_ssa(&mut mock_server, pdp_id);
        let token_endpoint = mock_token_endpoint(&mut mock_server);

        let oidc_url: url::Url = format!("{}/.well-known/openid-configuration", mock_server.url())
            .parse()
            .expect("valid URL");

        let result = register_client(
            pdp_id,
            &super::super::lock_config::Url(oidc_url),
            None,
            false,
            crate::http::HttpClientConfig::default(),
            access_token_scope(false),
        )
        .await;

        assert!(result.is_ok());
        oidc_endpoint.assert();
        dcr_endpoint.assert();
        token_endpoint.assert();
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

    /// Mocks the dynamic client registration endpoint with SSA
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

    /// Mocks the `/token` endpoint
    fn mock_token_endpoint(server: &mut ServerGuard) -> Mock {
        let token_path = "/jans-auth/restv1/token";

        server
            .mock("POST", token_path)
            .with_body(
                json!({
                    "access_token": "some.access.token",
                })
                .to_string(),
            )
            .expect(1)
            .create()
    }

    #[test]
    fn access_token_scope_adds_trace_write_only_with_trace() {
        let trace_write = "https://jans.io/oauth/lock/trace.write";
        assert_eq!(
            access_token_scope(false),
            ACCESS_TKN_SCOPE,
            "without TRACE the token request asks for the same scopes as before"
        );
        assert_eq!(
            access_token_scope(true),
            format!("{ACCESS_TKN_SCOPE} {trace_write}"),
            "with TRACE the token request also asks for trace.write"
        );
    }
}
