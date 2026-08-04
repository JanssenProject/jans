// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::{issuer_utils::IssClaim, policy_store::TrustedIssuer};
use serde::Deserialize;
use serde_json::Value;
use std::{collections::HashMap, sync::Arc};

/// Identifies where a validated [`Token`] came from.
///
/// A token is either backed by a JWT [`TrustedIssuer`] (the standard path) or by
/// a custom issuer resolved by a [`CustomTokenProcessor`](crate::CustomTokenProcessor).
#[derive(Debug, PartialEq, Clone)]
pub(crate) enum TokenIssuer {
    /// A JWT trusted issuer from the policy store.
    Jwt(Arc<TrustedIssuer>),
    /// A custom (non-JWT) issuer.
    Custom(CustomTokenIssuerMeta),
}

/// Issuer metadata for a custom (non-JWT) token, carried on the built [`Token`]
/// so the entity builder can resolve its entity type, id, and context key without
/// a [`TrustedIssuer`].
#[derive(Debug, PartialEq, Clone)]
pub(crate) struct CustomTokenIssuerMeta {
    /// Sanitized issuer id. Used directly for the `context.tokens.{issuer}_{type}`
    /// key, so it is expected to already be collision-checked at init.
    pub issuer_id: String,
    /// Cedar entity type name for this token, if configured on the custom issuer.
    pub entity_type_name: Option<String>,
    /// The id used as the token entity id (supplied by the processor).
    pub token_id: String,
}

/// Structure representing a validated token, used to derive a Cedar token entity.
/// The `iss` field carries the resolved issuer; a JWT-backed token references a
/// `TrustedIssuer`, a custom token carries [`CustomTokenIssuerMeta`].
#[derive(Debug, PartialEq, Clone)]
pub(crate) struct Token {
    pub name: String,
    pub iss: Option<TokenIssuer>,
    pub(crate) claims: TokenClaims,
}

impl Token {
    pub(crate) fn new(name: &str, claims: TokenClaims, iss: Option<TokenIssuer>) -> Token {
        Self {
            name: name.to_string(),
            iss,
            claims,
        }
    }

    pub(crate) fn get_claim(&self, name: &str) -> Option<TokenClaim<'_>> {
        self.claims.get_claim(name)
    }

    pub(crate) fn get_claim_val(&self, name: &str) -> Option<&Value> {
        self.claims.claims.get(name)
    }

    pub(crate) fn logging_info(&self, claim: &str) -> HashMap<String, serde_json::Value> {
        self.claims.logging_info(claim)
    }

    pub(crate) fn claims_value(&self) -> &HashMap<String, Value> {
        &self.claims.claims
    }

    /// Extract normalized issuer from a token.
    pub(crate) fn extract_normalized_issuer(&self) -> Option<IssClaim> {
        match &self.iss {
            // From TrustedIssuer reference (preferred)
            Some(TokenIssuer::Jwt(trusted_issuer)) => Some(trusted_issuer.iss_claim()),
            // Custom issuer: the sanitized issuer id is the normalized issuer.
            Some(TokenIssuer::Custom(meta)) => Some(IssClaim::new(meta.issuer_id.as_str())),
            // Fallback: from token claims
            None => self
                .claims
                .get_claim("iss")
                .and_then(|claim| claim.value().as_str().map(IssClaim::new)),
        }
    }
}

#[derive(Debug, PartialEq, Default, Deserialize, Clone)]
pub(crate) struct TokenClaims {
    #[serde(flatten)]
    claims: HashMap<String, Value>,
}

impl From<HashMap<String, Value>> for TokenClaims {
    fn from(claims: HashMap<String, Value>) -> Self {
        Self { claims }
    }
}

impl TryFrom<Value> for TokenClaims {
    type Error = &'static str;

    fn try_from(claims: Value) -> Result<Self, Self::Error> {
        match claims {
            Value::Object(map) => Ok(Self {
                claims: map.into_iter().collect(),
            }),
            _ => Err("expected a JSON object for TokenClaims"),
        }
    }
}

impl TokenClaims {
    pub(crate) fn get_claim(&self, name: &str) -> Option<TokenClaim<'_>> {
        self.claims.get(name).map(|value| TokenClaim { value })
    }

    pub(crate) fn logging_info(&self, claim: &str) -> HashMap<String, serde_json::Value> {
        let claim = if claim.is_empty() { "jti" } else { claim };

        self.claims
            .get(claim)
            .map(|value| HashMap::from([(claim.to_string(), value.clone())]))
            .unwrap_or_default()
    }
}

pub(crate) struct TokenClaim<'a> {
    value: &'a serde_json::Value,
}

impl TokenClaim<'_> {
    pub(crate) fn value(&self) -> &Value {
        self.value
    }
}
