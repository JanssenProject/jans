/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

// Represents the required claims for `IdToken`
pub struct IdToken {
    acr: Vec<String>,
    amr: String,
    aud: String,
    azp: String,
    birthdate: String,
    email: String,
    iss: String,
    jti: String,
    name: String,
    phone_number: Option<String>,
    role: Vec<String>,
    sub: String,
}

impl IdToken {
    /// Constructs a new `IdToken` with the required claims.
    ///
    /// # Arguments
    ///
    /// - `acr`: A vector of strings representing the Authentication Context Class Reference (ACR),
    ///   indicating the authentication strength.
    /// - `amr`: A string specifying the Authentication Methods Reference (AMR), describing how the user was authenticated.
    /// - `aud`: A string representing the audience for which the token is intended (typically the client ID).
    /// - `azp`: A string representing the authorized party, typically the client ID authorized to use the token.
    /// - `birthdate`: A string representing the user's birthdate in `YYYY-MM-DD` format.
    /// - `email`: A string containing the user's email address.
    /// - `iss`: A string representing the issuer of the token (usually the authorization server).
    /// - `jti`: A string representing the unique identifier for the token.
    /// - `name`: A string representing the user's full name.
    /// - `phone_number`: An optional string representing the user's phone number.
    /// - `role`: A vector of strings representing the roles assigned to the user, defining permissions or access levels.
    /// - `sub`: A string representing the subject identifier, a unique identifier for the user in the context of the issuer.
    ///
    /// # Returns
    ///
    /// A new instance of `IdToken` with the specified claims.
    pub fn new(
        acr: Vec<String>,
        amr: String,
        aud: String,
        azp: String,
        birthdate: String,
        email: String,
        iss: String,
        jti: String,
        name: String,
        phone_number: Option<String>,
        role: Vec<String>,
        sub: String,
    ) -> Self {
        IdToken {
            acr,
            amr,
            aud,
            azp,
            birthdate,
            email,
            iss,
            jti,
            name,
            phone_number,
            role,
            sub,
        }
    }

    /// Returns a reference to the `acr` (Authentication Context Class Reference).
    pub fn acr(&self) -> &Vec<String> {
        &self.acr
    }

    /// Returns a reference to the `amr` (Authentication Methods Reference).
    pub fn amr(&self) -> &String {
        &self.amr
    }

    /// Returns a reference to the `aud` (Audience).
    pub fn aud(&self) -> &String {
        &self.aud
    }

    /// Returns a reference to the `azp` (Authorized Party).
    pub fn azp(&self) -> &String {
        &self.azp
    }

    /// Returns a reference to the `birthdate`.
    pub fn birthdate(&self) -> &String {
        &self.birthdate
    }

    /// Returns a reference to the `email`.
    pub fn email(&self) -> &String {
        &self.email
    }

    /// Returns a reference to the `iss` (Issuer).
    pub fn iss(&self) -> &String {
        &self.iss
    }

    /// Returns a reference to the `jti` (Token ID).
    pub fn jti(&self) -> &String {
        &self.jti
    }

    /// Returns a reference to the `name`.
    pub fn name(&self) -> &String {
        &self.name
    }

    /// Returns an optional reference to the `phone_number`.
    pub fn phone_number(&self) -> &Option<String> {
        &self.phone_number
    }

    /// Returns a reference to the `role` (User roles).
    pub fn role(&self) -> &Vec<String> {
        &self.role
    }

    /// Returns a reference to the `sub` (Subject Identifier).
    pub fn sub(&self) -> &String {
        &self.sub
    }
}
