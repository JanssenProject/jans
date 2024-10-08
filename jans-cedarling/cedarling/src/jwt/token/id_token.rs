/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde::Deserialize;

/// Represents the claims contained within an ID Token.
///
/// An `IdToken` is a type of token that contains identity-related claims about the user.
/// It is typically used in authentication flows (such as OpenID Connect) to verify
/// the identity of the user. This struct is used to deserialize and validate the claims
/// present in the JWT ID token.
///
/// # Fields
/// - `acr`: A list of strings representing the Authentication Context Class Reference,
///    indicating the strength of the authentication.
/// - `amr`: The Authentication Methods Reference, a string that specifies how the user
///    was authenticated (e.g., password, biometric).
/// - `aud`: The audience for which the token is intended. It typically represents the
///    identifier of the recipient or client that should accept the token.
/// - `azp`: The authorized party, which is the client ID authorized to use the token.
/// - `birthdate`: The user's birthdate in `YYYY-MM-DD` format.
/// - `email`: The user's email address.
/// - `exp`: The expiration time (in seconds since the Unix epoch) after which the token is no longer valid.
/// - `iat`: The issued-at time (in seconds since the Unix epoch) when the token was created.
/// - `iss`: The issuer of the token, typically a URL representing the authorization server.
/// - `jti`: A unique identifier for the token, often used to prevent token reuse.
/// - `name`: The user's full name.
/// - `phone_number`: The user's phone number, which is optional and may not always be present.
/// - `role`: A list of roles assigned to the user, indicating their permissions or access level.
/// - `sub`: The subject identifier, a unique identifier for the user in the context of the token issuer.
#[derive(Deserialize)]
pub struct IdToken {
    /// The Authentication Context Class Reference (ACR), indicating the authentication strength.
    pub acr: Vec<String>,

    /// The Authentication Methods Reference (AMR), specifying how the user was authenticated.
    pub amr: String,

    /// The audience for which the token is intended (typically the client ID).
    pub aud: String,

    /// The authorized party, the client ID authorized to use the token.
    pub azp: String,

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

    /// The user's phone number, which is optional and may not always be present.
    pub phone_number: Option<String>,

    /// The roles assigned to the user, representing permissions or access levels.
    pub role: Vec<String>,

    /// The subject identifier, a unique identifier for the user in the context of the token issuer.
    pub sub: String,
}
