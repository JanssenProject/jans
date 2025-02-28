// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::{HashMap, HashSet};

use super::claim_mapping::ClaimMappings;
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
    /// The Cedar entity name that represents this token
    pub entity_type_name: String,
    /// The Cedar entities that this token is an attribute of
    #[serde(default)]
    #[builder(default)]
    pub principal_mapping: HashSet<String>,
    /// An optional string representing the principal identifier (e.g., `jti`).
    #[serde(default = "default_token_id")]
    #[builder(default = default_token_id())]
    pub token_id: String,
    /// The claim used to create the user id
    #[serde(deserialize_with = "parse_option_string", default)]
    #[builder(default)]
    pub user_id: Option<String>,
    /// An optional string indicating the role mapping for the user.
    #[serde(deserialize_with = "parse_option_string", default)]
    #[builder(default)]
    pub role_mapping: Option<String>,
    #[serde(deserialize_with = "parse_option_string", default)]
    #[builder(default)]
    pub workload_id: Option<String>,
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
}

fn default_trusted() -> bool {
    true
}

fn default_token_id() -> String {
    const DEFAULT_TKN_ID: &str = "jti";
    DEFAULT_TKN_ID.to_string()
}

impl TokenEntityMetadata {
    /// Default access token Metadata
    pub fn access_token() -> Self {
        Self {
            trusted: true,
            token_id: default_token_id(),
            user_id: None,
            role_mapping: None,
            claim_mapping: HashMap::new().into(),
            required_claims: HashSet::from(["iss".into(), "exp".into(), "jti".into()]),
            entity_type_name: "Jans::Access_token".into(),
            principal_mapping: HashSet::from(["Jans::Workload".into()]),
            workload_id: Some("aud".into()),
        }
    }

    /// Default id token Metadata
    pub fn id_token() -> Self {
        Self {
            trusted: true,
            token_id: default_token_id(),
            user_id: Some("aud".into()),
            role_mapping: None,
            claim_mapping: HashMap::new().into(),
            required_claims: HashSet::from([
                "iss".into(),
                "sub".into(),
                "aud".into(),
                "exp".into(),
            ]),
            entity_type_name: "Jans::id_token".into(),
            principal_mapping: HashSet::from(["Jans::User".into()]),
            workload_id: None,
        }
    }

    /// Default userinfo token Metadata
    pub fn userinfo_token() -> Self {
        Self {
            trusted: true,
            token_id: default_token_id(),
            user_id: Some("aud".into()),
            role_mapping: None,
            claim_mapping: HashMap::new().into(),
            required_claims: HashSet::from([
                "iss".into(),
                "sub".into(),
                "aud".into(),
                "exp".into(),
            ]),
            entity_type_name: "Jans::Userinfo_token".into(),
            principal_mapping: HashSet::from(["Jans::User".into()]),
            workload_id: None,
        }
    }
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
