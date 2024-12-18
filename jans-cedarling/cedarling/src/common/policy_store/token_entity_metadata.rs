/*
* This software is available under the Apache-2.0 license.

* See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
*
* Copyright (c) 2024, Gluu, Inc.
*/

pub use super::claim_mapping::ClaimMappings;
use super::parse_option_string;
use serde::Deserialize;

///  Structure for storing mapping JWT claims to `cedar-policy` custom defined types in the `schema`.
///
/// An optional mapping of claims to their values. Each claim is represented
/// by a key-value pair where the key is the claim name and the value is
/// a `ClaimMapping` struct.
#[derive(Debug, PartialEq, Clone, Default, Deserialize)]
pub struct TokenEntityMetadata {
    /// Indicates if the access token is trusted.
    #[serde(default)]
    pub trusted: bool,
    #[serde(default, deserialize_with = "parse_option_string")]
    /// An optional string representing the principal identifier (e.g., `jti`).
    pub principal_identifier: Option<String>,
    /// The claim used to create the user id
    #[serde(deserialize_with = "parse_option_string", default)]
    pub user_id: Option<String>,
    /// An optional string indicating the role mapping for the user.
    #[serde(deserialize_with = "parse_option_string", default)]
    pub role_mapping: Option<String>,
    /// An optional mapping of claims to their values. Each claim is represented
    /// by a key-value pair where the key is the claim name and the value is
    /// a `ClaimMapping` struct.
    #[serde(default)]
    pub claim_mapping: ClaimMappings,
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
                ..Default::default()
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
                ..Default::default()
            }, 
            "Expected YAML with user_id and empty role_mapping to be parsed into TokenEntityMetadata"
        );
    }
}
