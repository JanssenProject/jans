// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::{
    common::issuer_utils::IssClaim,
    http::{HttpClient, HttpClientError},
};

use super::key_service::JwkSet;
use super::status_list::StatusListJwtStr;
use async_trait::async_trait;
use reqwest::header::{CACHE_CONTROL, ToStrError};
use serde::{Deserialize, Deserializer, de};
use url::Url;

// async_traits are Send by default but wasm-bindgen doesn't support those
// so we opt out of it for the wasm bindings to compile.
//
// see this relevant discussion: https://github.com/rustwasm/wasm-bindgen/issues/2409
#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
pub(super) trait GetFromUrl<T: for<'de> serde::Deserialize<'de>> {
    /// Send a get request to receive the resource from a URL
    async fn get_from_url(url: &Url, client: &HttpClient) -> Result<T, HttpError> {
        require_secure_url(url)?;

        // add delay to simulate network latency and test async behavior
        // it would be great to implement delay in mock server, but mockito doesn't support it.
        #[cfg(test)]
        {
            use std::time::Duration;
            use tokio::time::sleep;

            sleep(Duration::from_millis(1)).await;
        }

        let result = client
            .get_json::<T>(url.as_str())
            .await
            .map_err(HttpError::Request)?;

        Ok(result)
    }
}

/// Returns `Ok(())` if `url` is safe to fetch from a JWT-validation context.
///
/// "Safe" means either `https`, or `http` pointed at a loopback address
/// (`localhost`, `127.0.0.1`, `::1`)
pub(super) fn require_secure_url(url: &Url) -> Result<(), HttpError> {
    if url.scheme().eq_ignore_ascii_case("https") {
        return Ok(());
    }

    if url.scheme().eq_ignore_ascii_case("http") && is_loopback_host(url) {
        return Ok(());
    }

    Err(HttpError::InsecureScheme {
        url: url.to_string(),
        scheme: url.scheme().to_string(),
    })
}

fn is_loopback_host(url: &Url) -> bool {
    match url.host() {
        Some(url::Host::Ipv4(addr)) => addr.is_loopback(),
        Some(url::Host::Ipv6(addr)) => addr.is_loopback(),
        Some(url::Host::Domain(domain)) => domain.eq_ignore_ascii_case("localhost"),
        None => false,
    }
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

impl GetFromUrl<OpenIdConfig> for OpenIdConfig {}

impl GetFromUrl<JwkSet> for JwkSet {}

impl JwkSet {
    pub(super) async fn get_from_url_with_max_age(
        url: &Url,
        client: &HttpClient,
    ) -> Result<(Self, Option<u64>), HttpError> {
        require_secure_url(url)?;

        let response = client
            .get_with_retry(url.as_str())
            .await
            .map_err(HttpError::Request)?;

        let max_age = response
            .headers()
            .get(CACHE_CONTROL)
            .and_then(|v| v.to_str().ok())
            .and_then(parse_max_age);

        // Read the body with the configured size cap so a hostile JWKS endpoint
        // can't OOM the backend with an oversized response.
        let bytes = crate::http_utils::read_response_capped(response, client.max_response_size_bytes())
            .await
            .map_err(|e| HttpError::Request(HttpClientError::new(e, None)))?;
        let jwk_set: JwkSet =
            serde_json::from_slice(&bytes).map_err(HttpError::JsonDeserializeBytes)?;

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
    pub(super) async fn get_from_url(url: &Url, client: &HttpClient) -> Result<Self, HttpError> {
        require_secure_url(url)?;

        let response = client
            .get_with_retry_with(url.as_str(), |b| {
                b.header("Accept", "application/statuslist+jwt")
            })
            .await
            .map_err(HttpError::Request)?;

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

        let bytes = crate::http_utils::read_response_capped(response, client.max_response_size_bytes())
            .await
            .map_err(|e| HttpError::Request(HttpClientError::new(e, None)))?;
        let status_list_jwt = String::from_utf8(bytes).map_err(HttpError::InvalidUtf8)?;

        Ok(StatusListJwtStr::new(status_list_jwt))
    }
}

#[derive(Debug, thiserror::Error)]
pub(crate) enum HttpError {
    #[error("HTTP request failed: {0}")]
    Request(#[from] HttpClientError),
    #[error("failed to deserialize response body bytes as JSON: {0}")]
    JsonDeserializeBytes(#[source] serde_json::Error),
    #[error("response body is not valid UTF-8: {0}")]
    InvalidUtf8(#[source] std::string::FromUtf8Error),
    #[error("the value of the '{0}' header is invalid: {1}")]
    InvalidHeader(String, ToStrError),
    #[error("{0}")]
    Unsupported(String),
    #[error(
        "refusing to fetch '{url}' over insecure scheme '{scheme}'; \
         JWT validation requires https (loopback http is allowed)"
    )]
    InsecureScheme { url: String, scheme: String },
}

#[cfg(test)]
mod tests {
    use super::{HttpError, parse_max_age, require_secure_url};
    use url::Url;

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

    #[test]
    fn allows_https() {
        let url = Url::parse("https://idp.example.com/.well-known/openid-configuration").unwrap();
        assert!(require_secure_url(&url).is_ok());
    }

    #[test]
    fn allows_http_loopback_hosts() {
        for raw in [
            "http://localhost/.well-known/openid-configuration",
            "http://localhost:8080/jwks",
            "http://127.0.0.1:1234/jwks",
            "http://[::1]:1234/jwks",
        ] {
            let url = Url::parse(raw).unwrap();
            assert!(
                require_secure_url(&url).is_ok(),
                "expected loopback url to be allowed: {raw}"
            );
        }
    }

    #[test]
    fn rejects_plain_http_to_remote_host() {
        let url = Url::parse("http://idp.example.com/.well-known/openid-configuration").unwrap();
        let err = require_secure_url(&url).expect_err("expected http remote to be rejected");
        match err {
            HttpError::InsecureScheme { scheme, .. } => assert_eq!(scheme, "http"),
            other => panic!("expected InsecureScheme, got {other:?}"),
        }
    }

    #[test]
    fn rejects_non_http_schemes() {
        let url = Url::parse("ftp://idp.example.com/jwks").unwrap();
        let err = require_secure_url(&url).expect_err("expected ftp to be rejected");
        match err {
            HttpError::InsecureScheme { scheme, .. } => assert_eq!(scheme, "ftp"),
            other => panic!("expected InsecureScheme, got {other:?}"),
        }
    }
}
