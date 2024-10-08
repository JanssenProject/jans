/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde::Deserialize;

/// Represents the claims contained within a User Info Token.
///
/// A `UserInfoToken` is a type of token that contains user profile information
/// and additional claims. It is typically used in authentication and authorization
/// scenarios to provide details about the authenticated user. This struct is used
/// to deserialize and validate the claims present in the JWT User Info token.
///
/// # Fields
/// - `aud`: The audience for which the token is intended, typically the identifier
///    of the recipient or client that should accept the token.
/// - `birthdate`: The user's birthdate in `YYYY-MM-DD` format.
/// - `email`: The user's email address.
/// - `exp`: The expiration time (in seconds since the Unix epoch) after which the
///    token is no longer valid.
/// - `iat`: The issued-at time (in seconds since the Unix epoch) when the token
///    was created.
/// - `iss`: The issuer of the token, typically a URL representing the authorization
///    server.
/// - `jti`: A unique identifier for the token, often used to prevent token reuse.
/// - `name`: The user's full name.
/// - `phone_number`: The user's phone number.
/// - `role`: A list of roles assigned to the user, indicating their permissions or
///    access level.
/// - `sub`: The subject identifier, a unique identifier for the user in the context
///    of the token issuer.
#[derive(Deserialize)]
pub struct UserInfoToken {
    /// The audience for which the token is intended (typically the client ID).
    pub aud: String,

    /// The user's birthdate in `YYYY-MM-DD` format.
    pub birthdate: String,

    /// The user's email address.
    pub email: String,

    /// The expiration timestamp of the token (in seconds since Unix epoch).
    pub exp: i64,

    /// The issued-at timestamp of the token (in seconds since Unix epoch).
    pub iat: i64,

    /// The issuer of the token, typically an authorization server.
    pub iss: String,

    /// A unique identifier for the token.
    pub jti: String,

    /// The user's full name.
    pub name: String,

    /// The user's phone number.
    pub phone_number: String,

    /// The roles assigned to the user, representing permissions or access levels.
    pub role: Vec<String>,

    /// The subject identifier, a unique identifier for the user in the context of the token issuer.
    pub sub: String,
}
