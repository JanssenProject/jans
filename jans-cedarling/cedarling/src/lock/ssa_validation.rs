// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Software Statement Assertion (SSA) JWT Validation
//! 
//! This module provides comprehensive validation for Software Statement Assertion (SSA) JWTs
//! used in Dynamic Client Registration (DCR) with Identity Providers.
//! 
//! ## Overview
//! 
//! SSA JWTs are signed tokens that contain claims about software identity, permissions,
//! and configuration. They provide a secure way to register clients with Identity Providers
//! by proving the software's identity and authorized capabilities.
//! 
//! ## SSA JWT Structure
//! 
//! An SSA JWT contains the following required claims:
//! 
//! **Claims defined by RFC 7591:**
//! - **software_id**: Unique identifier for the software
//! - **grant_types**: Array of OAuth2 grant types the software can use
//! - **iss**: Issuer of the SSA JWT
//! - **exp**: Expiration time (Unix timestamp)
//! - **iat**: Issued at time (Unix timestamp)
//! - **jti**: JWT ID (unique identifier)
//! 
//! **Additional custom claims required by Cedarling:**
//! - **org_id**: Organization identifier
//! - **software_roles**: Array of roles/permissions for the software
//! 
//! ## Validation Process
//! 
//! The validation process follows these steps:
//! 
//! 1. **JWT Decoding**: Parse and decode the JWT structure
//! 2. **Structure Validation**: Verify required claims are present and have correct types
//! 3. **JWKS Fetching**: Retrieve JSON Web Key Set from the IDP's JWKS endpoint
//! 4. **Key Discovery**: Find the appropriate key using the JWT's `kid` header
//! 5. **Signature Verification**: Validate the JWT signature using the public key
//! 6. **Claims Validation**: Check expiration and other time-based claims
//! 
//! ## Supported Algorithms
//! 
//! The module supports the following JWT signing algorithms:
//! 
//! - **HMAC**: HS256, HS384, HS512 (for symmetric keys)
//! - **RSA**: RS256, RS384, RS512 (for asymmetric keys)
//! 
//! ## Error Handling
//! 
//! The module provides detailed error types for different validation failures:
//! 
//! - **DecodeJwt**: JWT decoding failed
//! - **MissingRequiredClaims**: Required claims are missing
//! - **InvalidGrantTypes**: Grant types claim is not an array
//! - **InvalidSoftwareRoles**: Software roles claim is not an array
//! - **InvalidExpirationTime**: Expiration time is not a number
//! - **InvalidIssuedAtTime**: Issued at time is not a number
//! - **HttpClientError**: HTTP client initialization failed
//! - **JwksFetchError**: Failed to fetch JWKS from IDP
//! - **JwksParseError**: Failed to parse JWKS response
//! - **MissingKeyId**: JWT header missing key ID
//! - **KeyNotFound**: Key not found in JWKS
//! - **KeyDecodeError**: Failed to decode key data
//! - **InvalidKeyFormat**: Key format is invalid
//! - **UnsupportedAlgorithm**: JWT algorithm not supported
//! - **JwtError**: JWT validation error
//! 
//! ## Integration with Lock Server
//! 
//! This module is used by the Lock Server integration to validate SSA JWTs
//! before performing Dynamic Client Registration. The validated SSA JWT
//! is then included in the DCR request to the Identity Provider.

use crate::jwt::{decode_jwt, DecodeJwtError, DecodedJwt};
use base64::Engine;
use jsonwebtoken::{self as jwt, Algorithm, DecodingKey, Validation};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashSet;
use thiserror::Error;

/// Configuration for SSA JWT validation.
///
/// This struct allows customization of validation behavior including
/// required claims, allowed algorithms, and validation behavior.
#[derive(Debug, Clone)]
pub struct SsaValidationConfig {
    /// Set of required claims that must be present in the SSA JWT.
    /// Default includes: software_id, grant_types, iss, exp, iat, jti (RFC 7591/RFC 7519)
    /// plus org_id, software_roles (Cedarling custom requirements)
    pub required_claims: HashSet<String>,
    
