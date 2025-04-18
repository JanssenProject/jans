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

pub const CEDARLING_SCOPES: &str = "https://jans.io/oauth/lock/log.write";

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
        .send(|| client.get(oidc_endpoint.as_ref()))
        .await
        .map_err(ClientRegistrationError::GetOpenidConfig)?;

    // Register client
    let mut dcr_body = json!({
        "token_endpoint_auth_method": "client_secret_basic",
        "grant_types": ["client_credentials"],
        "client_name": format!("cedarling-{}", pdp_id),
        "scope": CEDARLING_SCOPES,
        "access_token_as_jwt": true,
    });
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
                .body(dcr_body.to_string())
        })
        .await
        .map_err(ClientRegistrationError::RegisterLockClient)?;

    // Get access token
    let form_data = serde_json::from_value::<HashMap<String, String>>(json!({
        "grant_type": "client_credentials",
        "scope": CEDARLING_SCOPES,
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
