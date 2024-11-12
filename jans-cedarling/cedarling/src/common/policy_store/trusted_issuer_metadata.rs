use super::token_entity_metadata::TokenEntityMetadata;
use serde::Deserialize;

/// Represents a trusted issuer that can provide JWTs.
///
/// This struct includes the issuer's name, description, and the OpenID configuration endpoint
/// for discovering issuer-related information.
#[derive(Debug, Deserialize, PartialEq)]
#[allow(dead_code)]
pub struct TrustedIssuerMetadata {
    /// The name of the trusted issuer.
    pub name: String,

    /// A brief description of the trusted issuer.
    pub description: String,

    /// This endpoint is used to obtain information about the issuer's capabilities.
    pub openid_configuration_endpoint: String,

    /// Token Entity Metadata for Access Tokens.
    #[serde(default)]
    pub access_tokens: TokenEntityMetadata,

    /// Token Entity Metadata for ID Tokens.
    #[serde(default)]
    pub id_tokens: TokenEntityMetadata,

    /// Token Entity Metadata for Userinfo Tokens.
    #[serde(default)]
    pub userinfo_tokens: TokenEntityMetadata,

    /// Token Entity Metadata for Transaction Tokens.
    #[serde(default)]
    pub tx_tokens: TokenEntityMetadata,
}

#[cfg(test)]
mod test {
    use crate::common::policy_store::token_entity_metadata::TokenEntityMetadata;

    use super::TrustedIssuerMetadata;
    use serde_json::json;

    #[test]
    fn can_parse_from_json() {
        let metadata_json = json!({
            "name": "Google",
            "description": "Consumer IDP",
            "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
            "access_tokens": {
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
            access_tokens: TokenEntityMetadata::default(),
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
            "Expected to parse Trusted Issuer Metadata to be parsed correctly: {:?}",
            parsed
        );
    }
}