    /// Set of allowed signature algorithms.
    /// If empty, all supported algorithms are allowed.
    pub allowed_algorithms: HashSet<Algorithm>,
    
    /// Whether to validate the expiration time (exp claim).
    /// Default: true
    pub validate_expiration: bool,
    
    /// Whether to validate the issued-at time (iat claim).
    /// Default: true
    pub validate_issued_at: bool,
    
    /// Whether to validate the not-before time (nbf claim).
    /// Default: false
    pub validate_not_before: bool,
    
    /// Whether to validate the audience (aud claim).
    /// Default: false
    pub validate_audience: bool,
    
    /// Whether to accept invalid SSL certificates when fetching JWKS.
    /// Default: false
    pub accept_invalid_certs: bool,
    
    /// Whether to validate that grant_types is an array.
    /// Default: true
    pub validate_grant_types_array: bool,
    
    /// Whether to validate that software_roles is an array.
    /// Default: true
    pub validate_software_roles_array: bool,
}

impl Default for SsaValidationConfig {
    fn default() -> Self {
        Self {
            required_claims: HashSet::from([
                "software_id".to_string(),
                "grant_types".to_string(),
                "org_id".to_string(),
                "iss".to_string(),
                "software_roles".to_string(),
                "exp".to_string(),
                "iat".to_string(),
                "jti".to_string(),
            ]),
            allowed_algorithms: HashSet::new(), // Empty means all supported
            validate_expiration: true,
            validate_issued_at: true,
            validate_not_before: false,
            validate_audience: false,
            accept_invalid_certs: false,
            validate_grant_types_array: true,
            validate_software_roles_array: true,
        }
    }
}

/// SSA JWT claims structure
/// 
/// This struct includes both RFC 7591 defined claims and custom Cedarling requirements.
#[derive(Debug, Deserialize, Serialize, PartialEq)]
pub struct SsaClaims {
    /// Software identifier (RFC 7591)
    pub software_id: String,
    /// Grant types the software is authorized to use (RFC 7591)
    pub grant_types: Vec<String>,
    /// Organization identifier (Cedarling custom requirement)
    pub org_id: String,
    /// Issuer of the SSA (RFC 7591)
    pub iss: String,
    /// Software roles/permissions (Cedarling custom requirement)
    pub software_roles: Vec<String>,
    /// Expiration time (RFC 7519 standard JWT claim)
    pub exp: u64,
    /// Issued at time (RFC 7519 standard JWT claim)
    pub iat: u64,
    /// JWT ID (RFC 7519 standard JWT claim)
    pub jti: String,
    /// Additional custom claims
    #[serde(flatten)]
    pub additional_claims: Value,
}

/// Validates an SSA JWT with default configuration.
///
/// This is a convenience function that uses the default validation settings.
/// For custom validation options, use `validate_ssa_jwt_with_config`.
pub async fn validate_ssa_jwt(
    ssa_jwt: &str,
    jwks_uri: &str,
    accept_invalid_certs: bool,
) -> Result<SsaClaims, SsaValidationError> {
    let config = SsaValidationConfig {
        accept_invalid_certs,
        ..Default::default()
    };
    validate_ssa_jwt_with_config(ssa_jwt, jwks_uri, &config).await
}

/// Validates an SSA JWT with custom configuration.
///
/// This function allows full customization of validation behavior including
/// required claims, allowed algorithms, and validation options.
pub async fn validate_ssa_jwt_with_config(
    ssa_jwt: &str,
    jwks_uri: &str,
    config: &SsaValidationConfig,
) -> Result<SsaClaims, SsaValidationError> {
    // First decode the JWT to get basic information (header and claims without signature validation)
    // This is needed to extract the algorithm and kid for key selection
    let decoded_jwt = decode_jwt(ssa_jwt)?;
    
    // Validate the JWT structure and basic claims
    validate_ssa_structure_with_config(&decoded_jwt, config)?;
    
    // Check if the algorithm is allowed by configuration
    if !config.allowed_algorithms.is_empty() && !config.allowed_algorithms.contains(&decoded_jwt.header.alg) {
        return Err(SsaValidationError::AlgorithmNotAllowed(decoded_jwt.header.alg));
    }
    
    // Fetch JWKS from the issuer
    let jwks = fetch_jwks(jwks_uri, config.accept_invalid_certs).await?;
    
    // Find the appropriate key for validation
    let decoding_key = find_decoding_key(&decoded_jwt, &jwks)?;
    
    // Second decode: Validate the JWT signature and claims
    // This is necessary because the first decode only extracts data without cryptographic validation
    let claims = validate_ssa_signature_and_claims_with_config(ssa_jwt, &decoding_key, decoded_jwt.header.alg, config)?;
    
    Ok(claims)
}

