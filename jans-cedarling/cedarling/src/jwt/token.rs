/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::authz;
use serde::Deserialize;
use serde_json::Value;
use std::collections::HashMap;

/// Represents a JWT token with standard claims.
///
/// The `Token` struct includes fields for the issuer (`iss`), audience (`aud`),
/// subject (`sub`), and any additional claims represented as a `HashMap`.
#[derive(Deserialize, Debug, PartialEq)]
pub struct Token {
    pub iss: String,
    pub aud: String,
    pub sub: String,
    #[serde(flatten)]
    claims: HashMap<String, Value>,
}

impl Token {
    /// Merges the standard claims into the existing claims map.
    fn merge_claims(mut self) -> HashMap<String, Value> {
        self.claims
            .insert("iss".to_string(), Value::String(self.iss));
        self.claims
            .insert("aud".to_string(), Value::String(self.aud));
        self.claims
            .insert("sub".to_string(), Value::String(self.sub));
        self.claims
    }
}

/// A trait that defines a method to retrieve claims from a token.
pub trait Claims {
    /// Retrieves the claims from the token as a `HashMap`.
    fn claims(self) -> HashMap<String, Value>;
}

// Implement `JsonWebToken` for each token type by using the `JsonWebToken`'s functionality
macro_rules! impl_jwt_for_token {
    ($token_type:ident, $output_type:ident) => {
        /// Represents a JWT token type.
        ///
        /// The `$token_type` struct encapsulates a `Token` instance and provides
        /// the ability to retrieve claims and convert to a corresponding output type.
        #[derive(Deserialize, Debug, PartialEq)]
        pub struct $token_type(pub Token);

        impl Claims for $token_type {
            /// Returns the claims for the token as a `HashMap`.
            ///
            /// This implementation calls the `merge_claims` method on the inner `Token`
            /// struct to combine standard claims with any additional claims.
            fn claims(self) -> HashMap<String, Value> {
                self.0.merge_claims()
            }
        }

        /// Converts the token type into an authorization-specific output type.
        ///
        /// This implementation provides a conversion from the `$token_type` to
        /// the `authz::$output_type`, encapsulating the claims as a token payload.
        impl From<$token_type> for authz::$output_type {
            fn from(token: $token_type) -> Self {
                authz::$output_type(authz::TokenPayload {
                    payload: token.claims(),
                })
            }
        }
    };
}

// Implement the macro for specific token types.
impl_jwt_for_token!(AccessToken, AccessTokenData);
impl_jwt_for_token!(IdToken, IdTokenData);
impl_jwt_for_token!(UserinfoToken, UserInfoTokenData);
