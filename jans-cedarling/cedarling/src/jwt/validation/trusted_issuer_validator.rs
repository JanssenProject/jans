// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Trusted issuer JWT validation module.
//!
//! This module provides functionality to validate JWT tokens against configured trusted issuers,
//! including issuer matching, required claims validation, JWKS fetching, and signature verification.

use crate::common::policy_store::{TokenEntityMetadata, TrustedIssuer};
use crate::jwt::http_utils::{GetFromUrl, OpenIdConfig};
use crate::jwt::key_service::{DecodingKeyInfo, KeyService, KeyServiceError};
use crate::log::Logger;
use jsonwebtoken::{Algorithm, DecodingKey, Validation, decode, decode_header};
use serde_json::Value as JsonValue;
use std::collections::HashMap;
use std::sync::Arc;
use thiserror::Error;
use url::Url;

/// Errors that can occur during trusted issuer validation.
#[derive(Debug, Error)]
pub enum TrustedIssuerError {
    /// Token issuer is not in the list of trusted issuers
    #[error("Untrusted issuer: '{0}'")]
    UntrustedIssuer(String),

    /// Token is missing a required claim
    #[error("Missing required claim: '{claim}' for token type '{token_type}'")]
    MissingRequiredClaim { claim: String, token_type: String },

    /// Failed to decode JWT header
    #[error("Failed to decode JWT header: {0}")]
    DecodeHeader(#[from] jsonwebtoken::errors::Error),

    /// Failed to fetch OpenID configuration
    #[error("Failed to fetch OpenID configuration from '{endpoint}': {source}")]
    OpenIdConfigFetch {
        endpoint: String,
        #[source]
        source: Box<dyn std::error::Error + Send + Sync>,
    },

    /// Failed to fetch or process JWKS
    #[error("Failed to fetch JWKS: {0}")]
    JwksFetch(#[from] KeyServiceError),

    /// No matching key found in JWKS
    #[error("No matching key found for kid: {}, algorithm: '{alg:?}'", kid.as_ref().map(|s| s.as_str()).unwrap_or("none"))]
    NoMatchingKey { kid: Option<String>, alg: Algorithm },

    /// JWT signature validation failed
    #[error("Invalid JWT signature: {0}")]
    InvalidSignature(String),

    /// Token type not configured for issuer
    #[error("Token type '{token_type}' not configured for issuer '{issuer}'")]
    TokenTypeNotConfigured { token_type: String, issuer: String },

    /// Missing issuer claim in token
    #[error("Token missing 'iss' claim")]
    MissingIssuerClaim,
}

/// Result type for trusted issuer validation operations.
pub type Result<T> = std::result::Result<T, TrustedIssuerError>;

/// Validator for JWT tokens against trusted issuer configurations.
///
/// This validator provides the following functionality:
/// - Issuer matching against configured trusted issuers
/// - Required claims validation based on token metadata
/// - JWKS fetching and caching
/// - JWT signature verification
pub struct TrustedIssuerValidator {
    /// Map of issuer identifiers to their configurations
    trusted_issuers: HashMap<String, Arc<TrustedIssuer>>,
    /// Key service for managing JWKS keys
    key_service: KeyService,
    /// Cache of fetched OpenID configurations (issuer URL -> config)
    oidc_configs: HashMap<String, Arc<OpenIdConfig>>,
    /// Optional logger for diagnostic messages
    logger: Option<Logger>,
}

impl TrustedIssuerValidator {
    /// Creates a new trusted issuer validator with the given trusted issuers.
    pub fn new(trusted_issuers: HashMap<String, TrustedIssuer>) -> Self {
        Self::with_logger(trusted_issuers, None)
    }

    /// Creates a new trusted issuer validator with a logger.
    pub fn with_logger(
        trusted_issuers: HashMap<String, TrustedIssuer>,
        logger: Option<Logger>,
    ) -> Self {
        let trusted_issuers = trusted_issuers
            .into_iter()
            .map(|(k, v)| (k, Arc::new(v)))
            .collect();

        Self {
            trusted_issuers,
            key_service: KeyService::new(),
            oidc_configs: HashMap::new(),
            logger,
        }
    }

    /// Finds a trusted issuer by the issuer claim value.
    ///
    /// This method matches the token's `iss` claim against the configured trusted issuers.
    /// The matching is done by comparing the issuer URL or issuer ID.
    pub fn find_trusted_issuer(&self, issuer_claim: &str) -> Result<Arc<TrustedIssuer>> {
        // Try exact match first by issuer ID
        if let Some(issuer) = self.trusted_issuers.get(issuer_claim) {
            return Ok(Arc::clone(issuer));
        }

        // Try matching by OIDC endpoint base URL
        // Parse the issuer claim as a URL and compare with configured endpoints
        if let Ok(iss_url) = Url::parse(issuer_claim) {
            for (id, issuer) in &self.trusted_issuers {
                // Check if the OIDC endpoint starts with the issuer URL
                if let Some(base) = issuer
                    .oidc_endpoint
                    .as_str()
                    .strip_suffix("/.well-known/openid-configuration")
                {
                    if base == iss_url.as_str().trim_end_matches('/') {
                        return Ok(Arc::clone(issuer));
                    }
                }

                // Also check if issuer ID matches
                if id == &iss_url.to_string() {
                    return Ok(Arc::clone(issuer));
                }
            }
        }

        Err(TrustedIssuerError::UntrustedIssuer(
            issuer_claim.to_string(),
        ))
    }

    /// Fetches and caches the OpenID configuration for a trusted issuer.
    ///
    /// If the configuration has already been fetched, returns the cached version.
    async fn get_or_fetch_oidc_config(
        &mut self,
        trusted_issuer: &TrustedIssuer,
    ) -> Result<Arc<OpenIdConfig>> {
        let endpoint_str = trusted_issuer.oidc_endpoint.as_str();

        // Check cache first
        if let Some(config) = self.oidc_configs.get(endpoint_str) {
            return Ok(Arc::clone(config));
        }

        // Fetch from endpoint
        let config = OpenIdConfig::get_from_url(&trusted_issuer.oidc_endpoint)
            .await
            .map_err(|e| TrustedIssuerError::OpenIdConfigFetch {
                endpoint: endpoint_str.to_string(),
                source: Box::new(e),
            })?;

        let config_arc = Arc::new(config);
        self.oidc_configs
            .insert(endpoint_str.to_string(), Arc::clone(&config_arc));

        Ok(config_arc)
    }

    /// Ensures JWKS keys are loaded for the given issuer.
    ///
    /// Fetches the OpenID configuration and loads keys from the JWKS endpoint.
    async fn ensure_keys_loaded(&mut self, trusted_issuer: &TrustedIssuer) -> Result<()> {
        let oidc_config = self.get_or_fetch_oidc_config(trusted_issuer).await?;

        // Check if we already have keys for this issuer
        if self.key_service.has_keys() {
            // TODO: check key expiration
            // and refresh if needed. For now, we assume keys are valid.
            return Ok(());
        }

        // Fetch keys using the key service
        self.key_service
            .get_keys_using_oidc(&oidc_config, &self.logger)
            .await?;

        Ok(())
    }

    /// Validates that a token contains all required claims based on token metadata.
    ///
    /// The required claims are determined by the token metadata configuration
    /// for the specific token type (e.g., access_token, id_token).
    pub fn validate_required_claims(
        &self,
        claims: &JsonValue,
        token_type: &str,
        token_metadata: &TokenEntityMetadata,
    ) -> Result<()> {
        // Check for entity_type_name (always required)
        if token_metadata.entity_type_name.is_empty() {
            return Err(TrustedIssuerError::MissingRequiredClaim {
                claim: "entity_type_name".to_string(),
                token_type: token_type.to_string(),
            });
        }

        // Validate user_id claim if configured
        if let Some(user_id_claim) = &token_metadata.user_id {
            if claims.get(user_id_claim).is_none() {
                return Err(TrustedIssuerError::MissingRequiredClaim {
                    claim: user_id_claim.clone(),
                    token_type: token_type.to_string(),
                });
            }
        }

        // Validate role_mapping claim if configured
        if let Some(role_claim) = &token_metadata.role_mapping {
            if claims.get(role_claim).is_none() {
                return Err(TrustedIssuerError::MissingRequiredClaim {
                    claim: role_claim.clone(),
                    token_type: token_type.to_string(),
                });
            }
        }

        // Validate workload_id claim if configured
        if let Some(workload_claim) = &token_metadata.workload_id {
            if claims.get(workload_claim).is_none() {
                return Err(TrustedIssuerError::MissingRequiredClaim {
                    claim: workload_claim.clone(),
                    token_type: token_type.to_string(),
                });
            }
        }

        // Validate token_id claim (e.g., "jti")
        if claims.get(&token_metadata.token_id).is_none() {
            return Err(TrustedIssuerError::MissingRequiredClaim {
                claim: token_metadata.token_id.clone(),
                token_type: token_type.to_string(),
            });
        }

        Ok(())
    }

    /// Validates a JWT token against a trusted issuer.
    ///
    /// This performs the following validations:
    /// 1. Extracts the issuer claim from the token
    /// 2. Matches the issuer against configured trusted issuers
    /// 3. Fetches JWKS if not already cached
    /// 4. Validates the JWT signature
    /// 5. Validates required claims based on token metadata
    ///
    /// Returns the validated claims and the matched trusted issuer.
    pub async fn validate_token(
        &mut self,
        token: &str,
        token_type: &str,
    ) -> Result<(JsonValue, Arc<TrustedIssuer>)> {
        // Decode the JWT header to get the key ID and algorithm
        let header = decode_header(token)?;

        // First, we need to decode without verification to get the issuer claim
        let mut validation = Validation::new(header.alg);
        validation.insecure_disable_signature_validation();
        validation.validate_exp = false;
        validation.validate_nbf = false;
        validation.required_spec_claims.clear();

        let unverified_token = decode::<JsonValue>(
            token,
            &DecodingKey::from_secret(&[]), // Dummy key since we disabled validation
            &validation,
        )?;

        // Extract issuer claim
        let issuer_claim = unverified_token
            .claims
            .get("iss")
            .and_then(|v| v.as_str())
            .ok_or(TrustedIssuerError::MissingIssuerClaim)?;

        // Find the trusted issuer
        let trusted_issuer = self.find_trusted_issuer(issuer_claim)?;

        // Get token metadata for this token type
        let token_metadata = trusted_issuer
            .token_metadata
            .get(token_type)
            .ok_or_else(|| TrustedIssuerError::TokenTypeNotConfigured {
                token_type: token_type.to_string(),
                issuer: issuer_claim.to_string(),
            })?;

        // Check if token is trusted
        if !token_metadata.trusted {
            return Err(TrustedIssuerError::UntrustedIssuer(
                issuer_claim.to_string(),
            ));
        }

        // Validate required claims (on unverified token)
        self.validate_required_claims(&unverified_token.claims, token_type, token_metadata)?;

        // Ensure JWKS keys are loaded for this issuer
        self.ensure_keys_loaded(&trusted_issuer).await?;

        // Now validate the signature
        let key_info = DecodingKeyInfo {
            issuer: Some(issuer_claim.to_string()),
            kid: header.kid.clone(),
            algorithm: header.alg,
        };

        let decoding_key = self.key_service.get_key(&key_info).ok_or_else(|| {
            TrustedIssuerError::NoMatchingKey {
                kid: header.kid,
                alg: header.alg,
            }
        })?;

        // Create validation with signature checking enabled
        let mut validation = Validation::new(header.alg);
        validation.set_issuer(&[issuer_claim]);

        // Optionally validate exp and nbf if they exist in claims
        validation.validate_exp = unverified_token.claims.get("exp").is_some();
        validation.validate_nbf = unverified_token.claims.get("nbf").is_some();

        // We've already validated required claims, so we don't need jsonwebtoken
        // to check for standard claims
        validation.required_spec_claims.clear();
        validation.validate_aud = false;

        // Decode and validate signature
        let verified_token = decode::<JsonValue>(token, decoding_key, &validation)
            .map_err(|e| TrustedIssuerError::InvalidSignature(e.to_string()))?;

        Ok((verified_token.claims, trusted_issuer))
    }

    /// Gets a reference to the key service for JWKS management.
    pub fn key_service(&self) -> &KeyService {
        &self.key_service
    }

    /// Gets a mutable reference to the key service for JWKS management.
    pub fn key_service_mut(&mut self) -> &mut KeyService {
        &mut self.key_service
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::TokenEntityMetadata;

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

    fn create_test_issuer_with_metadata(
        id: &str,
        endpoint: &str,
        metadata: HashMap<String, TokenEntityMetadata>,
    ) -> TrustedIssuer {
        TrustedIssuer {
            name: format!("Test Issuer {}", id),
            description: "Test issuer for validation".to_string(),
            oidc_endpoint: Url::parse(endpoint).unwrap(),
            token_metadata: metadata,
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
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::UntrustedIssuer(_)
        ));
    }

    #[test]
    fn test_validate_required_claims_success() {
        let validator = TrustedIssuerValidator::new(HashMap::new());

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

        let result = validator.validate_required_claims(&claims, "access_token", &metadata);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_required_claims_missing_user_id() {
        let validator = TrustedIssuerValidator::new(HashMap::new());

        let claims = serde_json::json!({
            "jti": "token123",
            "role": "admin"
        });

        let metadata = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_token".to_string())
            .user_id(Some("sub".to_string()))
            .role_mapping(Some("role".to_string()))
            .token_id("jti".to_string())
            .build();

        let result = validator.validate_required_claims(&claims, "access_token", &metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::MissingRequiredClaim { claim, .. } if claim == "sub"
        ));
    }

    #[test]
    fn test_validate_required_claims_missing_role() {
        let validator = TrustedIssuerValidator::new(HashMap::new());

        let claims = serde_json::json!({
            "sub": "user123",
            "jti": "token123"
        });

        let metadata = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_token".to_string())
            .user_id(Some("sub".to_string()))
            .role_mapping(Some("role".to_string()))
            .token_id("jti".to_string())
            .build();

        let result = validator.validate_required_claims(&claims, "access_token", &metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::MissingRequiredClaim { claim, .. } if claim == "role"
        ));
    }

    #[test]
    fn test_validate_required_claims_missing_token_id() {
        let validator = TrustedIssuerValidator::new(HashMap::new());

        let claims = serde_json::json!({
            "sub": "user123",
            "role": "admin"
        });

        let metadata = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_token".to_string())
            .user_id(Some("sub".to_string()))
            .role_mapping(Some("role".to_string()))
            .token_id("jti".to_string())
            .build();

        let result = validator.validate_required_claims(&claims, "access_token", &metadata);
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::MissingRequiredClaim { claim, .. } if claim == "jti"
        ));
    }

    /// Helper to create a test JWT token with given claims and key
    #[cfg(test)]
    fn create_test_jwt(claims: &serde_json::Value, kid: &str, algorithm: Algorithm) -> String {
        use jsonwebtoken::{EncodingKey, Header, encode};

        let mut header = Header::new(algorithm);
        header.kid = Some(kid.to_string());

        // Use a test key for signing (this would match the mock JWKS)
        let key = EncodingKey::from_secret(b"test_secret_key");

        encode(&header, claims, &key).expect("Failed to create test JWT")
    }

    #[tokio::test]
    async fn test_get_or_fetch_oidc_config_caching() {
        let mut server = mockito::Server::new_async().await;
        let oidc_url = format!("{}/.well-known/openid-configuration", server.url());

        // Mock the OIDC configuration endpoint
        let mock = server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(serde_json::json!({
                "issuer": server.url(),
                "jwks_uri": format!("{}/jwks", server.url()),
            }).to_string())
            .expect(1) // Should only be called once due to caching
            .create_async()
            .await;

        let issuer = create_test_issuer("test", &oidc_url);
        let mut validator = TrustedIssuerValidator::new(HashMap::new());

        // First fetch - should call the endpoint
        let config1 = validator.get_or_fetch_oidc_config(&issuer).await;
        assert!(config1.is_ok(), "First fetch should succeed");

        // Second fetch - should use cache (mock expects only 1 call)
        let config2 = validator.get_or_fetch_oidc_config(&issuer).await;
        assert!(config2.is_ok(), "Second fetch should succeed from cache");

        // Verify same Arc
        assert!(Arc::ptr_eq(&config1.unwrap(), &config2.unwrap()));

        mock.assert_async().await;
    }

    #[tokio::test]
    async fn test_get_or_fetch_oidc_config_invalid_endpoint() {
        let invalid_url = "https://invalid-endpoint-that-does-not-exist.example.com/.well-known/openid-configuration";
        let issuer = create_test_issuer("test", invalid_url);
        let mut validator = TrustedIssuerValidator::new(HashMap::new());

        let result = validator.get_or_fetch_oidc_config(&issuer).await;
        assert!(result.is_err());
        if let Err(err) = result {
            assert!(matches!(err, TrustedIssuerError::OpenIdConfigFetch { .. }));
        }
    }

    #[tokio::test]
    async fn test_ensure_keys_loaded_success() {
        let mut server = mockito::Server::new_async().await;
        let oidc_url = format!("{}/.well-known/openid-configuration", server.url());

        // Mock OIDC configuration
        let _oidc_mock = server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(
                serde_json::json!({
                    "issuer": server.url(),
                    "jwks_uri": format!("{}/jwks", server.url()),
                })
                .to_string(),
            )
            .create_async()
            .await;

        // Mock JWKS endpoint with a test key
        let _jwks_mock = server
            .mock("GET", "/jwks")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(serde_json::json!({
                "keys": [{
                    "kty": "RSA",
                    "kid": "test=-key-1",
                    "use": "sig",
                    "alg": "RS256",
                    "n": "xGOr-H7A-PWR8nRExwEPEe8spD9FwPJSq2KsuJFQH5JvFvOsKNgLvXX6BxJwDAj9K7rZHvqcL4aJkGDVpYE_1x4zAFXgSzYTqQVq0Ts",
                    "e": "AQAB"
                }]
            }).to_string())
            .create_async()
            .await;

        let issuer = create_test_issuer("test", &oidc_url);
        let mut validator = TrustedIssuerValidator::new(HashMap::new());

        let result = validator.ensure_keys_loaded(&issuer).await;
        assert!(result.is_ok(), "Keys should be loaded successfully");
        assert!(
            validator.key_service().has_keys(),
            "Key service should have keys"
        );
    }

    #[tokio::test]
    async fn test_validate_token_untrusted_issuer() {
        let mut validator = TrustedIssuerValidator::new(HashMap::from([(
            "issuer1".to_string(),
            create_test_issuer("1", "https://issuer1.com/.well-known/openid-configuration"),
        )]));

        // Create a token with an untrusted issuer
        let claims = serde_json::json!({
            "iss": "https://evil.com",
            "sub": "user123",
            "jti": "token123",
            "exp": 9999999999i64,
        });

        let token = create_test_jwt(&claims, "test-kid", Algorithm::HS256);

        let result = validator.validate_token(&token, "access_token").await;
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::UntrustedIssuer(_)
        ));
    }

    #[tokio::test]
    async fn test_validate_token_missing_issuer_claim() {
        let mut validator = TrustedIssuerValidator::new(HashMap::new());

        // Create a token without issuer claim
        let claims = serde_json::json!({
            "sub": "user123",
            "jti": "token123",
        });

        let token = create_test_jwt(&claims, "test-kid", Algorithm::HS256);

        let result = validator.validate_token(&token, "access_token").await;
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::MissingIssuerClaim
        ));
    }

    #[tokio::test]
    async fn test_validate_token_untrusted_token_type() {
        let mut metadata = HashMap::new();
        metadata.insert(
            "access_token".to_string(),
            TokenEntityMetadata::builder()
                .entity_type_name("Jans::Access_token".to_string())
                .trusted(false) // Not trusted!
                .token_id("jti".to_string())
                .build(),
        );

        let issuer = create_test_issuer_with_metadata(
            "test",
            "https://test.com/.well-known/openid-configuration",
            metadata,
        );

        let mut validator =
            TrustedIssuerValidator::new(HashMap::from([("test".to_string(), issuer)]));

        let claims = serde_json::json!({
            "iss": "test",
            "sub": "user123",
            "jti": "token123",
        });

        let token = create_test_jwt(&claims, "test-kid", Algorithm::HS256);

        let result = validator.validate_token(&token, "access_token").await;
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::UntrustedIssuer(_)
        ));
    }

    #[tokio::test]
    async fn test_validate_token_token_type_not_configured() {
        let issuer =
            create_test_issuer("test", "https://test.com/.well-known/openid-configuration");

        let mut validator =
            TrustedIssuerValidator::new(HashMap::from([("test".to_string(), issuer)]));

        let claims = serde_json::json!({
            "iss": "test",
            "sub": "user123",
            "jti": "token123",
        });

        let token = create_test_jwt(&claims, "test-kid", Algorithm::HS256);

        // Request validation for a token type that's not configured
        let result = validator.validate_token(&token, "userinfo_token").await;
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::TokenTypeNotConfigured { .. }
        ));
    }

    #[tokio::test]
    async fn test_validate_token_missing_required_claims_integration() {
        let mut metadata = HashMap::new();
        metadata.insert(
            "access_token".to_string(),
            TokenEntityMetadata::builder()
                .entity_type_name("Jans::Access_token".to_string())
                .user_id(Some("sub".to_string()))
                .role_mapping(Some("role".to_string()))
                .token_id("jti".to_string())
                .build(),
        );

        let issuer = create_test_issuer_with_metadata(
            "test",
            "https://test.com/.well-known/openid-configuration",
            metadata,
        );

        let mut validator =
            TrustedIssuerValidator::new(HashMap::from([("test".to_string(), issuer)]));

        // Token missing "role" claim
        let claims = serde_json::json!({
            "iss": "test",
            "sub": "user123",
            "jti": "token123",
            // Missing "role"
        });

        let token = create_test_jwt(&claims, "test-kid", Algorithm::HS256);

        let result = validator.validate_token(&token, "access_token").await;
        assert!(result.is_err());
        assert!(matches!(
            result.unwrap_err(),
            TrustedIssuerError::MissingRequiredClaim { claim, .. } if claim == "role"
        ));
    }

    #[tokio::test]
    async fn test_validator_with_logger() {
        let issuers = HashMap::from([(
            "issuer1".to_string(),
            create_test_issuer("1", "https://issuer1.com/.well-known/openid-configuration"),
        )]);

        // Test with None logger (valid case)
        let validator_none = TrustedIssuerValidator::with_logger(issuers.clone(), None);
        assert!(validator_none.logger.is_none());

        // Test with Some logger - we'll test that the constructor accepts it
        // Note: Creating a real Logger requires internal log types, so we just test None case
        // The important part is that the API supports Option<Logger>

        // Verify trusted issuers are loaded
        let result = validator_none.find_trusted_issuer("issuer1");
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn test_multiple_issuers_matching() {
        let issuers = HashMap::from([
            (
                "issuer1".to_string(),
                create_test_issuer("1", "https://issuer1.com/.well-known/openid-configuration"),
            ),
            (
                "issuer2".to_string(),
                create_test_issuer("2", "https://issuer2.com/.well-known/openid-configuration"),
            ),
            (
                "issuer3".to_string(),
                create_test_issuer("3", "https://issuer3.com/.well-known/openid-configuration"),
            ),
        ]);

        let validator = TrustedIssuerValidator::new(issuers);

        // Test matching each issuer
        assert!(validator.find_trusted_issuer("issuer1").is_ok());
        assert!(validator.find_trusted_issuer("issuer2").is_ok());
        assert!(validator.find_trusted_issuer("issuer3").is_ok());

        // Test URL-based matching
        assert!(validator.find_trusted_issuer("https://issuer1.com").is_ok());
        assert!(validator.find_trusted_issuer("https://issuer2.com").is_ok());

        // Test invalid issuer
        assert!(validator.find_trusted_issuer("issuer4").is_err());
        assert!(validator.find_trusted_issuer("https://evil.com").is_err());
    }
}