/// Validates the basic structure and required claims of an SSA JWT with custom configuration
pub fn validate_ssa_structure_with_config(
    decoded_jwt: &DecodedJwt,
    config: &SsaValidationConfig,
) -> Result<(), SsaValidationError> {
    // Check if required claims are present
    let claims = &decoded_jwt.claims.inner;
    
    let mut missing_claims = Vec::new();
    
    for claim in config.required_claims.iter() {
        if claims.get(claim).is_none() {
            missing_claims.push(claim.clone());
        }
    }
    
    if !missing_claims.is_empty() {
        return Err(SsaValidationError::MissingRequiredClaims(missing_claims));
    }
    
    // Validate grant_types is an array (if enabled)
    if config.validate_grant_types_array
        && let Some(grant_types) = claims.get("grant_types")
            && !grant_types.is_array() {
                return Err(SsaValidationError::InvalidGrantTypes);
            }
    
    // Validate software_roles is an array (if enabled)
    if config.validate_software_roles_array
        && let Some(software_roles) = claims.get("software_roles")
            && !software_roles.is_array() {
                return Err(SsaValidationError::InvalidSoftwareRoles);
            }
    
    // Validate exp is a number (if enabled)
    if config.validate_expiration
        && let Some(exp) = claims.get("exp")
            && !exp.is_number() {
                return Err(SsaValidationError::InvalidExpirationTime);
            }
    
    // Validate iat is a number (if enabled)
    if config.validate_issued_at
        && let Some(iat) = claims.get("iat")
            && !iat.is_number() {
                return Err(SsaValidationError::InvalidIssuedAtTime);
            }
    
    Ok(())
}

/// Fetches JWKS from the specified URI
async fn fetch_jwks(jwks_uri: &str, accept_invalid_certs: bool) -> Result<Value, SsaValidationError> {
    use super::init_http_client;
    
    let client = init_http_client(None, accept_invalid_certs)
        .map_err(SsaValidationError::HttpClientError)?;
    
    let response = client
        .get(jwks_uri)
        .send()
        .await
        .map_err(SsaValidationError::JwksFetchError)?;
    
    if !response.status().is_success() {
        return Err(SsaValidationError::JwksFetchError(
            response.error_for_status().unwrap_err()
        ));
    }
    
    let jwks: Value = response
        .json()
        .await
        .map_err(SsaValidationError::JwksParseError)?;
    
    Ok(jwks)
}

