mod error;
mod openid_config;
#[cfg(test)]
mod test;
mod traits;

use bytes::Bytes;
use di::{self, DependencySupplier};
use jsonwebtoken::jwk::JwkSet;
use jsonwebtoken::DecodingKey;
use openid_config::OpenIdConfig;
use reqwest::blocking::get;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use traits::HttpGet;

use crate::models::policy_store::PolicyStore;
pub use error::Error;

use super::traits::GetKey;
pub struct HttpService;

impl HttpGet for HttpService {
    fn get(&self, uri_str: &str) -> Result<Bytes, Error> {
        match get(uri_str)?.error_for_status() {
            Ok(resp) => Ok(resp.bytes()?),
            Err(e) => {
                let url: Box<str> = e
                    .url()
                    .expect("http response should have a url")
                    .as_str()
                    .into();
                let status: Box<str> = e
                    .status()
                    .expect("http response should have a status")
                    .as_str()
                    .into();

                Err(Error::HttpGetError(url, status))
            },
        }
    }
}

pub struct KeyService {
    idp_configs: HashMap<Box<str>, OpenIdConfig>, // <issuer (`iss`), OpenIdConfig>
    http_service: Arc<Mutex<dyn HttpGet>>,
}

#[allow(unused)]
impl KeyService {
    #[cfg(test)]
    pub fn new(
        openid_conf_endpoints: Vec<&str>,
        http_service: Arc<Mutex<dyn HttpGet>>,
    ) -> Result<Self, Error> {
        let mut idp_configs = HashMap::new();

        // fetch IDP configs
        for endpoint in &openid_conf_endpoints {
            let http_service = http_service.lock().unwrap();
            let conf_bytes = http_service.get(endpoint)?;
            let (issuer, conf) = OpenIdConfig::from_slice(&conf_bytes)?;
            idp_configs.insert(issuer, conf);
        }

        // fetch keys
        for (iss, conf) in &mut idp_configs {
            let http_service = http_service.lock().unwrap();
            let jwks_bytes = http_service.get(&conf.jwks_uri)?;
            let jwks: JwkSet = serde_json::from_slice(&jwks_bytes)?;
            for jwk in jwks.keys {
                let decoding_key = DecodingKey::from_jwk(&jwk)?;
                let key_id = jwk.common.key_id.ok_or(Error::JwkMissingKeyId)?;
                conf.decoding_keys.insert(key_id.into(), decoding_key);
            }
        }

        Ok(Self {
            idp_configs,
            http_service,
        })
    }

    pub fn new_with_container(dep_map: &di::DependencyMap) -> Self {
        let _policies: Arc<PolicyStore> = dep_map.get();
        // Get idp here from the policy store
        todo!()
    }

    pub fn get_key(&self, iss: &str, kid: &str) -> Option<&DecodingKey> {
        let idp = match self.idp_configs.get(iss) {
            Some(idp) => idp,
            None => return None,
        };

        match idp.decoding_keys.get(kid) {
            Some(key) => Some(&key),
            None => None,
        }
    }

    pub fn get_key_or_update_jwks(&mut self, iss: &str, kid: &str) -> Option<&DecodingKey> {
        match self.get_key(iss, kid) {
            Some(_) => todo!(),
            None => {
                self.update_jwks(iss);
                self.get_key(iss, kid)
            },
        }
    }

    fn update_jwks(&mut self, issuer: &str) -> Result<(), Error> {
        let conf = self
            .idp_configs
            .get_mut(issuer)
            .ok_or(Error::UnknownIssuer(issuer.into()))?;

        // fetch fresh keys
        let mut new_keys = HashMap::new();
        let http_service = self.http_service.lock().unwrap();
        let jwks_bytes = http_service.get(&conf.jwks_uri)?;
        let jwks: JwkSet = serde_json::from_slice(&jwks_bytes)?;
        for jwk in jwks.keys {
            let decoding_key = DecodingKey::from_jwk(&jwk)?;
            let key_id = jwk.common.key_id.ok_or(Error::JwkMissingKeyId)?;
            new_keys.insert(key_id.into(), decoding_key);
        }

        // we reassign the keys after fetching and deserializing the keys
        // so we don't lose the old ones in case the process fails
        conf.decoding_keys = new_keys;

        Ok(())
    }
}

impl GetKey for KeyService {
    fn get_key(&self, kid: &str) -> Result<&jsonwebtoken::DecodingKey, super::Error> {
        for conf in &self.idp_configs {
            if let Some(key) = self.get_key(&conf.0, &kid) {
                return Ok(key);
            }
        }

        Err(super::Error::MissingKey(kid.into()))
    }
}
