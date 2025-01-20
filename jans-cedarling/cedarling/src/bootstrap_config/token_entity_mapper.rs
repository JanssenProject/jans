// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::Deserialize;
use std::collections::HashMap;

/// Describes the mapping for **Token Entity** -> **Principal Entity**
///
/// This tells cedarling where to put token entity references in the
/// target principal entities.
#[derive(Debug, Deserialize, PartialEq, Clone)]
pub struct BsTknPrincipalMapper(HashMap<String, String>);

// TODO: it might be useful to implement mapping for multiple principals
impl Default for BsTknPrincipalMapper {
    fn default() -> Self {
        Self(HashMap::from([
            ("access_token".to_string(), "Workload".to_string()),
            ("id_token".to_string(), "User".to_string()),
            ("userinfo_token".to_string(), "User".to_string()),
        ]))
    }
}

impl From<BsTknPrincipalMapper> for HashMap<String, String> {
    fn from(value: BsTknPrincipalMapper) -> Self {
        value.0
    }
}

impl From<HashMap<String, String>> for BsTknPrincipalMapper {
    fn from(value: HashMap<String, String>) -> Self {
        Self(value)
    }
}
