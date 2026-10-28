// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;

use typed_builder::TypedBuilder;

/// Validation settings for one JWT token type, held by its
/// [`TokenTypeKind::Jwt`](super::TokenTypeKind::Jwt) entry.
#[derive(Debug, PartialEq, Clone, TypedBuilder)]
pub(crate) struct TokenEntityMetadata {
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
            token_id: default_token_id(),
            required_claims: HashSet::from(["iss".into(), "exp".into(), "jti".into()]),
        }
    }

    /// Default id token Metadata
    pub(crate) fn id_token() -> Self {
        Self {
            token_id: default_token_id(),
            required_claims: HashSet::from([
                "iss".into(),
                "sub".into(),
                "aud".into(),
                "exp".into(),
            ]),
        }
    }

    /// Default userinfo token Metadata
    pub(crate) fn userinfo_token() -> Self {
        Self {
            token_id: default_token_id(),
            required_claims: HashSet::from([
                "iss".into(),
                "sub".into(),
                "aud".into(),
                "exp".into(),
            ]),
        }
    }
}
