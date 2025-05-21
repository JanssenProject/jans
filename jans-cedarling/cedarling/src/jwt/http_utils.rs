// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::LazyLock;

use async_trait::async_trait;
use jsonwebtoken::jwk::JwkSet;
use reqwest::Client;
use serde::{Deserialize, Deserializer, de};
use url::Url;

static HTTP_CLIENT: LazyLock<Client> = LazyLock::new(Client::new);

#[async_trait]
pub trait GetFromUrl<T> {
    /// Send a get request to receive the resource from a URL
    async fn get_from_url(url: &Url) -> Result<T, HttpError>;
}

#[derive(Deserialize)]
pub struct OpenIdConfig {
    pub issuer: String,
    #[serde(deserialize_with = "deserialize_url")]
    pub jwks_uri: Url,
}

pub fn deserialize_url<'de, D>(deserializer: D) -> Result<Url, D::Error>
where
    D: Deserializer<'de>,
{
    let url_str = String::deserialize(deserializer)?;
    let url = Url::parse(&url_str)
        .map_err(|e| de::Error::custom(format!("invalid url '{url_str}': {e}")))?;
    Ok(url)
}

#[async_trait]
impl GetFromUrl<OpenIdConfig> for OpenIdConfig {
    async fn get_from_url(url: &Url) -> Result<Self, HttpError> {
        let openid_config = HTTP_CLIENT
            .get(url.as_str())
            .send()
            .await
            .map_err(HttpError::GetRequest)?
            .error_for_status()
            .map_err(HttpError::ErrorCode)?
            .json::<OpenIdConfig>()
            .await
            .map_err(HttpError::JsonDeserializeResponse)?;

        Ok(openid_config)
    }
}

#[async_trait]
impl GetFromUrl<JwkSet> for JwkSet {
    async fn get_from_url(url: &Url) -> Result<Self, HttpError> {
        let jwk_set = HTTP_CLIENT
            .get(url.as_str())
            .send()
            .await
            .map_err(HttpError::GetRequest)?
            .error_for_status()
            .map_err(HttpError::ErrorCode)?
            .json::<JwkSet>()
            .await
            .map_err(HttpError::JsonDeserializeResponse)?;

        Ok(jwk_set)
    }
}

#[derive(Debug, thiserror::Error)]
pub enum HttpError {
    #[error("failed to complete GET request: {0}")]
    GetRequest(#[source] reqwest::Error),
    #[error("received an error response: {0}")]
    ErrorCode(#[source] reqwest::Error),
    #[error("failed to deserialize respose from JSON: {0}")]
    JsonDeserializeResponse(#[source] reqwest::Error),
}
