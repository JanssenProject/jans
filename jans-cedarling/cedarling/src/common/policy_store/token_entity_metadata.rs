// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;

use typed_builder::TypedBuilder;

/// Structure for storing mapping JWT claims to `cedar-policy` custom defined types in the `schema`.
///
/// An optional mapping of claims to their values. Each claim is represented
/// by a key-value pair where the key is the claim name and the value is
/// a `ClaimMapping` struct.
#[derive(Debug, PartialEq, Clone, TypedBuilder)]
pub(crate) struct TokenEntityMetadata {
    /// Indicates if the access token is trusted.
    #[builder(default = true)]
    pub(crate) trusted: bool,
    /// The Cedar entity name that represents this token
    pub(crate) entity_type_name: String,
    /// An optional string representing the principal identifier (e.g., `jti`).
    #[builder(default = default_token_id())]
    pub(crate) token_id: String,
    /// The claims in this Vec will be required on token validation and will be
    /// validated if it is a registered claim listed in [`RFC 7519, Section 4.1`] (<https://datatracker.ietf.org/doc/html/rfc7519#section-4.1>)
    #[builder(default)]
    pub(crate) required_claims: HashSet<String>,
}

pub(crate) const DEFAULT_TKN_ID: &str = "jti";

fn default_token_id() -> String {
    DEFAULT_TKN_ID.to_string()
}

#[cfg(test)]
impl TokenEntityMetadata {
    /// Default access token Metadata
    pub(crate) fn access_token() -> Self {
        Self {
            trusted: true,
            token_id: default_token_id(),
            required_claims: HashSet::from(["iss".into(), "exp".into(), "jti".into()]),
            entity_type_name: "Jans::Access_token".into(),
        }
    }

    /// Default id token Metadata
    pub(crate) fn id_token() -> Self {
        Self {
            trusted: true,
            token_id: default_token_id(),
            required_claims: HashSet::from([
                "iss".into(),
                "sub".into(),
                "aud".into(),
                "exp".into(),
            ]),
            entity_type_name: "Jans::Id_token".into(),
        }
    }

    /// Default userinfo token Metadata
    pub(crate) fn userinfo_token() -> Self {
        Self {
            trusted: true,
            token_id: default_token_id(),
            required_claims: HashSet::from([
                "iss".into(),
                "sub".into(),
                "aud".into(),
                "exp".into(),
            ]),
            entity_type_name: "Jans::Userinfo_token".into(),
        }
    }
}
