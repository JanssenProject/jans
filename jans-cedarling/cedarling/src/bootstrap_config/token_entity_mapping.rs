// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::authorization_config::TokenEntityNames;
use serde::Deserialize;
use std::collections::HashMap;

/// Describes the mapping for **Token Name** -> **Token Entity Name**
///
/// This tells cedarling the entity names of the given tokens
#[derive(Debug, Deserialize, PartialEq, Clone)]
pub struct BsTknEntityMapping(HashMap<String, String>);

impl Default for BsTknEntityMapping {
    fn default() -> Self {
        Self(HashMap::from([
            ("access_token".to_string(), "Access_token".to_string()),
            ("id_token".to_string(), "id_token".to_string()),
            ("userinfo_token".to_string(), "Userinfo_token".to_string()),
        ]))
    }
}

#[cfg(test)]
impl BsTknEntityMapping {
    pub fn set_mapping(&mut self, tkn_name: impl ToString, tkn_entity_name: impl ToString) {
        self.0
            .insert(tkn_name.to_string(), tkn_entity_name.to_string());
    }
}

impl Into<TokenEntityNames> for BsTknEntityMapping {
    fn into(self) -> TokenEntityNames {
        TokenEntityNames(self.0)
    }
}

impl From<HashMap<String, String>> for BsTknEntityMapping {
    fn from(value: HashMap<String, String>) -> Self {
        Self(value)
    }
}
