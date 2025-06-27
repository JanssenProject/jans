// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module is responsible for Cedarling's Dynamic Client Registration (DCR) at startup.

use std::collections::HashMap;

use super::{init_http_client, lock_config::Url};
use crate::app_types::PdpID;
use http_utils::{Backoff, Sender};
use serde::Deserialize;
use serde_json::json;
use thiserror::Error;

pub const DCR_SCOPE: &str = "cedarling";
pub const ACCESS_TKN_SCOPE: &str = "https://jans.io/oauth/lock/log.write https://jans.io/oauth/lock/health.write https://jans.io/oauth/lock/telemetry.write";

pub async fn register_client(
    pdp_id: PdpID,
    oidc_endpoint: &Url,
    ssa_jwt: Option<&String>,
    accept_invalid_certs: bool,
) -> Result<ClientCredentials, ClientRegistrationError> {
    let client = init_http_client(None, accept_invalid_certs)?;

    #[cfg(not(test))]
    let mut sender = Sender::new(Backoff::default_exponential());

    // We implement a faster retry for the tests
    #[cfg(test)]
    let mut sender = {
        use std::time::Duration;
        Sender::new(Backoff::new_exponential(Duration::from_millis(10), Some(3)))
    };

    // Get openid config
    let oidc: OpenidConfig = sender
        .send(|| client.get(oidc_endpoint.0.as_str()))
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
    } = sender
        .send(|| {
            client
                .post(&oidc.registration_endpoint)
                .header("Content-Type", "application/json")
                .body(dcr_body.to_string())
        })
        .await
        .map_err(ClientRegistrationError::RegisterLockClient)?;

    // Get access token
    let form_data = serde_json::from_value::<HashMap<String, String>>(json!({
        "grant_type": "client_credentials",
        "scope": ACCESS_TKN_SCOPE,
    }))
    // this should never fail since this is a hard-coded valid JSON
    .expect("serialize form data");
    
    let AccessToken { access_token } = sender
        .send(|| {
            client
                .post(&oidc.token_endpoint)
                .basic_auth(&client_id, Some(&client_secret))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(&form_data)
        })
        .await
        .map_err(ClientRegistrationError::GetAccessToken)?;

    Ok(ClientCredentials {
        client_id,
        client_secret,
        access_token,
    })
}

#[derive(Debug, PartialEq, Clone)]
#[allow(dead_code)]
pub struct ClientCredentials {
    pub client_id: String,
    pub client_secret: String,
    pub access_token: String,
}

#[derive(Debug, Error)]
pub enum ClientRegistrationError {
    #[error("failed to get openid config: {0}")]
    GetOpenidConfig(#[source] http_utils::HttpRequestError),
    #[error("failed to register lock client: {0}")]
    RegisterLockClient(#[source] http_utils::HttpRequestError),
    #[error("failed to get access token: {0}")]
    GetAccessToken(#[source] http_utils::HttpRequestError),
    #[error("failed to initialize HTTP client: {0}")]
    InitializeHttpClient(#[from] reqwest::Error),
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
}
