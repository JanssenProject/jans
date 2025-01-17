// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::Deserialize;
use std::collections::HashMap;

#[derive(Debug, Deserialize, PartialEq, Clone)]
pub struct BsTknPrincipalMapper(HashMap<String, String>);

impl Default for BsTknPrincipalMapper {
    fn default() -> Self {
        Self(HashMap::from([
            ("access_token".to_string(), "Jans::Workload".to_string()),
            ("id_token".to_string(), "Jans::User".to_string()),
            ("userinfo_token".to_string(), "Jans::User".to_string()),
        ]))
    }
}

impl Into<HashMap<String, String>> for BsTknPrincipalMapper {
    fn into(self) -> HashMap<String, String> {
        self.0
    }
}

impl From<HashMap<String, String>> for BsTknPrincipalMapper {
    fn from(value: HashMap<String, String>) -> Self {
        Self(value)
    }
}
