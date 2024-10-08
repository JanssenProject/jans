/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{AccessToken, Error, IdToken, TransactionToken, UserInfoToken};
use jsonwebtoken::{decode, Algorithm, DecodingKey, Validation};

/// Represents the different types of tokens that the system supports.
///
/// This enum is used to distinguish between various token types, each serving
/// a unique role in authentication, authorization, and user management.
/// The `TokenKind` enum is primarily used in token validation to indicate
/// which type of token is being processed.
//
/// # Variants
/// - `AccessToken`: Used for authorizing access to resources.
/// - `IdToken`: Contains user identity claims. Often used to authenticate users in OpenID Connect flows.
/// - `TransactionToken`: Used to track and verify specific transactions, ensuring that the transaction is valid.
/// - `UserInfoToken`: Holds additional user details, often retrieved post-authentication to provide more user information.
pub enum TokenKind {
    /// Authorizes access to protected resources.
    AccessToken,

    /// Contains identity information about the user for authentication purposes.
    IdToken,

    /// Used to track and verify specific transactions.
    TransactionToken,

    /// Stores additional user details retrieved after authentication.
    UserInfoToken,
}

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
    /// - `Err(Box<dyn std::error::Error>)`: If validation fails or an unsupported algorithm is provided,
    ///   an error is returned.
    ///
    /// # Errors
    /// - Returns an error if the token is invalid, expired, or if the provided algorithm is not implemented.
    ///
    /// # TODO
    /// - Implement automatically obtaining the `decoding_key` from provided IDPs.
    /// - Implement custom validation for each `TokenKind`
    pub fn validate(
        token: &str,
        kind: TokenKind,
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
        let token = match kind {
            TokenKind::AccessToken => {
                // TODO: set the audience programatically
                validation.set_audience(&vec!["https://auth.myapp.com"]);

                let data = decode::<AccessToken>(&token, &decoding_key, &validation)
                    .map_err(|e| Error::ValidationError(e))?;
                Token::AccessToken(data.claims)
            },
            TokenKind::IdToken => {
                let data = decode::<IdToken>(&token, &decoding_key, &validation)
                    .map_err(|e| Error::ValidationError(e))?;
                Token::IdToken(data.claims)
            },
            TokenKind::TransactionToken => {
                let data = decode::<TransactionToken>(&token, &decoding_key, &validation)
                    .map_err(|e| Error::ValidationError(e))?;
                Token::TransactionToken(data.claims)
            },
            TokenKind::UserInfoToken => {
                let data = decode::<UserInfoToken>(&token, &decoding_key, &validation)
                    .map_err(|e| Error::ValidationError(e))?;
                Token::UserInfoToken(data.claims)
            },
        };

        Ok(token)
    }
}
