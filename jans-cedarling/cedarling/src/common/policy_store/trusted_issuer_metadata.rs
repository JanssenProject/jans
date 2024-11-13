/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::parse_option_string;
use super::token_entity_metadata::TokenEntityMetadata;
use serde::Deserialize;

/// Represents metadata for a trusted issuer that can provide JWTs.
///
/// This struct includes information about the issuer, such as its name, a description,
/// and its OpenID configuration endpoint. It also contains metadata about different types
/// of tokens (access, ID, userinfo, and transaction tokens) issued by this issuer.
#[derive(Debug, Deserialize, PartialEq, Clone, Default)]
#[allow(dead_code)]
pub struct TrustedIssuerMetadata {
    /// The name of the trusted issuer.
    pub name: String,

    /// A brief description of the trusted issuer.
    pub description: String,

    /// This endpoint is used to obtain information about the issuer's capabilities.
    pub openid_configuration_endpoint: String,

    /// Metadata for access tokens issued by the trusted issuer.
    #[serde(default)]
    pub access_tokens: AccessTokenMetadata,

    /// Metadata for ID tokens issued by the trusted issuer.
    #[serde(default)]
    pub id_tokens: TokenEntityMetadata,

    /// Metadata for userinfo tokens issued by the trusted issuer.
    #[serde(default)]
    pub userinfo_tokens: TokenEntityMetadata,

    /// Metadata for transaction tokens issued by the trusted issuer.
    #[serde(default)]
    pub tx_tokens: TokenEntityMetadata,
}

/// Represents metadata related to access tokens issued by a trusted issuer.
///
/// This struct includes information on whether the access token is trusted, the principal
/// identifier, and additional entity metadata.
#[derive(Deserialize, Clone, PartialEq, Default, Debug)]
pub struct AccessTokenMetadata {
    /// Indicates if the access token is trusted.
    #[serde(default)]
    pub trusted: bool,
    #[serde(default, deserialize_with = "parse_option_string")]
    /// An optional string representing the principal identifier (e.g., `jti`).
    pub principal_identifier: Option<String>,
    /// Additional metadata associated with the access token.
    #[serde(default, flatten)]
    pub entity_metadata: TokenEntityMetadata,
}

#[cfg(test)]
mod test {
    use crate::common::policy_store::{
        token_entity_metadata::TokenEntityMetadata, trusted_issuer_metadata::AccessTokenMetadata,
    };

    use super::TrustedIssuerMetadata;
    use serde_json::json;

    #[test]
    fn can_parse_from_json() {
        let metadata_json = json!({
            "name": "Google",
            "description": "Consumer IDP",
            "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
            "access_tokens": {
                "trusted": true,
                "principal_identifier": "jti",
                "user_id": "",
                "role_mapping": "",
                "claim_mapping": {},
            },
            "id_tokens": {
                "user_id": "sub",
                "role_mapping": "role",
                "claim_mapping": {},
            },
            "userinfo_tokens": {
                "user_id": "",
                "role_mapping": "",
                "claim_mapping": {},
            },
        }).to_string();

        let parsed = serde_json::from_str::<TrustedIssuerMetadata>(&metadata_json)
            .expect("Should parse Trusted Issuer Metadata JSON");

        let expected = TrustedIssuerMetadata {
            name: "Google".to_string(),
            description: "Consumer IDP".to_string(),
            openid_configuration_endpoint:
                "https://accounts.google.com/.well-known/openid-configuration".to_string(),
            access_tokens: AccessTokenMetadata {
                trusted: true,
                principal_identifier: Some("jti".to_string()),
                entity_metadata: TokenEntityMetadata::default(),
            },
            id_tokens: TokenEntityMetadata {
                user_id: Some("sub".to_string()),
                role_mapping: Some("role".to_string()),
                claim_mapping: None,
            },
            userinfo_tokens: TokenEntityMetadata::default(),
            tx_tokens: TokenEntityMetadata::default(),
        };

        assert_eq!(
            parsed, expected,
            "Expected to parse Trusted Issuer Metadata to be parsed correctly from JSON: {:?}",
            parsed
        );
    }

    #[test]
    fn can_parse_from_yaml() {
        let metadata_yaml = "
            name: 'Google'
            description: 'Consumer IDP'
            openid_configuration_endpoint: 'https://accounts.google.com/.well-known/openid-configuration'
            access_tokens:
                trusted: true
                principal_identifier: jti
                user_id: ''
                role_mapping: ''
                claim_mapping: {}
            id_tokens:
                user_id: 'sub'
                role_mapping: 'role'
                claim_mapping: {}
            userinfo_tokens:
                user_id: ''
                role_mapping: ''
                claim_mapping: {}
        ";

        let parsed = serde_yml::from_str::<TrustedIssuerMetadata>(&metadata_yaml)
            .expect("Should parse Trusted Issuer Metadata JSON");

        let expected = TrustedIssuerMetadata {
            name: "Google".to_string(),
            description: "Consumer IDP".to_string(),
            openid_configuration_endpoint:
                "https://accounts.google.com/.well-known/openid-configuration".to_string(),
            access_tokens: AccessTokenMetadata {
                trusted: true,
                principal_identifier: Some("jti".to_string()),
                entity_metadata: TokenEntityMetadata::default(),
            },
            id_tokens: TokenEntityMetadata {
                user_id: Some("sub".to_string()),
                role_mapping: Some("role".to_string()),
                claim_mapping: None,
            },
            userinfo_tokens: TokenEntityMetadata::default(),
            tx_tokens: TokenEntityMetadata::default(),
        };

        assert_eq!(
            parsed, expected,
            "Expected to parse Trusted Issuer Metadata to be parsed correctly from YAML: {:?}",
            parsed
        );
    }
}