/// Finds the appropriate decoding key from JWKS
fn find_decoding_key(
    decoded_jwt: &DecodedJwt,
    jwks: &Value,
) -> Result<DecodingKey, SsaValidationError> {
    let kid = decoded_jwt.header.kid.as_ref()
        .ok_or(SsaValidationError::MissingKeyId)?;
    
    if let Some(keys) = jwks.get("keys").and_then(|k| k.as_array()) {
        for key in keys {
            if let Some(key_kid) = key.get("kid").and_then(|k| k.as_str())
                && key_kid == kid {
                    // Validate that the JWK's algorithm matches the JWT header's algorithm
                    if let Some(jwk_alg) = key.get("alg").and_then(|a| a.as_str()) {
                        let jwt_alg_str = match decoded_jwt.header.alg {
                            Algorithm::HS256 => "HS256",
                            Algorithm::HS384 => "HS384",
                            Algorithm::HS512 => "HS512",
                            Algorithm::RS256 => "RS256",
                            Algorithm::RS384 => "RS384",
                            Algorithm::RS512 => "RS512",
                            Algorithm::ES256 => "ES256",
                            Algorithm::ES384 => "ES384",
                            Algorithm::PS256 => "PS256",
                            Algorithm::PS384 => "PS384",
                            Algorithm::PS512 => "PS512",
                            Algorithm::EdDSA => "EdDSA",
                        };
                        
                        if jwk_alg != jwt_alg_str {
                            return Err(SsaValidationError::AlgorithmMismatch {
                                jwt_alg: jwt_alg_str.to_string(),
                                jwk_alg: jwk_alg.to_string(),
                            });
                        }
                    }
                    
                    return create_decoding_key(key, &decoded_jwt.header.alg);
                }
        }
    }
    
    Err(SsaValidationError::KeyNotFound(kid.clone()))
}

/// Creates a decoding key from JWK
fn create_decoding_key(jwk: &Value, algorithm: &Algorithm) -> Result<DecodingKey, SsaValidationError> {
    match algorithm {
        Algorithm::HS256 | Algorithm::HS384 | Algorithm::HS512 => {
            // For HMAC algorithms, we need the k parameter
            if let Some(k) = jwk.get("k").and_then(|k| k.as_str()) {
                let key_data = base64::engine::general_purpose::URL_SAFE_NO_PAD
                    .decode(k)
                    .map_err(SsaValidationError::KeyDecodeError)?;
                Ok(DecodingKey::from_secret(&key_data))
            } else {
                Err(SsaValidationError::InvalidKeyFormat)
            }
        }
        Algorithm::RS256 | Algorithm::RS384 | Algorithm::RS512 => {
            // For RSA algorithms, we need the n and e parameters
            if let (Some(n), Some(e)) = (
                jwk.get("n").and_then(|n| n.as_str()),
                jwk.get("e").and_then(|e| e.as_str())
            ) {
                // The from_rsa_components method expects base64url-encoded strings
                Ok(DecodingKey::from_rsa_components(n, e)
                    .map_err(SsaValidationError::JwtError)?)
            } else {
                Err(SsaValidationError::InvalidKeyFormat)
            }
        }
        _ => Err(SsaValidationError::UnsupportedAlgorithm),
    }
}

/// Validates the SSA JWT signature and claims with custom configuration
fn validate_ssa_signature_and_claims_with_config(
    ssa_jwt: &str,
    decoding_key: &DecodingKey,
    algorithm: Algorithm,
    config: &SsaValidationConfig,
) -> Result<SsaClaims, SsaValidationError> {
    // Validate the signature using the jsonwebtoken library
    let mut validation = Validation::new(algorithm);
    validation.validate_exp = config.validate_expiration;
    validation.validate_nbf = config.validate_not_before;
    validation.validate_aud = config.validate_audience;
    validation.required_spec_claims.clear();
    
    // We need to validate the signature, but we can reuse the already decoded claims
    // The jsonwebtoken library will validate the signature and return the claims
    let token_data = jwt::decode::<SsaClaims>(ssa_jwt, decoding_key, &validation)
        .map_err(SsaValidationError::JwtError)?;
    
    Ok(token_data.claims)
}

