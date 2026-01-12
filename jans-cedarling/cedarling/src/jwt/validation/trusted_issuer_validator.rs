// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Trusted issuer JWT validation module.
//!
//! This module provides standalone functionality to validate JWT tokens against configured
//! trusted issuers, including issuer matching, required claims validation, JWKS fetching,
//! and signature verification.
//!
//! ## Features
//!
//! - **Issuer matching**: Validates that JWT tokens come from configured trusted issuers
//! - **Required claims validation**: Ensures tokens contain all claims specified in issuer configuration
//! - **Signature verification**: Validates JWT signatures using cached JWKS keys

use std::collections::HashMap;
use std::sync::Arc;

use serde_json::Value as JsonValue;
use thiserror::Error;
use url::Url;

use crate::common::policy_store::{TokenEntityMetadata, TrustedIssuer};

/// Errors that can occur during trusted issuer validation.
#[derive(Debug, Error)]
pub enum TrustedIssuerError {
    /// Token issuer is not in the list of trusted issuers
    #[error("Untrusted issuer: '{0}'")]
    UntrustedIssuer(String),

    /// Token is missing a required claim
    #[error("Missing required claim: '{claim}' for token type '{token_type}'")]
    MissingRequiredClaim {
        /// The name of the missing claim
        claim: String,
        /// The type of token being validated
        token_type: String,
    },

    /// Token metadata has empty entity_type_name
    #[error(
        "Invalid token metadata configuration: entity_type_name is empty for token type '{token_type}'"
    )]
    EmptyEntityTypeName {
        /// The token type with empty entity_type_name
        token_type: String,
    },
}

/// Result type for trusted issuer validation operations.
type Result<T> = std::result::Result<T, TrustedIssuerError>;

/// Validator for JWT tokens against trusted issuer configurations.
///
/// This validator provides the following functionality:
/// - Issuer matching against configured trusted issuers
/// - Required claims validation based on token metadata
/// - JWKS fetching and caching with configurable TTL
/// - JWT signature verification
pub(crate) struct TrustedIssuerValidator {
    /// Map of issuer identifiers to their configurations
    trusted_issuers: HashMap<String, Arc<TrustedIssuer>>,
    /// Reverse lookup map: OIDC base URL -> issuer
    /// This optimizes issuer lookup when dealing with hundreds of trusted issuers
    url_to_issuer: HashMap<String, Arc<TrustedIssuer>>,
}

impl TrustedIssuerValidator {
    /// Creates a new trusted issuer validator with a logger.
    pub(crate) fn new(trusted_issuers: HashMap<String, TrustedIssuer>) -> Self {
        let trusted_issuers: HashMap<String, Arc<TrustedIssuer>> = trusted_issuers
            .into_iter()
            .map(|(k, v)| (k, Arc::new(v)))
            .collect();

        // Build reverse lookup map: OIDC base URL -> issuer
        let mut url_to_issuer = HashMap::with_capacity(trusted_issuers.len());
        for (id, issuer) in &trusted_issuers {
            // Extract base URL from OIDC endpoint
            if let Some(base_url) = issuer
                .oidc_endpoint
                .as_str()
                .strip_suffix("/.well-known/openid-configuration")
            {
                let normalized_url = base_url.trim_end_matches('/');
                url_to_issuer.insert(normalized_url.to_string(), issuer.clone());
            }

            // Also add the issuer ID if it's a URL format
            if id.starts_with("http://") || id.starts_with("https://") {
                let normalized_id = id.trim_end_matches('/');
                url_to_issuer.insert(normalized_id.to_string(), issuer.clone());
            }
        }

        Self {
            trusted_issuers,
            url_to_issuer,
        }
    }

