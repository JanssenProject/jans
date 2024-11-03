/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::authz;
use serde::Deserialize;
use serde_json::Value;
use std::collections::HashMap;

pub trait JsonWebToken {
    fn claims(self) -> HashMap<String, Value>;
}

#[derive(Deserialize, Debug, PartialEq)]
pub struct AccessToken {
    pub iss: String,
    pub aud: String,
    pub sub: String,
    #[serde(flatten)]
    claims: HashMap<String, Value>,
}

impl JsonWebToken for AccessToken {
    fn claims(mut self) -> HashMap<String, Value> {
        self.claims
            .insert("iss".to_string(), Value::String(self.iss));
        self.claims
            .insert("aud".to_string(), Value::String(self.aud));
        self.claims
            .insert("sub".to_string(), Value::String(self.sub));
        self.claims
    }
}

impl From<AccessToken> for authz::AccessTokenData {
    fn from(access_token: AccessToken) -> Self {
        authz::AccessTokenData(authz::TokenPayload {
            payload: access_token.claims(),
        })
    }
}

#[derive(Deserialize, Debug, PartialEq)]
pub struct IdToken {
    pub iss: String,
    pub aud: String,
    pub sub: String,
    #[serde(flatten)]
    claims: HashMap<String, Value>,
}

impl JsonWebToken for IdToken {
    fn claims(mut self) -> HashMap<String, Value> {
        self.claims
            .insert("iss".to_string(), Value::String(self.iss));
        self.claims
            .insert("aud".to_string(), Value::String(self.aud));
        self.claims
            .insert("sub".to_string(), Value::String(self.sub));
        self.claims
    }
}

impl From<IdToken> for authz::IdTokenData {
    fn from(id_token: IdToken) -> Self {
        authz::IdTokenData(authz::TokenPayload {
            payload: id_token.claims(),
        })
    }
}

#[derive(Deserialize, Debug, PartialEq)]
pub struct UserInfoToken {
    pub iss: String,
    pub aud: String,
    pub sub: String,
    #[serde(flatten)]
    claims: HashMap<String, Value>,
}

impl JsonWebToken for UserInfoToken {
    fn claims(mut self) -> HashMap<String, Value> {
        self.claims
            .insert("iss".to_string(), Value::String(self.iss));
        self.claims
            .insert("aud".to_string(), Value::String(self.aud));
        self.claims
            .insert("sub".to_string(), Value::String(self.sub));
        self.claims
    }
}

impl From<UserInfoToken> for authz::UserInfoTokenData {
    fn from(userinfo_token: UserInfoToken) -> Self {
        authz::UserInfoTokenData(authz::TokenPayload {
            payload: userinfo_token.claims(),
        })
    }
}