/// Errors that can occur during SSA JWT validation
#[derive(Debug, Error)]
pub enum SsaValidationError {
    /// Failed to decode the JWT structure
    #[error("failed to decode JWT: {0}")]
    DecodeJwt(#[from] DecodeJwtError),
    
    /// Missing required claims in the SSA JWT
    #[error("missing required claims: {0:?}")]
    MissingRequiredClaims(Vec<String>),
    
    /// Grant types field is not an array
    #[error("grant_types must be an array")]
    InvalidGrantTypes,
    
    /// Software roles field is not an array
    #[error("software_roles must be an array")]
    InvalidSoftwareRoles,
    
    /// Expiration time is not a valid number
    #[error("exp must be a number")]
    InvalidExpirationTime,
    
    /// Issued at time is not a valid number
    #[error("iat must be a number")]
    InvalidIssuedAtTime,
    
    /// Failed to initialize HTTP client for JWKS fetching
    #[error("failed to initialize HTTP client: {0}")]
    HttpClientError(reqwest::Error),
    
    /// Failed to fetch JWKS due to network or HTTP errors
    #[error("failed to fetch JWKS (network/HTTP error): {0}")]
    JwksFetchError(reqwest::Error),
    
    /// Failed to parse JWKS JSON response
    #[error("failed to parse JWKS (JSON parsing error): {0}")]
    JwksParseError(reqwest::Error),
    
    /// Missing key ID (kid) in JWT header
    #[error("missing key ID (kid) in JWT header")]
    MissingKeyId,
    
    /// Key not found in JWKS for the given key ID
    #[error("key not found in JWKS: {0}")]
    KeyNotFound(String),
    
    /// Failed to decode the key from base64
    #[error("failed to decode key: {0}")]
    KeyDecodeError(#[from] base64::DecodeError),
    
    /// Invalid key format for the algorithm
    #[error("invalid key format")]
    InvalidKeyFormat,
    
    /// Algorithm is not supported
    #[error("unsupported algorithm")]
    UnsupportedAlgorithm,
    
    /// Algorithm is not allowed by configuration
    #[error("algorithm not allowed by configuration: {0:?}")]
    AlgorithmNotAllowed(Algorithm),
    
    /// Algorithm mismatch between JWT header and JWK
    #[error("algorithm mismatch: JWT header specifies {jwt_alg:?} but JWK specifies {jwk_alg:?}")]
    AlgorithmMismatch { 
        /// Algorithm specified in JWT header
        jwt_alg: String, 
        /// Algorithm specified in JWK
        jwk_alg: String 
    },
    
    /// JWT validation error (signature, claims, etc.)
    #[error("JWT validation error: {0}")]
    JwtError(#[from] jwt::errors::Error),
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;
    
    #[test]
    fn test_validate_ssa_structure_valid() {
        let claims = json!({
            "software_id": "test_software",
            "grant_types": ["client_credentials"],
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": ["cedarling"],
            "exp": 1735689600,
            "iat": 1735603200,
            "jti": "test-jti-123"
        });
        
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: claims },
        };
        
        let config = SsaValidationConfig::default();
        let result = validate_ssa_structure_with_config(&decoded_jwt, &config);
        assert!(result.is_ok());
    }
    
    #[test]
    fn test_validate_ssa_structure_missing_claims() {
        let claims = json!({
            "software_id": "test_software",
            "grant_types": ["client_credentials"],
            // Missing org_id, iss, software_roles, exp, iat, jti
        });
        
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: claims },
        };
        
