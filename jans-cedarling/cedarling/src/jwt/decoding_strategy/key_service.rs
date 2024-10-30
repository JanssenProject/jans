/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::super::InitError;
use jsonwebtoken::jwk::JwkSet;
use jsonwebtoken::DecodingKey;
use reqwest::blocking::Client;
use serde::Deserialize;
use std::collections::HashMap;
use std::sync::Arc;
use std::sync::RwLock;

/// represents the source data for OpenID configuration.
#[derive(Deserialize)]
pub struct OpenIdConfigSource {
    issuer: Box<str>,
    jwks_uri: Box<str>,
    // The following values are also normally returned when sending
    // a GET request to the `openid_configuration_endpoint` but are
    // not currently being used.
    //
    // authorization_endpoint: Box<str>,
    // device_authorization_endpoint: Box<str>,
    // token_endpoint: Box<str>,
    // userinfo_endpoint: Box<str>,
    // revocation_endpoint: Box<str>,
    // response_types_supported: Vec<Box<str>>,
    // subject_types_supported: Vec<Box<str>>,
    // id_token_signing_algs_values_supported: Vec<Box<str>>,
    // scopes_supported: Vec<Box<str>>,
    // claims_supported: Vec<Box<str>>,
}

/// represents the OpenID configuration for an identity provider.
pub struct OpenIdConfig {
    pub jwks_uri: Box<str>,
    // <key_id (`kid`), DecodingKey>
    pub decoding_keys: Arc<RwLock<HashMap<Box<str>, Arc<DecodingKey>>>>,
}

impl OpenIdConfig {
    /// creates an `OpenIdConfig` from the provided source.
    ///
    /// this method extracts the issuer and constructs a new `OpenIdConfig`
    /// instance, initializing the decoding keys storage.
    pub fn from_source(src: OpenIdConfigSource) -> (Box<str>, OpenIdConfig) {
        let issuer = src.issuer;
        (
            issuer,
            OpenIdConfig {
                jwks_uri: src.jwks_uri,
                decoding_keys: Arc::new(RwLock::new(HashMap::new())),
            },
        )
    }
}

pub struct KeyService {
    idp_configs: HashMap<Box<str>, OpenIdConfig>, // <issuer (`iss`), OpenIdConfig>
    http_client: Client,
}

