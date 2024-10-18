/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde::{Deserialize, Serialize};

#[derive(Deserialize, Serialize)]
pub struct AccessToken {
    pub iss: String,
    pub aud: String,
    #[serde(flatten)]
    pub extra: serde_json::Value,
}

#[derive(Deserialize, Serialize)]
pub struct IdToken {
    pub iss: String,
    pub aud: String,
    pub sub: String,
    #[serde(flatten)]
    pub extra: serde_json::Value,
}

#[derive(Deserialize, Serialize)]
pub struct UserInfoToken {
    pub iss: String,
    pub aud: String,
    pub sub: String,
    #[serde(flatten)]
    pub extra: serde_json::Value,
}