        let config = SsaValidationConfig::default();
        let result = validate_ssa_structure_with_config(&decoded_jwt, &config);
        assert!(matches!(result, Err(SsaValidationError::MissingRequiredClaims(_))));
    }
    
    #[test]
    fn test_validate_ssa_structure_invalid_grant_types() {
        let claims = json!({
            "software_id": "test_software",
            "grant_types": "client_credentials", // Should be array
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": ["cedarling"],
            "exp": 1735689600,
            "iat": 1735603200,
            "jti": "test-jti-123"
        });
        
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: claims },
        };
        
        let config = SsaValidationConfig::default();
        let result = validate_ssa_structure_with_config(&decoded_jwt, &config);
        assert!(matches!(result, Err(SsaValidationError::InvalidGrantTypes)));
    }
    
    #[test]
    fn test_algorithm_mismatch_detection() {
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: json!({}) },
        };
        
        // JWK with mismatched algorithm (HS256 instead of RS256)
        let jwks = json!({
            "keys": [
                {
                    "kid": "test-kid",
                    "alg": "HS256",
                    "k": "dGVzdC1rZXk=" // base64url encoded test key
                }
            ]
        });
        
        let result = find_decoding_key(&decoded_jwt, &jwks);
        assert!(matches!(result, Err(SsaValidationError::AlgorithmMismatch { jwt_alg, jwk_alg }) if jwt_alg == "RS256" && jwk_alg == "HS256"));
    }
    
    #[test]
    fn test_algorithm_match_success() {
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: json!({}) },
        };
        
        // JWK with matching algorithm
        let jwks = json!({
            "keys": [
                {
                    "kid": "test-kid",
                    "alg": "RS256",
                    "n": "test-n",
                    "e": "AQAB"
                }
            ]
        });
        
        let result = find_decoding_key(&decoded_jwt, &jwks);
        // Should not fail with AlgorithmMismatch, but may fail with other errors due to invalid key data
        assert!(!matches!(result, Err(SsaValidationError::AlgorithmMismatch { .. })));
    }
    
    #[test]
    fn test_validate_ssa_structure_invalid_exp_type() {
        let claims = json!({
            "software_id": "test_software",
            "grant_types": ["client_credentials"],
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": ["cedarling"],
            "exp": "not_a_number", // Should be number
            "iat": 1735603200,
            "jti": "test-jti-123"
        });
        
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: claims },
        };
        
        let result = validate_ssa_structure_with_config(&decoded_jwt, &SsaValidationConfig::default());
        assert!(matches!(result, Err(SsaValidationError::InvalidExpirationTime)));
    }
    
    #[test]
    fn test_validate_ssa_structure_invalid_iat_type() {
        let claims = json!({
            "software_id": "test_software",
            "grant_types": ["client_credentials"],
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": ["cedarling"],
            "exp": 1735689600,
            "iat": ["not", "a", "number"], // Should be number
            "jti": "test-jti-123"
        });
        
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: claims },
        };
        
        let result = validate_ssa_structure_with_config(&decoded_jwt, &SsaValidationConfig::default());
        assert!(matches!(result, Err(SsaValidationError::InvalidIssuedAtTime)));
    }
    
    #[test]
    fn test_validate_ssa_structure_invalid_software_roles() {
        let claims = json!({
            "software_id": "test_software",
            "grant_types": ["client_credentials"],
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": "cedarling", // Should be array
            "exp": 1735689600,
            "iat": 1735603200,
            "jti": "test-jti-123"
        });
        
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: claims },
        };
        
        let result = validate_ssa_structure_with_config(&decoded_jwt, &SsaValidationConfig::default());
        assert!(matches!(result, Err(SsaValidationError::InvalidSoftwareRoles)));
    }
    
    #[test]
    fn test_create_decoding_key_rsa_missing_n() {
        let jwk = json!({
            "kid": "test-kid",
            "alg": "RS256",
            "e": "AQAB"
            // Missing "n" parameter
        });
        
        let result = create_decoding_key(&jwk, &Algorithm::RS256);
        assert!(matches!(result, Err(SsaValidationError::InvalidKeyFormat)));
    }
    
    #[test]
    fn test_create_decoding_key_rsa_missing_e() {
        let jwk = json!({
            "kid": "test-kid",
            "alg": "RS256",
            "n": "test-n"
            // Missing "e" parameter
        });
        
        let result = create_decoding_key(&jwk, &Algorithm::RS256);
        assert!(matches!(result, Err(SsaValidationError::InvalidKeyFormat)));
    }
    
    #[test]
    fn test_create_decoding_key_hmac_missing_k() {
        let jwk = json!({
            "kid": "test-kid",
            "alg": "HS256"
            // Missing "k" parameter
        });
        
        let result = create_decoding_key(&jwk, &Algorithm::HS256);
        assert!(matches!(result, Err(SsaValidationError::InvalidKeyFormat)));
    }
    
    #[test]
    fn test_create_decoding_key_unsupported_algorithm() {
        let jwk = json!({
            "kid": "test-kid",
            "alg": "ES256",
            "x": "test-x",
            "y": "test-y"
        });
        
        let result = create_decoding_key(&jwk, &Algorithm::ES256);
        assert!(matches!(result, Err(SsaValidationError::UnsupportedAlgorithm)));
    }
    
    #[test]
    fn test_find_decoding_key_missing_kid() {
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: None, // Missing kid
            },
            claims: crate::jwt::DecodedJwtClaims { inner: json!({}) },
        };
        
        let jwks = json!({
            "keys": [
                {
                    "kid": "test-kid",
                    "alg": "RS256",
                    "n": "test-n",
                    "e": "AQAB"
                }
            ]
        });
        
        let result = find_decoding_key(&decoded_jwt, &jwks);
        assert!(matches!(result, Err(SsaValidationError::MissingKeyId)));
    }
    
    #[test]
    fn test_find_decoding_key_kid_not_found() {
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("different-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: json!({}) },
        };
        
        let jwks = json!({
            "keys": [
                {
                    "kid": "test-kid",
                    "alg": "RS256",
                    "n": "test-n",
                    "e": "AQAB"
                }
            ]
        });
        
        let result = find_decoding_key(&decoded_jwt, &jwks);
        assert!(matches!(result, Err(SsaValidationError::KeyNotFound(kid)) if kid == "different-kid"));
    }
    
    #[test]
    fn test_find_decoding_key_empty_keys_array() {
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: json!({}) },
        };
        
        let jwks = json!({
            "keys": [] // Empty keys array
        });
        
        let result = find_decoding_key(&decoded_jwt, &jwks);
        assert!(matches!(result, Err(SsaValidationError::KeyNotFound(kid)) if kid == "test-kid"));
    }
    
    #[test]
    fn test_find_decoding_key_missing_keys_field() {
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: json!({}) },
        };
        
        let jwks = json!({
            // Missing "keys" field entirely
        });
        
        let result = find_decoding_key(&decoded_jwt, &jwks);
        assert!(matches!(result, Err(SsaValidationError::KeyNotFound(kid)) if kid == "test-kid"));
    }
    
    #[test]
    fn test_config_default() {
        let config = SsaValidationConfig::default();
        assert!(config.validate_expiration);
        assert!(config.validate_issued_at);
        assert!(!config.validate_not_before);
        assert!(!config.validate_audience);
        assert!(!config.accept_invalid_certs);
        assert!(config.validate_grant_types_array);
        assert!(config.validate_software_roles_array);
        assert!(config.allowed_algorithms.is_empty()); // Empty means all allowed
        assert!(config.required_claims.contains("software_id"));
        assert!(config.required_claims.contains("exp"));
    }
    
    #[test]
    fn test_validate_ssa_structure_with_config_skip_exp() {
        let claims = json!({
            "software_id": "test_software",
            "grant_types": ["client_credentials"],
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": ["cedarling"],
            // Missing exp and iat
            "jti": "test-jti-123"
        });
        
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: claims },
        };
        
        let mut config = SsaValidationConfig::default();
        config.required_claims.remove("exp");
        config.required_claims.remove("iat");
        config.validate_expiration = false;
        config.validate_issued_at = false;
        
        let result = validate_ssa_structure_with_config(&decoded_jwt, &config);
        assert!(result.is_ok());
    }
    
    #[test]
    fn test_validate_ssa_structure_with_config_skip_array_validation() {
        let claims = json!({
            "software_id": "test_software",
            "grant_types": "client_credentials", // Should be array but validation disabled
            "org_id": "test_org",
            "iss": "https://test.issuer.com",
            "software_roles": "cedarling", // Should be array but validation disabled
            "exp": 1735689600,
            "iat": 1735603200,
            "jti": "test-jti-123"
        });
        
        let decoded_jwt = DecodedJwt {
            header: crate::jwt::DecodedJwtHeader {
                typ: Some("JWT".to_string()),
                alg: Algorithm::RS256,
                cty: None,
                kid: Some("test-kid".to_string()),
            },
            claims: crate::jwt::DecodedJwtClaims { inner: claims },
        };
        
        let mut config = SsaValidationConfig::default();
        config.validate_grant_types_array = false;
        config.validate_software_roles_array = false;
        
        let result = validate_ssa_structure_with_config(&decoded_jwt, &config);
        assert!(result.is_ok());
    }
} 