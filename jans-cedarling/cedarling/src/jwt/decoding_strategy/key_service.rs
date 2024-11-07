/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod error;
mod openid_config;

pub use error::KeyServiceError;
use jsonwebtoken::jwk::Jwk;
use jsonwebtoken::DecodingKey;
pub(crate) use openid_config::*;
use reqwest::blocking::Client;
use serde::Deserialize;
use std::collections::HashMap;
use std::sync::Arc;
use std::thread::sleep;
use std::time::Duration;

/// Retrieves a [`DecodingKey`]-s based on the provided jwks_uri.
fn fetch_decoding_keys(
    jwks_uri: &str,
    http_client: &HttpClient,
) -> Result<HashMap<Box<str>, DecodingKey>, KeyServiceError> {
    let jwks: Jwks = http_client
        .get(jwks_uri)?
        .json()
        .map_err(KeyServiceError::RequestDeserialization)?;

    let mut decoding_keys = HashMap::new();
    for jwk in jwks.keys {
        let jwk = serde_json::from_str::<Jwk>(&jwk.to_string());

        match jwk {
            Ok(jwk) => {
                // convert the parsed JWK to a DecodingKey and insert it into the decoding_keys map
                // if the JWK does not have a key ID (kid), return a MissingKeyId error
                let decoding_key =
                    DecodingKey::from_jwk(&jwk).map_err(KeyServiceError::KeyParsing)?;
                let key_id = jwk.common.key_id.ok_or(KeyServiceError::MissingKeyId)?;

                // insert the key into the map, using the key ID as the map's key
                decoding_keys.insert(key_id.into(), decoding_key);
            },
            Err(e) => {
                // if the error indicates an unknown variant, we can safely ignore it.
                //
                // TODO: also print it in the logging
                if !e.to_string().contains("unknown variant") {
                    return Err(KeyServiceError::KeyParsing(e.into()));
                }
            },
        };
    }

    Ok(decoding_keys)
}

/// Retrieves a [`OpenIdConfig`] based on the provided openid uri endpoint.
pub(crate) fn fetch_openid_config(
    openid_endpoint: &str,
    http_client: &HttpClient,
) -> Result<OpenIdConfig, KeyServiceError> {
    let conf_src: OpenIdConfigSource = http_client
        .get(openid_endpoint)?
        .json()
        .map_err(KeyServiceError::RequestDeserialization)?;

    let decoding_keys = fetch_decoding_keys(&conf_src.jwks_uri, http_client)?;

    Ok(OpenIdConfig::from_source(conf_src, decoding_keys))
}

/// A wrapper around `reqwest::blocking::Client` providing HTTP request functionality with retry logic.
///
/// The `HttpClient` struct allows for sending GET requests with a retry mechanism that attempts to
/// fetch the requested resource up to a maximum number of times if an error occurs.
pub struct HttpClient {
    client: reqwest::blocking::Client,
    max_retries: u32,
    retry_delay: Duration,
}

impl HttpClient {
    pub fn new() -> Result<Self, KeyServiceError> {
        let client = Client::builder()
            .build()
            .map_err(KeyServiceError::HttpClientInitialization)?;

        // We're doing this for now since `ServiceConfig::new` is also calling this
        // which is slowing down the tests. This would probably disappear once we
        // implement lazy loading.
        #[cfg(test)]
        let retry_delay = Duration::from_millis(10);
        #[cfg(not(test))]
        let retry_delay = Duration::from_secs(1);

        Ok(Self {
            client,
            max_retries: 3,
            retry_delay,
        })
    }

    /// Sends a GET request to the specified URI with retry logic.
    ///
    /// This method will attempt to fetch the resource up to 3 times, with an increasing delay
    /// between each attempt.
    fn get(&self, uri: &str) -> Result<reqwest::blocking::Response, KeyServiceError> {
        // Fetch the JWKS from the jwks_uri
        let mut attempts = 0;
        let response = loop {
            match self.client.get(uri).send() {
                Ok(response) => break response, // Exit loop on success
                Err(e) if attempts < self.max_retries => {
                    attempts += 1;
                    // TODO: pass this message to the logger
                    eprintln!(
                        "Request failed (attempt {} of {}): {}. Retrying...",
                        attempts, self.max_retries, e
                    );
                    sleep(self.retry_delay * attempts);
                },
                Err(e) => return Err(KeyServiceError::MaxHttpRetriesReached(e)), // Exit if max retries exceeded
            }
        };
        response
            .error_for_status()
            .map_err(KeyServiceError::HttpStatus)
    }
}

/// A service that manages key retrieval and caching for OpenID Connect configurations.
pub struct KeyService {
    idp_configs: HashMap<Box<str>, OpenIdConfig>, // <issuer (`iss`), OpenIdConfig>
    http_client: HttpClient,
}

