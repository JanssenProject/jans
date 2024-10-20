/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebtoken::DecodingKey;
use serde::Deserialize;
use std::{
    collections::HashMap,
    sync::{Arc, RwLock},
};

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
