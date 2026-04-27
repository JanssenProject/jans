// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::LazyLock;

use crate::common::issuer_utils::IssClaim;

use super::key_service::JwkSet;
use super::status_list::StatusListJwtStr;
use async_trait::async_trait;
use reqwest::{
    Client,
    header::{CACHE_CONTROL, ToStrError},
};
use serde::{Deserialize, Deserializer, de};
use url::Url;

static HTTP_CLIENT: LazyLock<Client> = LazyLock::new(Client::new);

// async_traits are Send by default but wasm-bindgen doesn't support those
// so we opt out of it for the wasm bindings to compile.
//
// see this relevant discussion: https://github.com/rustwasm/wasm-bindgen/issues/2409
#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
pub(super) trait GetFromUrl<T> {
    /// Send a get request to receive the resource from a URL
    async fn get_from_url(url: &Url) -> Result<T, HttpError>;
}

#[derive(Deserialize, Clone)]
pub(super) struct OpenIdConfig {
    pub issuer: IssClaim,
    #[serde(deserialize_with = "deserialize_url")]
    pub jwks_uri: Url,
    #[serde(deserialize_with = "deserialize_opt_url", default)]
    pub status_list_endpoint: Option<Url>,
}

fn deserialize_url<'de, D>(deserializer: D) -> Result<Url, D::Error>
where
    D: Deserializer<'de>,
{
    let url_str = String::deserialize(deserializer)?;
    let url = Url::parse(&url_str)
        .map_err(|e| de::Error::custom(format!("invalid url '{url_str}': {e}")))?;
    Ok(url)
}

fn deserialize_opt_url<'de, D>(deserializer: D) -> Result<Option<Url>, D::Error>
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

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl GetFromUrl<OpenIdConfig> for OpenIdConfig {
    async fn get_from_url(url: &Url) -> Result<Self, HttpError> {
        // add delay to simulate network latency and test async behavior of trusted issuers loading
        // it would be great to implement delay in mock server, but mockito doesn't support it.
        #[cfg(test)]
        {
            use std::time::Duration;
            use tokio::time::sleep;

            sleep(Duration::from_millis(1)).await;
        }

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

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
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

impl JwkSet {
    pub(super) async fn get_from_url_with_max_age(
        url: &Url,
    ) -> Result<(Self, Option<u64>), HttpError> {
        let response = HTTP_CLIENT
            .get(url.as_str())
            .send()
            .await
            .map_err(HttpError::GetRequest)?
            .error_for_status()
            .map_err(HttpError::ErrorCode)?;

        let max_age = response
            .headers()
            .get(CACHE_CONTROL)
            .and_then(|v| v.to_str().ok())
            .and_then(parse_max_age);

        let jwk_set = response
            .json::<JwkSet>()
            .await
            .map_err(HttpError::JsonDeserializeResponse)?;

        Ok((jwk_set, max_age))
    }
}

/// Parses `max-age=<seconds>` from a `Cache-Control` header value.
fn parse_max_age(cache_control: &str) -> Option<u64> {
    cache_control
        .split(',')
        .map(str::trim)
        .filter_map(|directive| directive.split_once('='))
        .find(|(name, _)| name.trim().eq_ignore_ascii_case("max-age"))
        .and_then(|(_, value)| value.trim().parse::<u64>().ok())
}

// NOTE: we cant use the async_trait here since this is called from another async
// function which requires this to be Send.
impl StatusListJwtStr {
    pub(super) async fn get_from_url(url: &Url) -> Result<Self, HttpError> {
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

#[cfg(test)]
mod tests {
    use super::parse_max_age;

    #[test]
    fn parses_max_age_from_cache_control_header() {
        let max_age = parse_max_age("public, max-age=172800, immutable");

        assert_eq!(
            max_age,
            Some(172_800),
            "expected to parse max-age directive from Cache-Control header"
        );
    }

    #[test]
    fn ignores_invalid_max_age_value() {
        let max_age = parse_max_age("public, max-age=abc");

        assert_eq!(
            max_age, None,
            "expected invalid max-age value to be ignored"
        );
    }

    #[test]
    fn returns_none_when_max_age_missing() {
        let max_age = parse_max_age("no-cache, no-store");

        assert_eq!(
            max_age, None,
            "expected no max-age when directive is missing"
        );
    }
}
