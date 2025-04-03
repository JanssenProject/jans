// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module is responsible for getting lock config from the
//! `.well-known/lock-server-configuration` endpoint.

use std::str::FromStr;

use derive_more::derive::Deref;
use http_utils::{Backoff, HttpRequestError, Sender};
use reqwest::Client;
use serde::{Deserialize, Deserializer, de};

#[derive(Debug, Deserialize, PartialEq, Clone)]
#[allow(dead_code)]
pub struct LockConfig {
    pub version: String,
    #[serde(rename = "issuer", deserialize_with = "host_to_oidc")]
    pub issuer_oidc_url: Url,
    #[serde(rename = "audit", default)]
    pub audit_endpoints: AuditEndpoints,
    #[serde(rename = "config", default)]
    pub config_endpoints: ConfigEndpoints,
}

#[derive(Debug, Deserialize, PartialEq, Clone, Default)]
#[allow(dead_code)]
pub struct AuditEndpoints {
    #[serde(
        rename = "log_endpoint",
        deserialize_with = "deserialize_to_bulk_endpoint",
        default
    )]
    pub log: Option<Url>,
    #[serde(
        rename = "health_endpoint",
        deserialize_with = "deserialize_to_bulk_endpoint",
        default
    )]
    pub health: Option<Url>,
    #[serde(
        rename = "telemetery_endpoint",
        deserialize_with = "deserialize_to_bulk_endpoint",
        default
    )]
    pub telemetry: Option<Url>,
}

#[derive(Debug, Deserialize, PartialEq, Clone, Default)]
#[allow(dead_code)]
pub struct ConfigEndpoints {
    #[serde(rename = "config_endpoint", default)]
    pub config: Option<Url>,
    #[serde(rename = "issuers_endpoint", default)]
    pub issuers: Option<Url>,
    #[serde(rename = "policy_endpoint", default)]
    pub policy: Option<Url>,
    #[serde(rename = "schema_endpoint", default)]
    pub schema: Option<Url>,
    #[serde(rename = "sse_endpoint", default)]
    pub sse: Option<Url>,
}

impl LockConfig {
    pub async fn get(lock_config_url: &url::Url) -> Result<Self, HttpRequestError> {
        let client = Client::new();
        let mut sender = Sender::new(Backoff::default_exponential());

        let config: LockConfig = sender.send(|| client.get(lock_config_url.as_ref())).await?;
        println!("config: {:?}", config);

        Ok(config)
    }
}

/// A wrapper for [`url::Url`] that implements [`serde::de::Deserialize`].
#[derive(Debug, Deref, PartialEq, Clone)]
pub struct Url(url::Url);

impl FromStr for Url {
    type Err = url::ParseError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let inner: url::Url = s.parse()?;
        Ok(Self(inner))
    }
}

impl<'de> Deserialize<'de> for Url {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let src: String = String::deserialize(deserializer)?;
        println!("deserializing url src: {src}");
        let url = src
            .parse()
            .map_err(|e| de::Error::custom(format!("error while parsing `{src}` as a URL: {e}")))?;

        Ok(url)
    }
}

/// Deserialize an audit endpoint like `/api/v1/audit/log` to `/apit/v1/audit/log/bulk`.
fn deserialize_to_bulk_endpoint<'de, D>(deserializer: D) -> Result<Option<Url>, D::Error>
where
    D: Deserializer<'de>,
{
    let src: String = String::deserialize(deserializer)?;

    if src.is_empty() {
        return Ok(None);
    }

    // We need to append the `/bulk` manually since the response from the
    // `.well-known/lock-server-configuration` endpoint will not have the `/bulk`.
    let src = src + "/bulk";

    let url: url::Url = src.parse().map_err(|e| {
        de::Error::custom(format!(
            "the bulk endpoint url, '{}', is not valid: {}",
            src, e
        ))
    })?;

    Ok(Some(Url(url)))
}

/// Deserialize the host, `demoexample.jans.io`, to the openid configuration endpoint:
/// `https://demoexample.jans.io/.well-known/openid-configuration`.
fn host_to_oidc<'de, D>(deserializer: D) -> Result<Url, D::Error>
where
    D: Deserializer<'de>,
{
    let host: String = String::deserialize(deserializer)?;

    // NOTE: for tests, we can't use http since mockito doesn't support creating https
    // endpoints. However in prod, this should always be https.
    #[cfg(not(test))]
    let derived_url = format!("https://{}/.well-known/openid-configuration", host);
    #[cfg(test)]
    let derived_url = format!("http://{}/.well-known/openid-configuration", host);

    let url: url::Url = derived_url.parse().map_err(|e| {
        de::Error::custom(format!(
            "the derived url, '{}', from the host, '{}' is not valid: {}",
            derived_url, host, e
        ))
    })?;

    Ok(Url(url))
}

#[cfg(test)]
mod test {
    use super::LockConfig;
    use crate::log::lock_logger::lock_config::{AuditEndpoints, ConfigEndpoints};
    use serde_json::json;
    use test_utils::assert_eq;

    #[tokio::test]
    async fn should_deserialize_lock_config() {
        let src = json!({
            "version": "1.0",
            "issuer": "test.com",
            "audit": {
                "log_endpoint": "https://test.com/audit/log",
                "health_endpoint": "",
            },
        });

        let deserialized =
            serde_json::from_value::<LockConfig>(src).expect("deserialize audit endpoints");

        assert_eq!(deserialized, LockConfig {
            version: "1.0".into(),
            // NOTE: resolving this url in tests will always be `http` instead of `https`
            // to support mocking using mockito
            issuer_oidc_url: "http://test.com/.well-known/openid-configuration"
                .parse()
                .unwrap(),
            audit_endpoints: AuditEndpoints {
                // should resolve to the `/bulk` endpoint automatically
                log: Some("https://test.com/audit/log/bulk".parse().unwrap()),
                health: None,
                telemetry: None
            },
            config_endpoints: ConfigEndpoints::default(),
        })
    }
}
