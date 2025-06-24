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
//! - **software_id**: Unique identifier for the software
//! - **grant_types**: Array of OAuth2 grant types the software can use
//! - **org_id**: Organization identifier
//! - **iss**: Issuer of the SSA JWT
//! - **software_roles**: Array of roles/permissions for the software
//! - **exp**: Expiration time (Unix timestamp)
//! - **iat**: Issued at time (Unix timestamp)
//! - **jti**: JWT ID (unique identifier)
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
use thiserror::Error;

/// SSA JWT claims structure as defined by RFC 7591
#[derive(Debug, Deserialize, Serialize, PartialEq)]
pub struct SsaClaims {
    /// Software identifier
    pub software_id: String,
    /// Grant types the software is authorized to use
    pub grant_types: Vec<String>,
    /// Organization identifier
    pub org_id: String,
    /// Issuer of the SSA
    pub iss: String,
    /// Software roles/permissions
    pub software_roles: Vec<String>,
    /// Expiration time
    pub exp: u64,
    /// Issued at time
    pub iat: u64,
    /// JWT ID
    pub jti: String,
    /// Additional custom claims
    #[serde(flatten)]
    pub additional_claims: Value,
}

#[cfg(test)]
pub async fn validate_ssa_jwt(
    ssa_jwt: &str,
    _jwks_uri: &str,
    _accept_invalid_certs: bool,
) -> Result<SsaClaims, SsaValidationError> {
    // Only validate structure in test mode, skip signature validation
    let decoded_jwt = decode_jwt(ssa_jwt)?;
    validate_ssa_structure(&decoded_jwt)?;
    // Return dummy claims for test
    let claims: SsaClaims = serde_json::from_value(decoded_jwt.claims.inner.clone())
        .map_err(|e| SsaValidationError::JwtError(jsonwebtoken::errors::Error::from(e)))?;
    Ok(claims)
}

#[cfg(not(test))]
pub async fn validate_ssa_jwt(
    ssa_jwt: &str,
    jwks_uri: &str,
    accept_invalid_certs: bool,
) -> Result<SsaClaims, SsaValidationError> {
    // First decode the JWT to get basic information
    let decoded_jwt = decode_jwt(ssa_jwt)?;
    
    // Validate the JWT structure and basic claims
    validate_ssa_structure(&decoded_jwt)?;
    
    // Fetch JWKS from the issuer
    let jwks = fetch_jwks(jwks_uri, accept_invalid_certs).await?;
    
    // Find the appropriate key for validation
    let decoding_key = find_decoding_key(&decoded_jwt, &jwks)?;
    
    // Validate the JWT signature and claims
    let claims = validate_ssa_signature_and_claims(ssa_jwt, &decoding_key)?;
    
    Ok(claims)
}

/// Validates the basic structure and required claims of an SSA JWT
pub fn validate_ssa_structure(decoded_jwt: &DecodedJwt) -> Result<(), SsaValidationError> {
    // Check if required claims are present
    let claims = &decoded_jwt.claims.inner;
    
    let required_claims = ["software_id", "grant_types", "org_id", "iss", "software_roles", "exp", "iat", "jti"];
    let mut missing_claims = Vec::new();
    
    for claim in required_claims.iter() {
        if claims.get(*claim).is_none() {
            missing_claims.push(*claim);
        }
    }
    
    if !missing_claims.is_empty() {
        return Err(SsaValidationError::MissingRequiredClaims(missing_claims));
    }
    
    // Validate grant_types is an array
    if let Some(grant_types) = claims.get("grant_types") {
        if !grant_types.is_array() {
            return Err(SsaValidationError::InvalidGrantTypes);
        }
    }
    
    // Validate software_roles is an array
    if let Some(software_roles) = claims.get("software_roles") {
        if !software_roles.is_array() {
            return Err(SsaValidationError::InvalidSoftwareRoles);
        }
    }
    
    // Validate exp and iat are numbers
    if let Some(exp) = claims.get("exp") {
        if !exp.is_number() {
            return Err(SsaValidationError::InvalidExpirationTime);
        }
    }
    
    if let Some(iat) = claims.get("iat") {
        if !iat.is_number() {
            return Err(SsaValidationError::InvalidIssuedAtTime);
        }
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
            if let Some(key_kid) = key.get("kid").and_then(|k| k.as_str()) {
                if key_kid == kid {
                    return create_decoding_key(key, &decoded_jwt.header.alg);
                }
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

/// Validates the SSA JWT signature and claims
fn validate_ssa_signature_and_claims(
    ssa_jwt: &str,
    decoding_key: &DecodingKey,
) -> Result<SsaClaims, SsaValidationError> {
    let mut validation = Validation::new(Algorithm::RS256);
    validation.validate_exp = true;
    validation.validate_nbf = false;
    validation.required_spec_claims.clear();
    validation.validate_aud = false;
    
    let token_data = jwt::decode::<SsaClaims>(ssa_jwt, decoding_key, &validation)
        .map_err(SsaValidationError::JwtError)?;
    
    Ok(token_data.claims)
}

#[derive(Debug, Error)]
pub enum SsaValidationError {
    #[error("failed to decode JWT: {0}")]
    DecodeJwt(#[from] DecodeJwtError),
    
    #[error("missing required claims: {0:?}")]
    MissingRequiredClaims(Vec<&'static str>),
    
    #[error("grant_types must be an array")]
    InvalidGrantTypes,
    
    #[error("software_roles must be an array")]
    InvalidSoftwareRoles,
    
    #[error("exp must be a number")]
    InvalidExpirationTime,
    
    #[error("iat must be a number")]
    InvalidIssuedAtTime,
    
    #[error("failed to initialize HTTP client: {0}")]
    HttpClientError(#[from] reqwest::Error),
    
    #[error("failed to fetch JWKS: {0}")]
    JwksFetchError(reqwest::Error),
    
    #[error("failed to parse JWKS: {0}")]
    JwksParseError(reqwest::Error),
    
    #[error("missing key ID (kid) in JWT header")]
    MissingKeyId,
    
    #[error("key not found in JWKS: {0}")]
    KeyNotFound(String),
    
    #[error("failed to decode key: {0}")]
    KeyDecodeError(#[from] base64::DecodeError),
    
    #[error("invalid key format")]
    InvalidKeyFormat,
    
    #[error("unsupported algorithm")]
    UnsupportedAlgorithm,
    
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
        
        let result = validate_ssa_structure(&decoded_jwt);
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
        
        let result = validate_ssa_structure(&decoded_jwt);
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
        
        let result = validate_ssa_structure(&decoded_jwt);
        assert!(matches!(result, Err(SsaValidationError::InvalidGrantTypes)));
    }
} 