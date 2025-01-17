// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{FeatureToggle, TokenValidationConfig};
use serde::Deserialize;
use std::collections::HashMap;

/// Token validation configs from the boostrap config
#[derive(Debug, Deserialize, PartialEq, Clone)]
pub struct BsTknValidationConfigs(HashMap<String, BsTknValidationConfig>);

impl Default for BsTknValidationConfigs {
    fn default() -> Self {
        Self(HashMap::from([
            ("access_token".to_string(), BsTknValidationConfig {
                iss: FeatureToggle::Enabled,
                sub: FeatureToggle::Disabled,
                aud: FeatureToggle::Disabled,
                exp: FeatureToggle::Enabled,
                nbf: FeatureToggle::Disabled,
                iat: FeatureToggle::Disabled,
                jti: FeatureToggle::Enabled,
            }),
            ("id_token".to_string(), BsTknValidationConfig {
                iss: FeatureToggle::Enabled,
                sub: FeatureToggle::Enabled,
                aud: FeatureToggle::Enabled,
                exp: FeatureToggle::Enabled,
                nbf: FeatureToggle::Disabled,
                iat: FeatureToggle::Disabled,
                jti: FeatureToggle::Disabled,
            }),
            ("userinfo_token".to_string(), BsTknValidationConfig {
                iss: FeatureToggle::Enabled,
                sub: FeatureToggle::Enabled,
                aud: FeatureToggle::Enabled,
                exp: FeatureToggle::Enabled,
                nbf: FeatureToggle::Disabled,
                iat: FeatureToggle::Disabled,
                jti: FeatureToggle::Disabled,
            }),
        ]))
    }
}

/// Token validation config from the boostrap config
#[derive(Debug, Deserialize, PartialEq, Clone)]
pub struct BsTknValidationConfig {
    #[serde(default)]
    iss: FeatureToggle,
    #[serde(default)]
    sub: FeatureToggle,
    #[serde(default)]
    aud: FeatureToggle,
    #[serde(default)]
    exp: FeatureToggle,
    #[serde(default)]
    nbf: FeatureToggle,
    #[serde(default)]
    iat: FeatureToggle,
    #[serde(default)]
    jti: FeatureToggle,
}

impl From<BsTknValidationConfig> for TokenValidationConfig {
    fn from(setting: BsTknValidationConfig) -> Self {
        Self {
            iss_validation: setting.iss.into(),
            aud_validation: setting.aud.into(),
            sub_validation: setting.sub.into(),
            jti_validation: setting.jti.into(),
            iat_validation: setting.iat.into(),
            exp_validation: setting.exp.into(),
            nbf_validation: setting.nbf.into(),
        }
    }
}

impl Into<HashMap<String, TokenValidationConfig>> for BsTknValidationConfigs {
    fn into(self) -> HashMap<String, TokenValidationConfig> {
        self.0
            .into_iter()
            .map(|(tkn_name, conf)| (tkn_name, conf.into()))
            .collect()
    }
}