impl KeyService {
    /// initializes a new `KeyService` with the provided OpenID configuration endpoints.
    ///
    /// this method fetches the OpenID configuration and the associated keys (JWKS) for each
    /// endpoint, populating the internal `idp_configs` map. any HTTP errors or parsing
    /// failures will return a corresponding `Error`.
    pub fn new(openid_conf_endpoints: Vec<&str>) -> Result<Self, KeyServiceError> {
        let mut idp_configs = HashMap::new();
        let http_client = HttpClient::new()?;

        // fetch IDP configs
        for endpoint in openid_conf_endpoints {
            let conf = fetch_openid_config(endpoint, &http_client)?;
            idp_configs.insert(conf.issuer.clone(), conf);
        }

        Ok(Self {
            idp_configs,
            http_client,
        })
    }

    /// retrieves a decoding key based on the provided key ID (`kid`).
    ///
    /// this method first attempts to retrieve the key from the local key store. if the key
    /// is not found, it will refresh the JWKS and try again. if the key is still not found,
    /// an error of type `KeyNotFound` is returned.
    pub fn get_key(&self, kid: &str) -> Result<Arc<DecodingKey>, KeyServiceError> {
        for iss in self.idp_configs.keys() {
            // first try to get the key from the local keystore
            if let Some(key) = self.get_key_from_iss(iss, kid)? {
                return Ok(key.clone());
            } else {
                // TODO: pass this on to the logger
                eprintln!("could not find {}, updating jwks", kid);
                // if the key is not found in the local keystore, update
                // the local keystore and try again
                self.update_jwks_for_iss(iss)?;
                if let Some(key) = self.get_key_from_iss(iss, kid)? {
                    return Ok(key.clone());
                }
            }
        }

        Err(KeyServiceError::KeyNotFound(kid.into()))
    }

    /// helper function to retrieve a key for a specific issuer (`iss`).
    fn get_key_from_iss(
        &self,
        iss: &str,
        kid: &str,
    ) -> Result<Option<Arc<DecodingKey>>, KeyServiceError> {
        if let Some(idp) = self.idp_configs.get(iss) {
            let decoding_keys = idp
                .decoding_keys
                .read()
                .map_err(|_| KeyServiceError::Lock)?;
            if let Some(key) = decoding_keys.get(kid) {
                return Ok(Some(key.clone()));
            }
        }

        Ok(None)
    }

    /// updates the JWKS for a given issuer (`iss`).
    ///
    /// this method fetches a fresh set of keys from the JWKS URI of the given issuer
    /// and updates the local key store.
    fn update_jwks_for_iss(&self, iss: &str) -> Result<(), KeyServiceError> {
        match self.idp_configs.get(iss) {
            Some(conf) => Ok(Self::update_decoding_keys(&self.http_client, conf)?),
            // do nothing if the issuer isn't in the current store
            None => Ok(()),
        }
    }

    /// Updates the keys in the given config
    fn update_decoding_keys(
        http_client: &HttpClient,
        conf: &OpenIdConfig,
    ) -> Result<(), KeyServiceError> {
        let mut fetched_keys = HashMap::new();

        // Fetch the JWKS from the jwks_uri
        let response = http_client.get(&conf.jwks_uri)?;

        // Deserialize the response into a Jwks
        let jwks: Jwks = response
            .json()
            .map_err(KeyServiceError::RequestDeserialization)?;

        // Deserialize the Jwk into multiple Decoding Keys then store them
        for jwk in jwks.keys {
            let jwk = serde_json::from_str::<Jwk>(&jwk.to_string());

            match jwk {
                Ok(jwk) => {
                    // convert the parsed JWK to a DecodingKey and insert it into the decoding_keys map
                    // if the JWK does not have a key ID (kid), return a MissingKeyId error
                    let decoding_key =
                        DecodingKey::from_jwk(&jwk).map_err(KeyServiceError::KeyParsing)?;
                    let key_id = jwk.common.key_id.ok_or(KeyServiceError::MissingKeyId)?;

                    // insert the key into the map, using the key ID as the map's key
                    fetched_keys.insert(key_id.into(), Arc::new(decoding_key));
                },
                Err(e) => {
                    // if the error indicates an unknown variant, we can safely ignore it.
                    //
                    // TODO: also print it in the logging
                    if !e.to_string().contains("unknown variant") {
                        return Err(KeyServiceError::KeyParsing(e.into()));
                    }
                },
            };
        }

        let mut decoding_keys = conf
            .decoding_keys
            .write()
            .map_err(|_| KeyServiceError::Lock)?;
        *decoding_keys = fetched_keys;

        Ok(())
    }
}

/// A simple struct to deserialize a collection of JWKs (JSON Web Keys).
///
/// This struct holds the raw keys in a vector of `serde_json::Value`, allowing
/// the keys to be processed or validated individually. It is primarily used
/// as an intermediary step for deserialization before iterating over each key
/// to check for errors or unsupported algorithms and skipping any invalid keys.
#[derive(Deserialize)]
struct Jwks {
    keys: Vec<serde_json::Value>,
}