#[allow(unused)]
impl KeyService {
    /// initializes a new `KeyService` with the provided OpenID configuration endpoints.
    ///
    /// this method fetches the OpenID configuration and the associated keys (JWKS) for each
    /// endpoint, populating the internal `idp_configs` map. any HTTP errors or parsing
    /// failures will return a corresponding `Error`.
    pub fn new(openid_conf_endpoints: Vec<&str>) -> Result<Self, InitError> {
        let mut idp_configs = HashMap::new();
        let http_client = Client::builder()
            .build()
            .map_err(|e| InitError::KeyService(Error::Http(e)))?;

        // fetch IDP configs
        for endpoint in openid_conf_endpoints {
            let conf_src: OpenIdConfigSource = http_client
                .get(endpoint)
                .send()
                .map_err(|e| InitError::KeyService(Error::Http(e)))?
                .error_for_status()
                .map_err(|e| InitError::KeyService(Error::Http(e)))?
                .json()
                .map_err(|e| InitError::KeyService(Error::RequestDeserialization(e)))?;
            let (issuer, conf) = OpenIdConfig::from_source(conf_src);
            idp_configs.insert(issuer, conf);
        }

        /// retrieves a decoding key based on the provided key ID (`kid`).
        ///
        /// this method first attempts to retrieve the key from the local key store. if the key
        /// is not found, it will refresh the JWKS and try again. if the key is still not found,
        /// an error of type `KeyNotFound` is returned.
        for (iss, conf) in &mut idp_configs {
            let jwks: JwkSet = http_client
                .get(&*conf.jwks_uri)
                .send()
                .map_err(|e| InitError::KeyService(Error::Http(e)))?
                .error_for_status()
                .map_err(|e| InitError::KeyService(Error::Http(e)))?
                .json()
                .map_err(|e| InitError::KeyService(Error::RequestDeserialization(e)))?;
            let mut decoding_keys = conf
                .decoding_keys
                .write()
                .map_err(|_| InitError::KeyService(Error::Lock))?;
            for jwk in jwks.keys {
                let decoding_key = DecodingKey::from_jwk(&jwk)
                    .map_err(|e| InitError::KeyService(Error::KeyParsing(e)))?;
                let key_id = jwk
                    .common
                    .key_id
                    .ok_or(InitError::KeyService(Error::MissingKeyId))?;
                decoding_keys.insert(key_id.into(), Arc::new(decoding_key));
            }
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
    pub fn get_key(&self, kid: &str) -> Result<Arc<DecodingKey>, Error> {
        for (iss, config) in &self.idp_configs {
            // first try to get the key from the local keystore
            if let Some(key) = self.get_key_from_iss(iss, kid)? {
                return Ok(key.clone());
            } else {
                eprintln!("could not find {}, updating jwks", kid);
                // if the key is not found in the local keystore, update
                // the local keystore and try again
                self.update_jwks(iss);
                if let Some(key) = self.get_key_from_iss(iss, kid)? {
                    return Ok(key.clone());
                }
            }
        }

        Err(Error::KeyNotFound(kid.into()))
    }

    /// helper function to retrieve a key for a specific issuer (`iss`).
    fn get_key_from_iss(&self, iss: &str, kid: &str) -> Result<Option<Arc<DecodingKey>>, Error> {
        if let Some(idp) = self.idp_configs.get(iss) {
            let decoding_keys = idp.decoding_keys.read().map_err(|_| Error::Lock)?;
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
    fn update_jwks(&self, iss: &str) -> Result<(), Error> {
        let conf = match self.idp_configs.get(iss) {
            Some(conf) => conf,
            // do nothing if the issuer isn't in the current store
            None => return Ok(()),
        };

        // fetch fresh keys
        let mut new_keys = HashMap::new();
        let jwks: JwkSet = self
            .http_client
            .get(&*conf.jwks_uri)
            .send()
            .map_err(Error::Http)?
            .error_for_status()
            .map_err(Error::Http)?
            .json()
            .map_err(Error::RequestDeserialization)?;
        for jwk in jwks.keys {
            let decoding_key = DecodingKey::from_jwk(&jwk).map_err(Error::KeyParsing)?;
            let key_id = jwk.common.key_id.ok_or(Error::MissingKeyId)?;
            new_keys.insert(key_id.into(), Arc::new(decoding_key));
        }

        // we reassign the keys after fetching and deserializing the keys
        // so we don't lose the old ones in case the process fails
        let mut decoding_keys = conf.decoding_keys.write().map_err(|_| Error::Lock)?;
        *decoding_keys = new_keys;

        Ok(())
    }
}

/// Error type for issues encountered in the Key Service.
///
/// The `KeyService` is responsible for retrieving and managing keys, used to
/// decode JWTs. This enum represents the various errors that can occur when 
/// interacting with the key service, including network issues, parsing problems, 
/// and other key-related errors.
#[derive(thiserror::Error, Debug)]

pub enum Error {
    /// The specified key ID (`kid`) was not found in the JSON Web Key Set (JWKS).
    ///
    /// This error occurs when the JWKS does not contain a key matching the provided
    /// `kid`, which is required for verifying JWTs.
    #[error("No key with `kid`=\"{0}\" found in the JWKS.")]
    KeyNotFound(Box<str>),

    /// An HTTP error occurred during the request to fetch the JWKS.
    ///
    /// This error occurs when the `KeyService` makes an HTTP request to retrieve
    /// the JWKS and encounters issues such as connectivity failures, timeouts, or 
    /// invalid responses.
    #[error("HTTP error occurred: {0}")]
    Http(#[source] reqwest::Error),

    /// Failed to deserialize the HTTP response when fetching the JWKS.
    ///
    /// This error occurs when the response body from the HTTP request cannot be
    /// deserialized into the expected JSON format, possibly due to invalid or
    /// malformed data from the server.
    #[error("Failed to deserialize the response from the HTTP request: {0}")]
    RequestDeserialization(#[source] reqwest::Error),

    /// Failed to parse a decoding key from the JWKS JSON data.
    ///
    /// This error occurs when the JWKS contains invalid or unsupported key data
    /// making it impossible to parse a valid key that can be used for JWT decoding.
    #[error("Error parsing decoding key from JWKS JSON: {0}")]
    KeyParsing(#[source] jsonwebtoken::errors::Error),

    /// The JSON Web Key (JWK) is missing the required `kid` field.
    ///
    /// The `kid` (Key ID) is necessary to identify the correct key in the JWKS
    /// when verifying a JWT. This error occurs if the JWK does not contain a `kid`.
    #[error("The JWK is missing a required `kid`.")]
    MissingKeyId,

    /// Failed to acquire a write lock on the decoding keys.
    ///
    /// This error is occurs when the service attempts to acquire a write lock
    /// on the set of decoding keys, but the lock is poisoned (e.g., due to a panic
    /// in another thread), making it unsafe to proceed.
    #[error("Failed to acquire write lock on decoding keys.")]
    Lock,
}
