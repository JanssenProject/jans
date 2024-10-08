/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde::{Deserialize, Serialize};

/// Represents the claims contained within an Access Token.
///
/// An `AccessToken` is a token used to authorize access to protected resources.
/// It typically contains a set of claims that describe the token’s purpose, issuer,
/// and expiration. This struct is used to deserialize and validate the claims
/// present in the JWT access token.
///
/// # Fields
/// - `aud`: The audience for which the token is intended. It typically represents
///    the identifier of the recipient or service that should accept the token.
/// - `exp`: The expiration time (in seconds since the Unix epoch) after which the token is no longer valid.
/// - `iat`: The issued-at time (in seconds since the Unix epoch) when the token was created.
/// - `iss`: The issuer of the token, typically a URL representing the authorization server.
/// - `jti`: A unique identifier for the token, often used to prevent token reuse.
/// - `scope`: The scope of the access granted by the token, typically a space-separated string representing permissions.
#[derive(Deserialize, Serialize, Debug, PartialEq)]
pub struct AccessToken {
    /// The intended recipient of the token, usually a service or API identifier.
    pub aud: String,

    /// The expiration timestamp of the token (in seconds since Unix epoch).
    pub exp: i64,

    /// The issued-at timestamp of the token (in seconds since Unix epoch).
    pub iat: i64,

    /// The issuer of the token, typically an authorization server.
    pub iss: String,

    /// A unique identifier for the token.
    pub jti: String,

    /// The permissions or scope granted by the token, typically a space-separated list.
    pub scope: String,
}
