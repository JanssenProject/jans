/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod error;
mod openid_config;

pub use error::KeyServiceError;
use jsonwebtoken::jwk::JwkSet;
use jsonwebtoken::DecodingKey;
use openid_config::*;
use reqwest::blocking::Client;
use std::collections::HashMap;
use std::sync::Arc;

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
    pub fn new(openid_conf_endpoints: Vec<&str>) -> Result<Self, KeyServiceError> {
        let mut idp_configs = HashMap::new();
        let http_client = Client::builder().build().map_err(KeyServiceError::Http)?;

        // fetch IDP configs
        for endpoint in openid_conf_endpoints {
            let conf_src: OpenIdConfigSource = http_client
                .get(endpoint)
                .send()
                .map_err(KeyServiceError::Http)?
                .error_for_status()
                .map_err(KeyServiceError::Http)?
                .json()
                .map_err(KeyServiceError::RequestDeserialization)?;
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
                .map_err(KeyServiceError::Http)?
                .error_for_status()
                .map_err(KeyServiceError::Http)?
                .json()
                .map_err(KeyServiceError::RequestDeserialization)?;
            let mut decoding_keys = conf
                .decoding_keys
                .write()
                .map_err(|_| KeyServiceError::Lock)?;
            for jwk in jwks.keys {
                let decoding_key =
                    DecodingKey::from_jwk(&jwk).map_err(KeyServiceError::KeyParsing)?;
                let key_id = jwk.common.key_id.ok_or(KeyServiceError::MissingKeyId)?;
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
    pub fn get_key(&self, kid: &str) -> Result<Arc<DecodingKey>, KeyServiceError> {
        for (iss, config) in &self.idp_configs {
            // first try to get the key from the local keystore
            if let Some(key) = self.get_key_from_iss(iss, kid)? {
                return Ok(key.clone());
            } else {
                // TODO: pass this on to the logger
                eprintln!("could not find {}, updating jwks", kid);
                // if the key is not found in the local keystore, update
                // the local keystore and try again
                self.update_jwks(iss);
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
    fn update_jwks(&self, iss: &str) -> Result<(), KeyServiceError> {
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
            .map_err(KeyServiceError::Http)?
            .error_for_status()
            .map_err(KeyServiceError::Http)?
            .json()
            .map_err(KeyServiceError::RequestDeserialization)?;
        for jwk in jwks.keys {
            let decoding_key = DecodingKey::from_jwk(&jwk).map_err(KeyServiceError::KeyParsing)?;
            let key_id = jwk.common.key_id.ok_or(KeyServiceError::MissingKeyId)?;
            new_keys.insert(key_id.into(), Arc::new(decoding_key));
        }

        // we reassign the keys after fetching and deserializing the keys
        // so we don't lose the old ones in case the process fails
        let mut decoding_keys = conf
            .decoding_keys
            .write()
            .map_err(|_| KeyServiceError::Lock)?;
        *decoding_keys = new_keys;

        Ok(())
    }
}
