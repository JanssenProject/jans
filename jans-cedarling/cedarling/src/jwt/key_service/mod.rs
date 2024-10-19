/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod error;
mod openid_config;
#[cfg(test)]
mod test;

use jsonwebtoken::jwk::JwkSet;
use jsonwebtoken::DecodingKey;
use openid_config::{OpenIdConfig, OpenIdConfigSource};
use reqwest::blocking::get;
use std::collections::HashMap;
use std::sync::Arc;

pub use error::Error;

pub struct KeyService {
    idp_configs: HashMap<Box<str>, OpenIdConfig>, // <issuer (`iss`), OpenIdConfig>
}

#[allow(unused)]
impl KeyService {
    pub fn new(openid_conf_endpoints: Vec<&str>) -> Result<Self, Error> {
        let mut idp_configs = HashMap::new();

        // fetch IDP configs
        for endpoint in openid_conf_endpoints {
            let conf_src: OpenIdConfigSource = get(endpoint)?.json()?;
            let (issuer, conf) = OpenIdConfig::from_source(conf_src);
            println!("deserialized conf bytes");
            idp_configs.insert(issuer, conf);
        }
        println!("got idp conf");

        // fetch keys
        for (iss, conf) in &mut idp_configs {
            let jwks_bytes = get(&*conf.jwks_uri)?.bytes()?;
            println!("got bytes");
            let jwks: JwkSet = serde_json::from_slice(&jwks_bytes)?;
            println!("got jwks");
            let mut decoding_keys = conf.decoding_keys.write().unwrap();
            for jwk in jwks.keys {
                let decoding_key = DecodingKey::from_jwk(&jwk)?;
                let key_id = jwk.common.key_id.ok_or(Error::JwkMissingKeyId)?;
                decoding_keys.insert(key_id.into(), Arc::new(decoding_key));
            }
        }

        Ok(Self { idp_configs })
    }

    pub fn get_key(&self, kid: &str) -> Result<Arc<DecodingKey>, Error> {
        for (iss, config) in &self.idp_configs {
            // first try to get the key from the local keystore
            if let Some(key) = self.get_key_from_iss(&iss, &kid) {
                return Ok(key.clone());
            } else {
                // if the key is not found in the local keystore, update
                // the local keystore and try again
                self.update_jwks(iss);
                if let Some(key) = self.get_key_from_iss(&iss, &kid) {
                    return Ok(key.clone());
                }
            }
        }

        Err(Error::KeyNotFound(kid.into()))
    }

    fn get_key_from_iss(&self, iss: &str, kid: &str) -> Option<Arc<DecodingKey>> {
        if let Some(idp) = self.idp_configs.get(iss) {
            let decoding_keys = idp.decoding_keys.read().unwrap();
            if let Some(key) = decoding_keys.get(kid) {
                return Some(key.clone());
            }
        }

        None
    }

    fn update_jwks(&self, issuer: &str) -> Result<(), Error> {
        let conf = self
            .idp_configs
            .get(issuer)
            .ok_or(Error::UnknownIssuer(issuer.into()))?;

        // fetch fresh keys
        let mut new_keys = HashMap::new();
        let jwks_bytes = get(&*conf.jwks_uri)?.bytes()?;
        let jwks: JwkSet = serde_json::from_slice(&jwks_bytes)?;
        for jwk in jwks.keys {
            let decoding_key = DecodingKey::from_jwk(&jwk)?;
            let key_id = jwk.common.key_id.ok_or(Error::JwkMissingKeyId)?;
            new_keys.insert(key_id.into(), Arc::new(decoding_key));
        }

        // we reassign the keys after fetching and deserializing the keys
        // so we don't lose the old ones in case the process fails
        let mut decoding_keys = conf.decoding_keys.write().unwrap();
        *decoding_keys = new_keys;

        Ok(())
    }
}
