// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Validation of a JWT against its trusted issuer's per-token configuration.

use serde_json::Value as JsonValue;
use thiserror::Error;

use crate::common::policy_store::TokenEntityMetadata;

/// Errors that can occur during trusted issuer validation.
#[derive(Debug, Error)]
pub(crate) enum TrustedIssuerError {
    /// Token is missing a required claim
    #[error("Missing required claim: '{claim}' for token type '{token_type}'")]
    MissingRequiredClaim {
        /// The name of the missing claim
        claim: String,
        /// The type of token being validated
        token_type: String,
    },
}

/// Result type for trusted issuer validation operations.
type Result<T> = std::result::Result<T, TrustedIssuerError>;

/// Validates that a token contains all required claims based on token metadata.
///
/// It validates:
/// - All claims in `required_claims` set exist
///
/// Note: Mapping fields like `user_id`, `role_mapping`, and `token_id`
/// are configuration hints for claim extraction, not strictly required claims.
/// They are validated only if explicitly included in `required_claims`.
///
/// # Arguments
///
/// * `claims` - The JWT claims as a JSON value
/// * `token_type` - The token's Cedar entity type (e.g., "`Jans::Access_token`")
/// * `token_metadata` - The token metadata configuration from the trusted issuer
///
pub(crate) fn validate_required_claims(
    claims: &JsonValue,
    token_type: &str,
    token_metadata: &TokenEntityMetadata,
) -> Result<()> {
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

    #[test]
    fn test_validate_required_claims_success() {
        let claims = serde_json::json!({
            "sub": "user123",
            "jti": "token123",
            "role": "admin"
        });

        let metadata = TokenEntityMetadata::builder()
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
            .token_id("jti".to_string())          // Mapping only
            .required_claims(HashSet::new())      // No required claims
            .build();

        // Should pass because required_claims is empty
        let result = validate_required_claims(&claims, "access_token", &metadata);
        assert!(result.is_ok());
    }
}
