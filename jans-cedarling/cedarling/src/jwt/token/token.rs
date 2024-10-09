/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::jwt::claims::{self, Claims};

use super::*;
use jsonwebtoken::{decode, Algorithm, DecodingKey, Validation};

/// Represents various types of supported JWT tokens
pub enum Token {
    /// An access token used to authenticate and authorize access to resources.
    AccessToken(AccessToken),

    /// An identity token containing user identity claims
    IdToken(IdToken),

    /// A transaction token used to verify and track a specific transaction.
    TransactionToken(TransactionToken),

    /// A user information token, which holds additional user details.
    UserInfoToken(UserInfoToken),
}

impl Token {
    /// Validates a given JWT token and returns the corresponding `Token` enum variant.
    ///
    /// This function handles different types of tokens (Access, ID, Transaction, and UserInfo)
    /// and verifies the token against the specified algorithm and decoding key.
    ///
    /// # Arguments
    /// - `token`: A string slice representing the JWT to validate.
    /// - `kind`: The type of token being validated, represented by the `TokenKind` enum.
    /// - `decoding_key`: A byte vector containing the key used to decode the token.
    /// - `algorithm`: The cryptographic algorithm used to sign the token, such as `RS256` or `HS256`.
    ///
    /// # Returns
    /// - `Ok(Token)`: If the token is successfully validated, it returns the appropriate `Token` variant.
    /// - `Err(jwt::error::Error)`: If validation fails or an unsupported algorithm is provided,
    ///   an error is returned.
    ///
    /// # Errors
    /// - Returns an error if the token is invalid, expired, or if the provided algorithm is not implemented.
    ///
    /// # TODO
    /// - Implement automatically obtaining the `decoding_key` from provided IDPs.
    /// - Implement custom validation for each kind of token
    pub fn validate(
        token: &str,
        expected_claims: Claims,
        decoding_key: &Vec<u8>,
        algorithm: Algorithm,
    ) -> Result<Self, Error> {
        // Define validation parameters
        let mut validation = Validation::new(algorithm);
        validation.validate_exp = true; // Ensure that the token hasn't expired

        // Decode key
        let decoding_key = match algorithm {
            Algorithm::RS256 => {
                DecodingKey::from_rsa_pem(decoding_key).map_err(|e| Error::KeyError(e))?
            },
            Algorithm::HS256 => DecodingKey::from_secret(decoding_key),
            _ => return Err(Error::UnsupportedAlgorithm(algorithm)),
        };

        // Decode and validate token
        let token = match expected_claims {
            Claims::AccessToken(expected_claims) => {
                Self::validate_access_token(token, decoding_key, validation, expected_claims)?
            },
            Claims::IdToken(expected_claims) => {
                Self::validate_id_token(token, decoding_key, validation, expected_claims)?
            },
            Claims::TransactionToken(expected_claims) => {
                Self::validate_transaction_token(token, decoding_key, validation, expected_claims)?
            },
            Claims::UserInfoToken(expected_claims) => {
                Self::validate_userinfo_token(token, decoding_key, validation, expected_claims)?
            },
        };

        Ok(token)
    }

    fn validate_access_token(
        token: &str,
        decoding_key: DecodingKey,
        mut validation: Validation,
        expected_claims: claims::AccessToken,
    ) -> Result<Token, Error> {
        // Define validation parameters
        validation.set_required_spec_claims(&access_token::REQUIRED_CLAIMS);
        validation.set_audience(&vec![expected_claims.aud()]);
        validation.set_issuer(&vec![expected_claims.iss()]);

        // Validate required params
        let data = decode::<AccessToken>(&token, &decoding_key, &validation)
            .map_err(|e| Error::ValidationError(e.to_string()))?;

        // Validate custom params
        if data.claims.jti != *expected_claims.jti() {
            return Err(Error::ValidationError("invalid jti".to_string()));
        }

        if data.claims.scope != *expected_claims.scope() {
            return Err(Error::ValidationError("invalid scope".to_string()));
        }

        Ok(Token::AccessToken(data.claims))
    }

    fn validate_id_token(
        token: &str,
        decoding_key: DecodingKey,
        validation: Validation,
        _expected_claims: claims::IdToken,
    ) -> Result<Token, Error> {
        // TODO: Define validation parameters for `ValidateIdToken`

        let data = decode::<IdToken>(&token, &decoding_key, &validation)
            .map_err(|e| Error::ValidationError(e.to_string()))?;

        Ok(Token::IdToken(data.claims))
    }

    fn validate_userinfo_token(
        token: &str,
        decoding_key: DecodingKey,
        validation: Validation,
        _expected_claims: claims::UserinfoToken,
    ) -> Result<Token, Error> {
        // TODO: Define validation parameters for `UserInfoToken`

        let data = decode::<UserInfoToken>(&token, &decoding_key, &validation)
            .map_err(|e| Error::ValidationError(e.to_string()))?;

        Ok(Token::UserInfoToken(data.claims))
    }

    fn validate_transaction_token(
        token: &str,
        decoding_key: DecodingKey,
        validation: Validation,
        _expected_claims: claims::TransactionToken,
    ) -> Result<Token, Error> {
        // TODO: Define validation parameters for `TransactionToken`

        let data = decode::<TransactionToken>(&token, &decoding_key, &validation)
            .map_err(|e| Error::ValidationError(e.to_string()))?;

        Ok(Token::TransactionToken(data.claims))
    }
}
