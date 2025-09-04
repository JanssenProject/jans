// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::LazyLock;

use super::key_service::JwkSet;
use super::status_list::StatusListJwtStr;
use async_trait::async_trait;
use reqwest::{Client, header::ToStrError};
use serde::{Deserialize, Deserializer, de};
use url::Url;

static HTTP_CLIENT: LazyLock<Client> = LazyLock::new(Client::new);

// async_traits are Send by default but wasm-bindgen doesn't support those
// so we opt out of it for the wasm bindings to compile.
//
// see this relevant discussion: https://github.com/rustwasm/wasm-bindgen/issues/2409
#[async_trait(?Send)]
pub trait GetFromUrl<T> {
    /// Send a get request to receive the resource from a URL
    async fn get_from_url(url: &Url) -> Result<T, HttpError>;
}

#[derive(Deserialize)]
pub struct OpenIdConfig {
    pub issuer: String,
    #[serde(deserialize_with = "deserialize_url")]
    pub jwks_uri: Url,
    #[serde(deserialize_with = "deserialize_opt_url", default)]
    pub status_list_endpoint: Option<Url>,
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

pub fn deserialize_opt_url<'de, D>(deserializer: D) -> Result<Option<Url>, D::Error>
where
    D: Deserializer<'de>,
{
    let url_str = Option::<String>::deserialize(deserializer)?;
    let url = url_str
        .as_ref()
        .map(|url_str| {
            Url::parse(url_str)
                .map_err(|e| de::Error::custom(format!("invalid url '{url_str}': {e}")))
        })
        .transpose()?;
    Ok(url)
}

#[async_trait(?Send)]
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

#[async_trait(?Send)]
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

// NOTE: we cant use the async_trait here since this is called from another aysnc 
// function which requires this to be Send.
impl StatusListJwtStr {
    pub async fn get_from_url(url: &Url) -> Result<Self, HttpError> {
        let response = HTTP_CLIENT
            .get(url.as_str())
            .header("Content-Type", "application/statuslist+jwt")
            .send()
            .await
            .map_err(HttpError::GetRequest)?
            .error_for_status()
            .map_err(HttpError::ErrorCode)?;

        if let Some(content_type) = response.headers().get("Content-Type") {
            let content_type = content_type
                .to_str()
                .map_err(|e| HttpError::InvalidHeader("Content-Type".to_string(), e))?;
            if content_type != "application/statuslist+jwt" {
                return Err(HttpError::Unsupported(format!(
                    "got unsupported status list type, '{0}', from '{1}'. Cedarling currently only supports 'application/statuslist+jwt'",
                    content_type,
                    url.as_str(),
                )));
            }
        }

        let status_list_jwt = response.text().await.map_err(HttpError::ReadTextResponse)?;

        Ok(StatusListJwtStr::new(status_list_jwt))
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
    #[error("failed to read the respose text: {0}")]
    ReadTextResponse(#[source] reqwest::Error),
    #[error("the value of the '{0}' header is invalid: {1}")]
    InvalidHeader(String, ToStrError),
    #[error("{0}")]
    Unsupported(String),
}
