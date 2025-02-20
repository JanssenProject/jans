// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;

pub use super::claim_mapping::ClaimMappings;
use super::parse_option_string;
use serde::Deserialize;
use typed_builder::TypedBuilder;

/// Structure for storing mapping JWT claims to `cedar-policy` custom defined types in the `schema`.
///
/// An optional mapping of claims to their values. Each claim is represented
/// by a key-value pair where the key is the claim name and the value is
/// a `ClaimMapping` struct.
#[derive(Debug, PartialEq, Clone, Deserialize, TypedBuilder)]
pub struct TokenEntityMetadata {
    /// Indicates if the access token is trusted.
    #[serde(default = "default_trusted")]
    #[builder(default = true)]
    pub trusted: bool,
    /// An optional string representing the principal identifier (e.g., `jti`).
    #[serde(default, deserialize_with = "parse_option_string")]
    #[builder(default)]
    pub principal_identifier: Option<String>,
    /// The claim used to create the user id
    #[serde(deserialize_with = "parse_option_string", default)]
    #[builder(default)]
    pub user_id: Option<String>,
    /// An optional string indicating the role mapping for the user.
    #[serde(deserialize_with = "parse_option_string", default)]
    #[builder(default)]
    pub role_mapping: Option<String>,
    /// An optional mapping of claims to their values. Each claim is represented
    /// by a key-value pair where the key is the claim name and the value is
    /// a `ClaimMapping` struct.
    #[serde(default)]
    #[builder(default)]
    pub claim_mapping: ClaimMappings,
    /// The claims in this Vec will be required on token validation and will be
    /// validated if it is a registered claim listed in [`RFC 7519, Section 4.1`] (https://datatracker.ietf.org/doc/html/rfc7519#section-4.1)
    #[serde(default)]
    #[builder(default)]
    pub required_claims: HashSet<String>,
    /// The Cedar entity name that represents this token
    pub entity_type_name: String,
    /// The Cedar entities that this token is an attribute of
    #[serde(default)]
    #[builder(default)]
    pub entity_mapping: HashSet<String>,
}

fn default_trusted() -> bool {
    true
}

#[cfg(test)]
mod test {
    use super::TokenEntityMetadata;
    use serde_json::json;

    /// Test deserialization of `TokenEntityMetadata` from JSON.
    #[test]
    fn can_parse_from_json() {
        // Test case: Parsing from a minimal JSON object
        let json = json!({
            "entity_type_name": "Jans::Access_token",
        });
        let parsed = serde_json::from_value::<TokenEntityMetadata>(json)
            .expect("Failed to parse an empty JSON object into TokenEntityMetadata");
        assert_eq!(
            parsed,
            TokenEntityMetadata::builder()
                .entity_type_name("Jans::Access_token".into())
                .build(),
            "Expected empty JSON to be parsed into default TokenEntityMetadata"
        );

        // Test case: Parsing JSON with specified `user_id` and `role_mapping`
        let json = json!({
            "entity_type_name": "Jans::Access_token",
            "user_id": "sub",
            "role_mapping": "",
        });
        let parsed = serde_json::from_value::<TokenEntityMetadata>(json).expect(
            "Failed to parse JSON object with user_id and role_mapping into TokenEntityMetadata",
        );
        assert_eq!(
            parsed,
            TokenEntityMetadata::builder()
                .user_id(Some("sub".into()))
                .entity_type_name("Jans::Access_token".into())
                .build(),
            "Expected JSON with user_id and empty role_mapping to be parsed into \
             TokenEntityMetadata"
        );
    }

    /// Test deserialization of `TokenEntityMetadata` from YAML.
    #[test]
    fn can_parse_from_yaml() {
        // Test case: Parsing an empty YAML string
        let yaml = "
            entity_type_name: Jans::Access_token
        ";
        let parsed = serde_yml::from_str::<TokenEntityMetadata>(yaml)
            .expect("Failed to parse an empty YAML object into TokenEntityMetadata");
        assert_eq!(
            parsed,
            TokenEntityMetadata::builder()
                .entity_type_name("Jans::Access_token".into())
                .build(),
            "Expected empty YAML to be parsed into default TokenEntityMetadata"
        );

        // Test case: Parsing YAML with specified `user_id` and `role_mapping`
        let yaml = "
            user_id: 'sub'
            role_mapping: ''
            entity_type_name: Jans::Access_token
        ";
        let parsed = serde_yml::from_str::<TokenEntityMetadata>(yaml).expect(
            "Failed to parse YAML object with user_id and role_mapping into TokenEntityMetadata",
        );
        assert_eq!(
            parsed,
            TokenEntityMetadata::builder()
                .user_id(Some("sub".into()))
                .entity_type_name("Jans::Access_token".into())
                .build(),
            "Expected YAML with user_id and empty role_mapping to be parsed into \
             TokenEntityMetadata"
        );
    }
}
