/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::claim_mapping::ClaimMapping;
use super::{parse_option_hashmap, parse_option_string};
use serde::Deserialize;
use std::collections::HashMap;

/// Metadata associated with a token entity, which includes user identification,
/// role mappings, and claim mappings.
#[derive(Debug, PartialEq, Clone, Default, Deserialize)]
#[allow(dead_code)]
pub struct TokenEntityMetadata {
    /// An optional user identifier extracted from the token metadata.
    #[serde(deserialize_with = "parse_option_string", default)]
    pub user_id: Option<String>,
    /// An optional string indicating the role mapping for the user.
    #[serde(deserialize_with = "parse_option_string", default)]
    pub role_mapping: Option<String>,
    /// An optional mapping of claims to their values. Each claim is represented
    /// by a key-value pair where the key is the claim name and the value is
    /// a `ClaimMapping` struct.
    #[serde(deserialize_with = "parse_option_hashmap", default)]
    pub claim_mapping: Option<HashMap<String, ClaimMapping>>,
}

#[cfg(test)]
mod test {
    use super::TokenEntityMetadata;
    use serde_json::json;

     /// Test deserialization of `TokenEntityMetadata` from JSON.
    #[test]
    fn can_parse_from_json() {
        // Test case: Parsing an empty JSON object
        let json = json!({});
        let parsed = serde_json::from_value::<TokenEntityMetadata>(json)
            .expect("Failed to parse an empty JSON object into TokenEntityMetadata");
        assert_eq!(
            parsed,
            TokenEntityMetadata::default(),
            "Expected empty JSON to be parsed into default TokenEntityMetadata"
        );

         // Test case: Parsing JSON with specified `user_id` and `role_mapping`
        let json = json!({
            "user_id": "sub",
            "role_mapping": "",
        });
        let parsed = serde_json::from_value::<TokenEntityMetadata>(json).expect(
            "Failed to parse JSON object with user_id and role_mapping into TokenEntityMetadata",
        );
        assert_eq!(
            parsed, 
            TokenEntityMetadata { 
                user_id: Some("sub".into()), 
                role_mapping: None, 
                claim_mapping: None 
            }, 
            "Expected JSON with user_id and empty role_mapping to be parsed into TokenEntityMetadata"
        );
    }
    
    /// Test deserialization of `TokenEntityMetadata` from YAML.
    #[test]
    fn can_parse_from_yaml() {
        // Test case: Parsing an empty YAML string
        let yaml = "";
        let parsed = serde_yml::from_str::<TokenEntityMetadata>(yaml)
            .expect("Failed to parse an empty YAML object into TokenEntityMetadata");
        assert_eq!(
            parsed,
            TokenEntityMetadata::default(),
            "Expected empty YAML to be parsed into default TokenEntityMetadata"
        );
        
        // Test case: Parsing YAML with specified `user_id` and `role_mapping`
        let yaml = "
            user_id: 'sub'
            role_mapping: ''
        ";
        let parsed = serde_yml::from_str::<TokenEntityMetadata>(yaml).expect(
            "Failed to parse YAML object with user_id and role_mapping into TokenEntityMetadata",
        );
        assert_eq!(
            parsed, 
            TokenEntityMetadata { 
                user_id: Some("sub".into()), 
                role_mapping: None, 
                claim_mapping: None 
            }, 
            "Expected YAML with user_id and empty role_mapping to be parsed into TokenEntityMetadata"
        );
    }
}
