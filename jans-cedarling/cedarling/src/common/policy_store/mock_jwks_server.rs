// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Mock JWKS server for testing trusted issuer validation.
//!
//! This module provides a mock server that simulates an OIDC provider with:
//! - OpenID Configuration endpoint (/.well-known/openid-configuration)
//! - JWKS endpoint for token signature validation
//! - Token generation with configurable claims
//!
//! # Example
//!
//! ```ignore
//! use cedarling::common::policy_store::mock_jwks_server::MockJwksServer;
//!
//! let server = MockJwksServer::new().await?;
//!
//! // Get a trusted issuer configuration for this server
//! let issuer = server.trusted_issuer_config();
//!
//! // Generate a test token
//! let token = server.generate_token(serde_json::json!({
//!     "sub": "user123",
//!     "email": "user@example.com"
//! }))?;
//!
//! // Verify endpoints are accessible
//! server.assert_endpoints_called();
//! ```

#![cfg(test)]
#![allow(dead_code)] // Test utilities - not all methods used in every test

use jsonwebtoken::{self as jwt, EncodingKey, Header};
use mockito::{Mock, Server, ServerGuard};
use serde::Serialize;
use serde_json::{Value, json};

/// Error types for mock server operations.
#[derive(Debug, thiserror::Error)]
pub enum MockServerError {
    #[error("Failed to generate keypair: {0}")]
    KeyGeneration(String),
    #[error("Failed to encode token: {0}")]
    TokenEncoding(#[from] jwt::errors::Error),
}

/// A key pair for signing and verifying tokens.
#[derive(Clone)]
pub struct JwkKeyPair {
    /// Key ID
    pub kid: String,
    /// Algorithm
    pub alg: jwt::Algorithm,
    /// Encoding key (for signing)
    encoding_key: EncodingKey,
    /// Public key in JWK format (for JWKS endpoint)
    jwk: jwt::jwk::Jwk,
}

impl JwkKeyPair {
    /// Generate an HS256 key pair.
    pub fn generate_hs256(kid: impl Into<String>) -> Result<Self, MockServerError> {
        use jsonwebkey as jwk_gen;

        let kid = kid.into();
        let mut key = jwk_gen::JsonWebKey::new(jwk_gen::Key::generate_symmetric(256));
        key.set_algorithm(jwk_gen::Algorithm::HS256)
            .map_err(|e| MockServerError::KeyGeneration(e.to_string()))?;
        key.key_id = Some(kid.clone());

        // Create encoding key
        let encoding_key = match *key.key {
            jwk_gen::Key::Symmetric { ref key } => EncodingKey::from_secret(key),
            _ => {
                return Err(MockServerError::KeyGeneration(
                    "Expected symmetric key".to_string(),
                ));
            },
        };

        // Create JWK for JWKS endpoint
        let mut jwk_value = serde_json::to_value(&key)
            .map_err(|e| MockServerError::KeyGeneration(e.to_string()))?;
        jwk_value["kid"] = json!(kid);

        let jwk: jwt::jwk::Jwk = serde_json::from_value(jwk_value)
            .map_err(|e| MockServerError::KeyGeneration(e.to_string()))?;

        Ok(Self {
            kid,
            alg: jwt::Algorithm::HS256,
            encoding_key,
            jwk,
        })
    }

    /// Sign claims and generate a JWT.
    pub fn sign_token(&self, claims: &impl Serialize) -> Result<String, MockServerError> {
        let header = Header {
            alg: self.alg,
            kid: Some(self.kid.clone()),
            ..Default::default()
        };

        jwt::encode(&header, claims, &self.encoding_key).map_err(MockServerError::from)
    }
}

/// Mock endpoints configuration.
pub struct MockEndpoints {
    /// OpenID Configuration endpoint mock
    pub oidc_config: Mock,
    /// JWKS endpoint mock
    pub jwks: Mock,
}

impl MockEndpoints {
    /// Assert all endpoints were called the expected number of times.
    pub fn assert(&self) {
        self.oidc_config.assert();
        self.jwks.assert();
    }
}

/// Mock JWKS server for testing trusted issuer validation.
pub struct MockJwksServer {
    /// The underlying mock server
    server: ServerGuard,
    /// Key pair for signing tokens
    key_pair: JwkKeyPair,
    /// Endpoints
    endpoints: MockEndpoints,
}

impl MockJwksServer {
    /// Path for OpenID Configuration endpoint.
    pub const OIDC_CONFIG_PATH: &'static str = "/.well-known/openid-configuration";
    /// Path for JWKS endpoint.
    pub const JWKS_PATH: &'static str = "/jwks";

    /// Create a new mock JWKS server with default endpoints.
    pub async fn new() -> Result<Self, MockServerError> {
        Self::with_kid("test-key-1").await
    }

