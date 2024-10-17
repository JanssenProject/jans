use std::collections::HashMap;

use jsonwebtoken::DecodingKey;
use serde::Deserialize;

use super::Error;

#[derive(Deserialize)]
#[allow(dead_code)]
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

pub struct OpenIdConfig {
    pub jwks_uri: Box<str>,
    pub decoding_keys: HashMap<Box<str>, DecodingKey>, // <key_id (`kid`), DecodingKey>
}

#[allow(dead_code)]
impl OpenIdConfig {
    pub fn from_slice(bytes: &[u8]) -> Result<(Box<str>, OpenIdConfig), Error> {
        let conf_src: OpenIdConfigSource = serde_json::from_slice(&bytes)?;
        let issuer = conf_src.issuer;
        Ok((
            issuer,
            OpenIdConfig {
                jwks_uri: conf_src.jwks_uri,
                decoding_keys: HashMap::new(),
            },
        ))
    }
}