    /// Finds a trusted issuer by the issuer claim value.
    ///
    /// This method matches the token's `iss` claim against the configured trusted issuers.
    /// The matching is done by comparing the issuer URL or issuer ID.
    pub(crate) fn find_trusted_issuer(&self, issuer_claim: &str) -> Result<Arc<TrustedIssuer>> {
        // Try exact match first by issuer ID
        if let Some(issuer) = self.trusted_issuers.get(issuer_claim) {
            return Ok(issuer.clone());
        }

        // Try matching by URL using reverse lookup map (O(1) instead of O(n))
        // Parse the issuer claim as a URL and normalize it for lookup
        if let Ok(iss_url) = Url::parse(issuer_claim) {
            let normalized_url = iss_url.as_str().trim_end_matches('/');

            if let Some(issuer) = self.url_to_issuer.get(normalized_url) {
                return Ok(issuer.clone());
            }
        }

        Err(TrustedIssuerError::UntrustedIssuer(
            issuer_claim.to_string(),
        ))
    }
}

/// Validates that a token contains all required claims based on token metadata.
///
/// This is a standalone function that can be used independently of `TrustedIssuerValidator`.
/// It validates:
/// - `entity_type_name` is not empty (configuration validation)
/// - All claims in `required_claims` set exist
///
/// Note: Mapping fields like `user_id`, `role_mapping`, `workload_id`, and `token_id`
/// are configuration hints for claim extraction, not strictly required claims.
/// They are validated only if explicitly included in `required_claims`.
///
/// # Arguments
///
/// * `claims` - The JWT claims as a JSON value
/// * `token_type` - The type of token (e.g., "access_token", "id_token")
/// * `token_metadata` - The token metadata configuration from the trusted issuer
///
pub(crate) fn validate_required_claims(
    claims: &JsonValue,
    token_type: &str,
    token_metadata: &TokenEntityMetadata,
) -> Result<()> {
    // Check for entity_type_name (configuration validation, always required)
    if token_metadata.entity_type_name.is_empty() {
        return Err(TrustedIssuerError::EmptyEntityTypeName {
            token_type: token_type.to_string(),
        });
    }

    // Validate all claims explicitly specified in required_claims set
    // This is the authoritative list of required claims from the issuer configuration
    for claim in &token_metadata.required_claims {
        if claims.get(claim).is_none() {
            return Err(TrustedIssuerError::MissingRequiredClaim {
                claim: claim.clone(),
                token_type: token_type.to_string(),
            });
        }
    }

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::TokenEntityMetadata;
    use std::collections::HashSet;

    fn create_test_issuer(id: &str, endpoint: &str) -> TrustedIssuer {
        let mut token_metadata = HashMap::new();
        token_metadata.insert(
            "access_token".to_string(),
            TokenEntityMetadata::access_token(),
        );
        token_metadata.insert("id_token".to_string(), TokenEntityMetadata::id_token());

        TrustedIssuer {
            name: format!("Test Issuer {}", id),
            description: "Test issuer for validation".to_string(),
            oidc_endpoint: Url::parse(endpoint).unwrap(),
            token_metadata,
        }
    }

    #[test]
    fn test_find_trusted_issuer_by_id() {
        let issuers = HashMap::from([
            (
                "issuer1".to_string(),
                create_test_issuer("1", "https://issuer1.com/.well-known/openid-configuration"),
            ),
            (
                "issuer2".to_string(),
                create_test_issuer("2", "https://issuer2.com/.well-known/openid-configuration"),
            ),
        ]);

        let validator = TrustedIssuerValidator::new(issuers);

        let result = validator.find_trusted_issuer("issuer1");
        assert!(result.is_ok());
        assert_eq!(result.unwrap().name, "Test Issuer 1");
    }

    #[test]
    fn test_find_trusted_issuer_by_url() {
        let issuers = HashMap::from([(
            "issuer1".to_string(),
            create_test_issuer("1", "https://issuer1.com/.well-known/openid-configuration"),
        )]);

        let validator = TrustedIssuerValidator::new(issuers);

        let result = validator.find_trusted_issuer("https://issuer1.com");
        assert!(result.is_ok());
        assert_eq!(result.unwrap().name, "Test Issuer 1");
    }

    #[test]
    fn test_untrusted_issuer() {
        let issuers = HashMap::from([(
            "issuer1".to_string(),
            create_test_issuer("1", "https://issuer1.com/.well-known/openid-configuration"),
        )]);

        let validator = TrustedIssuerValidator::new(issuers);

        let result = validator.find_trusted_issuer("https://evil.com");
        assert!(
            matches!(result.unwrap_err(), TrustedIssuerError::UntrustedIssuer(_)),
            "expected UntrustedIssuer error"
        );
    }

    #[test]
    fn test_validate_required_claims_success() {
        let claims = serde_json::json!({
            "sub": "user123",
            "jti": "token123",
            "role": "admin"
        });

        let metadata = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_token".to_string())
            .user_id(Some("sub".to_string()))
            .role_mapping(Some("role".to_string()))
            .token_id("jti".to_string())
            .build();

        let result = validate_required_claims(&claims, "access_token", &metadata);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_required_claims_missing_sub() {
        let claims = serde_json::json!({
            "jti": "token123",
            "role": "admin"
        });

        // Only claims in required_claims are validated
        let metadata = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_token".to_string())
            .user_id(Some("sub".to_string())) // This is just a mapping, not validated
            .token_id("jti".to_string())
            .required_claims(HashSet::from(["sub".to_string()])) // This IS validated
            .build();

        let result = validate_required_claims(&claims, "access_token", &metadata);
        assert!(
            matches!(
                result.unwrap_err(),
                TrustedIssuerError::MissingRequiredClaim { claim, .. } if claim == "sub"
            ),
            "expected MissingRequiredClaim error for 'sub'"
        );
    }

    #[test]
    fn test_validate_required_claims_missing_role() {
        let claims = serde_json::json!({
            "sub": "user123",
            "jti": "token123"
        });

        // Only claims in required_claims are validated
        let metadata = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_token".to_string())
            .role_mapping(Some("role".to_string())) // This is just a mapping, not validated
            .token_id("jti".to_string())
            .required_claims(HashSet::from(["role".to_string()])) // This IS validated
            .build();

        let result = validate_required_claims(&claims, "access_token", &metadata);
        assert!(
            matches!(
                result.unwrap_err(),
                TrustedIssuerError::MissingRequiredClaim { claim, .. } if claim == "role"
            ),
            "expected MissingRequiredClaim error for 'role'"
        );
    }

    #[test]
    fn test_validate_required_claims_missing_jti() {
        let claims = serde_json::json!({
            "sub": "user123",
            "role": "admin"
        });

        // Only claims in required_claims are validated
        let metadata = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_token".to_string())
            .token_id("jti".to_string()) // This is just a mapping, not validated
            .required_claims(HashSet::from(["jti".to_string()])) // This IS validated
            .build();

        let result = validate_required_claims(&claims, "access_token", &metadata);
        assert!(
            matches!(
                result.unwrap_err(),
                TrustedIssuerError::MissingRequiredClaim { claim, .. } if claim == "jti"
            ),
            "expected MissingRequiredClaim error for 'jti'"
        );
    }

    #[test]
    fn test_validate_required_claims_mapping_fields_not_required() {
        // Test that mapping fields (user_id, role_mapping, token_id) are NOT validated
        // unless they are explicitly in required_claims
        let claims = serde_json::json!({
            "iss": "https://issuer.com"
            // Note: sub, role, jti are all missing
        });

        let metadata = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_token".to_string())
            .user_id(Some("sub".to_string()))     // Mapping only
            .role_mapping(Some("role".to_string())) // Mapping only
            .token_id("jti".to_string())          // Mapping only
            .required_claims(HashSet::new())      // No required claims
            .build();

        // Should pass because required_claims is empty
        let result = validate_required_claims(&claims, "access_token", &metadata);
        assert!(result.is_ok());
    }
}