    /// Create a new mock JWKS server with a custom key ID.
    pub async fn with_kid(kid: impl Into<String>) -> Result<Self, MockServerError> {
        let mut server = Server::new_async().await;
        let key_pair = JwkKeyPair::generate_hs256(kid)?;

        let base_url = server.url();

        // Setup OpenID Configuration endpoint
        let oidc_config = server
            .mock("GET", Self::OIDC_CONFIG_PATH)
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(
                json!({
                    "issuer": base_url,
                    "jwks_uri": format!("{}{}", base_url, Self::JWKS_PATH),
                    "authorization_endpoint": format!("{}/authorize", base_url),
                    "token_endpoint": format!("{}/token", base_url),
                    "userinfo_endpoint": format!("{}/userinfo", base_url),
                    "response_types_supported": ["code", "token", "id_token"],
                    "subject_types_supported": ["public"],
                    "id_token_signing_alg_values_supported": ["HS256", "RS256"],
                })
                .to_string(),
            )
            .expect_at_least(0)
            .create();

        // Setup JWKS endpoint
        let jwks = server
            .mock("GET", Self::JWKS_PATH)
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(
                json!({
                    "keys": [key_pair.jwk.clone()]
                })
                .to_string(),
            )
            .expect_at_least(0)
            .create();

        Ok(Self {
            server,
            key_pair,
            endpoints: MockEndpoints { oidc_config, jwks },
        })
    }

    /// Get the base URL of the mock server.
    pub fn url(&self) -> String {
        self.server.url()
    }

    /// Get the OpenID Configuration endpoint URL.
    pub fn oidc_config_url(&self) -> String {
        format!("{}{}", self.server.url(), Self::OIDC_CONFIG_PATH)
    }

    /// Get the JWKS endpoint URL.
    pub fn jwks_url(&self) -> String {
        format!("{}{}", self.server.url(), Self::JWKS_PATH)
    }

    /// Generate a trusted issuer configuration for use in policy stores.
    pub fn trusted_issuer_json(&self, issuer_id: &str) -> Value {
        json!({
            issuer_id: {
                "name": format!("Mock Issuer {}", issuer_id),
                "oidc_endpoint": self.oidc_config_url(),
                "token_metadata": {
                    "access_token": {
                        "user_id": "sub",
                        "required_claims": ["sub", "aud"]
                    },
                    "id_token": {
                        "user_id": "sub",
                        "required_claims": ["sub", "email"]
                    }
                }
            }
        })
    }

    /// Generate a trusted issuer configuration with custom token metadata.
    pub fn trusted_issuer_json_with_metadata(
        &self,
        issuer_id: &str,
        token_metadata: Value,
    ) -> Value {
        json!({
            issuer_id: {
                "name": format!("Mock Issuer {}", issuer_id),
                "oidc_endpoint": self.oidc_config_url(),
                "token_metadata": token_metadata
            }
        })
    }

    /// Generate a test token with the given claims.
    ///
    /// Automatically adds `iss` claim set to the server URL.
    pub fn generate_token(&self, mut claims: Value) -> Result<String, MockServerError> {
        claims["iss"] = json!(self.url());
        self.key_pair.sign_token(&claims)
    }

    /// Generate a token with standard claims.
    ///
    /// # Arguments
    /// * `sub` - Subject (user ID)
    /// * `aud` - Audience
    /// * `extra_claims` - Additional claims to merge
    pub fn generate_standard_token(
        &self,
        sub: &str,
        aud: &str,
        extra_claims: Option<Value>,
    ) -> Result<String, MockServerError> {
        use std::time::{SystemTime, UNIX_EPOCH};

        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();

        let mut claims = json!({
            "sub": sub,
            "aud": aud,
            "iss": self.url(),
            "iat": now,
            "exp": now + 3600, // 1 hour
            "jti": uuid7::uuid7().to_string(),
        });

        if let Some(extra) = extra_claims {
            if let (Some(claims_obj), Some(extra_obj)) = (claims.as_object_mut(), extra.as_object())
            {
                for (k, v) in extra_obj {
                    claims_obj.insert(k.clone(), v.clone());
                }
            }
        }

        self.key_pair.sign_token(&claims)
    }

    /// Generate an access token with required claims.
    pub fn generate_access_token(
        &self,
        sub: &str,
        aud: &str,
        scope: &str,
    ) -> Result<String, MockServerError> {
        self.generate_standard_token(
            sub,
            aud,
            Some(json!({
                "scope": scope,
                "token_type": "Bearer"
            })),
        )
    }

