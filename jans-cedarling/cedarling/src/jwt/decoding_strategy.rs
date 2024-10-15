/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::traits::{Decode, GetKey};
use super::Error;
use di::DependencySupplier;
use jsonwebtoken as jwt;
use serde::de::DeserializeOwned;
use std::sync::Arc;

/// Represents the decoding strategy for JWT tokens.
///
/// This enum determines how JWT tokens are decoded, either without validation
/// or with validation using specified algorithms and a key service. The
/// appropriate strategy can be selected based on the bootstrap config.
pub enum DecodingStrategy {
    /// Decoding strategy without validation.
    WithoutValidation,

    /// Decoding strategy with validation.
    WithValidation {
        key_service: Arc<dyn GetKey>,
        supported_algs: Vec<jwt::Algorithm>,
    },
}

impl DecodingStrategy {
    /// Creates a new decoding strategy that does not perform validation.
    ///
    /// This method initializes the `WithoutValidation` strategy, which
    /// decodes JWT tokens without any validation checks
    pub fn new_without_validation() -> Self {
        Self::WithoutValidation
    }

    /// Decodes a JWT without performing validation.
    fn decode_jwt_without_validation<T: DeserializeOwned>(jwt: &str) -> Result<T, Error> {
        let header = jwt::decode_header(jwt).map_err(|e| Error::ParsingError(e))?;

        let mut validator = jwt::Validation::new(header.alg);
        validator.insecure_disable_signature_validation();
        validator.validate_exp = false;
        validator.validate_aud = false;
        validator.validate_nbf = false;

        let key = jwt::DecodingKey::from_secret("secret".as_ref());

        let claims = jwt::decode::<T>(&jwt, &key, &validator)
            .map_err(|e| Error::ValidationError(e))?
            .claims;

        Ok(claims)
    }

    /// Creates a new decoding strategy that performs validation.
    pub fn new_with_validation(
        dep_map: &di::DependencyMap,
        config_algs: &Vec<String>,
    ) -> Result<Self, Error> {
        let key_service: Arc<KeyServiceWrapper> = dep_map.get();
        let mut supported_algs = vec![];
        for alg_str in config_algs {
            supported_algs.push(string_to_alg(&alg_str)?);
        }

        Ok(Self::WithValidation {
            key_service: key_service.0.clone(),
            supported_algs,
        })
    }

    /// Decodes and validates a JWT using the specified algorithms and keys.
    ///
    /// Returns an error for tokens not signed with an algorithm not in
    /// `supported_algs` or when validation fails.
    fn decode_and_validate_jwt<T: serde::de::DeserializeOwned>(
        jwt: &str,
        supported_algs: &Vec<jwt::Algorithm>,
        key_service: &Arc<dyn GetKey>,
    ) -> Result<T, Error> {
        let header = jwt::decode_header(jwt).map_err(|e| Error::ParsingError(e))?;

        // Automatically reject unsupported algorithms
        if !supported_algs.contains(&header.alg) {
            return Err(Error::TokenSignedWithUnsupportedAlgorithm(header.alg));
        }

        let mut validator = jwt::Validation::new(header.alg);
        // TODO: set valid `aud` and `iss` depending on
        // the OpenID configuration fetched from the server
        validator.validate_aud = false;

        // fetch decoding key from the KeyService
        let key = key_service.get_key(
            &header
                .kid
                .ok_or(Error::MissingRequiredHeader("kid".to_string()))?,
        )?;

        let claims = jwt::decode::<T>(&jwt, &key, &validator)
            .map_err(|e| Error::ValidationError(e))?
            .claims;

        Ok(claims)
    }
}

/// Decodes a JWT token based on the current decoding strategy.
impl Decode for DecodingStrategy {
    fn decode<T: DeserializeOwned>(&self, jwt: &str) -> Result<T, Error> {
        match self {
            DecodingStrategy::WithoutValidation => Self::decode_jwt_without_validation(jwt),
            DecodingStrategy::WithValidation {
                key_service,
                supported_algs,
            } => Self::decode_and_validate_jwt(jwt, supported_algs, key_service),
        }
    }
}

/// A wrapper for the key service used to retrieve decoding keys.
///
/// This struct is used to implement the `DependencySupplier` trait, enabling
/// the retrieval of the underlying key service in a thread-safe manner.
pub struct KeyServiceWrapper(Arc<dyn GetKey>);

impl DependencySupplier<KeyServiceWrapper> for KeyServiceWrapper {
    /// Retrieves an instance of `KeyServiceWrapper` from the current instance.
    ///
    /// This method clones the underlying key service and returns a new wrapper
    /// instance encapsulating the cloned service.
    fn get(&self) -> Arc<KeyServiceWrapper> {
        KeyServiceWrapper(Arc::clone(&self.0)).into()
    }
}

/// Converts a string representation of an algorithm to the corresponding `jwt::Algorithm`.
///
/// This function attempts to map the provided algorithm name to a valid JWT
/// algorithm. If the algorithm is unrecognized, it returns an error.
fn string_to_alg(algorithm: &str) -> Result<jwt::Algorithm, Error> {
    match algorithm {
        "HS256" => Ok(jwt::Algorithm::HS256),
        "HS384" => Ok(jwt::Algorithm::HS384),
        "HS512" => Ok(jwt::Algorithm::HS512),
        "ES256" => Ok(jwt::Algorithm::ES256),
        "ES384" => Ok(jwt::Algorithm::ES384),
        "RS256" => Ok(jwt::Algorithm::RS256),
        "RS384" => Ok(jwt::Algorithm::RS384),
        "RS512" => Ok(jwt::Algorithm::RS512),
        "PS256" => Ok(jwt::Algorithm::PS256),
        "PS384" => Ok(jwt::Algorithm::PS384),
        "PS512" => Ok(jwt::Algorithm::PS512),
        "EdDSA" => Ok(jwt::Algorithm::EdDSA),
        _ => Err(Error::UnimplementedAlgorithm(algorithm.into())),
    }
}