    /// Generate an ID token with required claims.
    pub fn generate_id_token(
        &self,
        sub: &str,
        aud: &str,
        email: &str,
    ) -> Result<String, MockServerError> {
        use std::time::{SystemTime, UNIX_EPOCH};

        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();

        self.generate_standard_token(
            sub,
            aud,
            Some(json!({
                "email": email,
                "email_verified": true,
                "name": "Test User",
                "auth_time": now,
                "nonce": "test-nonce"
            })),
        )
    }

    /// Assert that endpoints were called.
    pub fn assert_endpoints(&self) {
        self.endpoints.assert();
    }

    /// Get access to the endpoints for custom assertions.
    pub fn endpoints(&self) -> &MockEndpoints {
        &self.endpoints
    }

    /// Get the key pair for custom token generation.
    pub fn key_pair(&self) -> &JwkKeyPair {
        &self.key_pair
    }
}

/// Builder for creating multiple mock servers (for multi-issuer testing).
pub struct MockJwksServerBuilder {
    servers: Vec<MockJwksServer>,
}

impl MockJwksServerBuilder {
    /// Create a new builder.
    pub fn new() -> Self {
        Self {
            servers: Vec::new(),
        }
    }

    /// Add a server with the given key ID.
    pub async fn with_server(mut self, kid: &str) -> Result<Self, MockServerError> {
        let server = MockJwksServer::with_kid(kid).await?;
        self.servers.push(server);
        Ok(self)
    }

    /// Build and return all servers.
    pub fn build(self) -> Vec<MockJwksServer> {
        self.servers
    }

    /// Generate trusted issuers JSON for all servers.
    pub fn trusted_issuers_json(&self) -> Value {
        let mut issuers = json!({});

        for (i, server) in self.servers.iter().enumerate() {
            let issuer_id = format!("issuer{}", i);
            let issuer_config = server.trusted_issuer_json(&issuer_id);
            if let Some(obj) = issuer_config.as_object() {
                for (k, v) in obj {
                    issuers[k] = v.clone();
                }
            }
        }

        issuers
    }
}

impl Default for MockJwksServerBuilder {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_mock_server_creation() {
        let server = MockJwksServer::new().await.unwrap();
        assert!(!server.url().is_empty());
        assert!(server.oidc_config_url().contains(".well-known"));
    }

    #[tokio::test]
    async fn test_generate_token() {
        let server = MockJwksServer::new().await.unwrap();
        let token = server
            .generate_token(json!({
                "sub": "user123",
                "aud": "my-app"
            }))
            .unwrap();

        // Token should have 3 parts (header.payload.signature)
        assert_eq!(token.split('.').count(), 3);
    }

    #[tokio::test]
    async fn test_generate_access_token() {
        let server = MockJwksServer::new().await.unwrap();
        let token = server
            .generate_access_token("user123", "my-app", "openid profile")
            .unwrap();

        assert!(!token.is_empty());
    }

    #[tokio::test]
    async fn test_generate_id_token() {
        let server = MockJwksServer::new().await.unwrap();
        let token = server
            .generate_id_token("user123", "my-app", "user@example.com")
            .unwrap();

        assert!(!token.is_empty());
    }

    #[tokio::test]
    async fn test_trusted_issuer_json() {
        let server = MockJwksServer::new().await.unwrap();
        let config = server.trusted_issuer_json("test-issuer");

        assert!(config["test-issuer"]["name"].is_string());
        assert!(config["test-issuer"]["oidc_endpoint"].is_string());
        assert!(config["test-issuer"]["token_metadata"].is_object());
    }

    #[tokio::test]
    async fn test_multiple_servers() {
        let servers = MockJwksServerBuilder::new()
            .with_server("key1")
            .await
            .unwrap()
            .with_server("key2")
            .await
            .unwrap()
            .build();

        assert_eq!(servers.len(), 2);
        assert_ne!(servers[0].url(), servers[1].url());
    }

    #[tokio::test]
    async fn test_oidc_endpoint_accessible() {
        let server = MockJwksServer::new().await.unwrap();
        let url = server.oidc_config_url();

        // Make a request to verify the endpoint works
        let client = reqwest::Client::new();
        let response = client.get(&url).send().await.unwrap();

        assert!(response.status().is_success());
        let body: Value = response.json().await.unwrap();
        assert!(body["issuer"].is_string());
        assert!(body["jwks_uri"].is_string());
    }

    #[tokio::test]
    async fn test_jwks_endpoint_accessible() {
        let server = MockJwksServer::new().await.unwrap();
        let url = server.jwks_url();

        let client = reqwest::Client::new();
        let response = client.get(&url).send().await.unwrap();

        assert!(response.status().is_success());
        let body: Value = response.json().await.unwrap();
        assert!(body["keys"].is_array());
        assert!(!body["keys"].as_array().unwrap().is_empty());
    }
}
