// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::entity_id_getters::{EntityIdSrc, get_first_valid_entity_id};
use super::{
    BuildEntityError, BuiltEntities, DEFAULT_ENTITY_TYPE_NAME, EntityBuilder,
    default_tkn_entity_name,
};
use crate::common::default_entities::DefaultEntities;
use crate::common::issuer_utils::IssClaim;
use crate::common::policy_store::token_entity_metadata::DEFAULT_TKN_ID;
use crate::entity_builder::{BuildAttrsErrorVec, schema};
use crate::jwt::Token;
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LogEntry, LogLevel};
use cedar_policy::{Entity, EntityId, EntityTypeName, EntityUid, RestrictedExpression};
use serde_json::Value;
use std::collections::{HashMap, HashSet};
use std::str::FromStr;
use std::sync::Arc;

/// Errors that can occur during multi-issuer entity building
#[derive(Debug, thiserror::Error)]
pub enum MultiIssuerEntityError {
    #[error("Missing issuer claim in JWT")]
    MissingIssuer,

    #[error("Missing exp claim in JWT")]
    MissingExpClaim,

    #[error("Invalid entity UID: {0}")]
    InvalidEntityUid(String),

    #[error("Entity creation failed: {0}")]
    EntityCreationFailed(String),

    #[error("No valid tokens found")]
    NoValidTokens,

    #[error("Could not create cedar uid for trusted issuer: {0}")]
    BuildTrustedIssuerUid(#[from] BuildEntityError),

    #[error("Could not build entity attributes: {0}")]
    BuildAttrs(#[from] BuildAttrsErrorVec),
}

/// Sanitize issuer name for Cedar compatibility
fn sanitize_issuer_name(name: &str) -> String {
    name.replace(['.', ' ', '-'], "_").to_lowercase()
}

/// Simplify token type for Cedar compatibility
fn simplify_token_type(mapping: &str) -> String {
    // Split by namespace separator and use the last part
    // Keep underscores in token type names as specified in design
    mapping.split("::").last().unwrap_or(mapping).to_lowercase()
}

/// Create a Set of String from a single string value
fn create_string_set(value: &str) -> RestrictedExpression {
    RestrictedExpression::new_set(vec![RestrictedExpression::new_string(value.to_string())])
}

/// Create a Set of String from an array of strings
fn create_string_set_array(values: &[String]) -> RestrictedExpression {
    RestrictedExpression::new_set(
        values
            .iter()
            .map(|s| RestrictedExpression::new_string(s.clone()))
            .collect::<Vec<_>>(),
    )
}

const RESERVED_CLAIMS: [&str; 3] = ["iss", "jti", "exp"];

/// Add reserved claims to entity attributes based on schema shape.
fn add_reserved_claims(
    attrs: &mut HashMap<String, RestrictedExpression>,
    token: &Token,
    entity_id: &str,
    attrs_shape_opt: Option<&HashMap<smol_str::SmolStr, schema::AttrsShape>>,
    validated_at_ts: i64,
) -> Result<(), MultiIssuerEntityError> {
    const TOKEN_TYPE: &str = "token_type";
    const JTI_CLAIM: &str = "jti";
    const ISS_CLAIM: &str = "iss";
    const EXP_CLAIM: &str = "exp";
    const VALIDATED_AT_CLAIM: &str = "validated_at";

    if let Some(attrs_shape) = attrs_shape_opt {
        // add token_type claim
        if attrs_shape.contains_key(TOKEN_TYPE) {
            attrs.insert(
                TOKEN_TYPE.to_string(),
                RestrictedExpression::new_string(token.name.clone()),
            );
        }

        // add jti claim
        if attrs_shape.contains_key(JTI_CLAIM) {
            attrs.insert(
                JTI_CLAIM.to_string(),
                RestrictedExpression::new_string(entity_id.to_string()),
            );
        }

        // add iss claim
        if let Some(shape) = attrs_shape.get(ISS_CLAIM) {
            const UNDEFINED_ISSUER: &str = "undefined";

            if let Some(token_iss) = &token.iss {
                let issuer = token.extract_normalized_issuer()
                    // it should never be None here since token iss exists
                    .unwrap_or_else(|| IssClaim::new(UNDEFINED_ISSUER));

                attrs.insert(
                    ISS_CLAIM.to_string(),
                    RestrictedExpression::new_entity_uid(EntityBuilder::trusted_issuer_cedar_uid(
                        &token_iss.name,
                        &issuer,
                    )?),
                );
            } else if shape.is_required() {
                // iss is required but token has no issuer (in trusted issuer)
                attrs.insert(
                    "iss".to_string(),
                    RestrictedExpression::new_string(
                        token
                            .get_claim(ISS_CLAIM)
                            .and_then(|v| v.value().as_str().map(str::to_string))
                            .unwrap_or_else(|| UNDEFINED_ISSUER.to_string()),
                    ),
                );
            }
        }

        // add exp claim
        if let Some(shape) = attrs_shape.get(EXP_CLAIM) {
            if let Some(exp) = token
                .get_claim_val(EXP_CLAIM)
                .and_then(serde_json::Value::as_i64)
            {
                attrs.insert(EXP_CLAIM.to_string(), RestrictedExpression::new_long(exp));
            } else if shape.is_required() {
                // exp is required but missing in token
                return Err(MultiIssuerEntityError::MissingExpClaim);
            }
        }

        // add validated_at claim
        if attrs_shape.contains_key(VALIDATED_AT_CLAIM) {
            attrs.insert(
                VALIDATED_AT_CLAIM.to_string(),
                RestrictedExpression::new_long(validated_at_ts),
            );
        }
    } else {
        // No schema shape provided, add all reserved claims as is

        attrs.insert(
            TOKEN_TYPE.to_string(),
            RestrictedExpression::new_string(token.name.clone()),
        );

        attrs.insert(
            JTI_CLAIM.to_string(),
            RestrictedExpression::new_string(entity_id.to_string()),
        );

        if let Some(token_iss) = &token.iss {
            let issuer = token
                .extract_normalized_issuer()
                .ok_or(MultiIssuerEntityError::MissingIssuer)?;

            attrs.insert(
                ISS_CLAIM.to_string(),
                RestrictedExpression::new_entity_uid(EntityBuilder::trusted_issuer_cedar_uid(
                    &token_iss.name,
                    &issuer,
                )?),
            );
        }

        if let Some(exp) = token
            .get_claim_val(EXP_CLAIM)
            .and_then(serde_json::Value::as_i64)
        {
            attrs.insert(EXP_CLAIM.to_string(), RestrictedExpression::new_long(exp));
        }

        attrs.insert(
            VALIDATED_AT_CLAIM.to_string(),
            RestrictedExpression::new_long(validated_at_ts),
        );
    }

    Ok(())
}

/// Convert a claim value to a Set of String
fn convert_claim_to_string_set(value: &Value) -> RestrictedExpression {
    match value {
        Value::String(s) => create_string_set(s),
        Value::Number(n) => create_string_set(&n.to_string()),
        Value::Bool(b) => create_string_set(&b.to_string()),
        Value::Array(arr) => {
            let string_values: Vec<String> = arr
                .iter()
                .map(|v| match v {
                    Value::String(s) => s.clone(),
                    Value::Number(n) => n.to_string(),
                    Value::Bool(b) => b.to_string(),
                    _ => v.to_string(),
                })
                .collect();
            create_string_set_array(&string_values)
        },
        _ => create_string_set(&value.to_string()),
    }
}

/// Determine the entity type for a token dynamically
fn determine_token_entity_type(token: &Token) -> String {
    if let Some(issuer) = token.iss.as_ref()
        && let Some(metadata) = issuer.token_metadata.get(&token.name)
    {
        return metadata.entity_type_name.clone();
    }

    if token.name.contains("::") {
        return token.name.clone();
    }

    if let Some(default_type) = default_tkn_entity_name(&token.name) {
        return default_type.to_string();
    }

    DEFAULT_ENTITY_TYPE_NAME.to_string()
}

/// Resource-independent multi-issuer entities produced by
/// [`EntityBuilder::build_multi_issuer_setup_entities`]. Built once per
/// authorization call (single-item or batch) and combined with a per-item
/// resource entity by the caller.
#[derive(Debug, Clone)]
pub(crate) struct MultiIssuerSetupEntities {
    pub tokens: HashMap<String, Entity>,
    pub issuers: HashSet<Entity>,
    pub default_entities: DefaultEntities,
}

impl EntityBuilder {
    /// Build the resource-independent multi-issuer entities (tokens + issuers +
    /// default entities). Used by both single-item and batch multi-issuer
    /// authorization paths — the caller pairs the result with a per-item
    /// [`Self::build_resource_entity`] call to complete each decision.
    pub(crate) fn build_multi_issuer_setup_entities(
        &self,
        tokens: &HashMap<String, Arc<Token>>,
        log_service: &impl LogWriter,
    ) -> Result<MultiIssuerSetupEntities, MultiIssuerEntityError> {
        let mut built_entities = BuiltEntities::from(&self.iss_entities);

        let mut token_entities = HashMap::new();
        for (token_name, token) in tokens {
            match self.build_single_token_entity(token, &built_entities) {
                Ok(entity) => match self.generate_entity_key(token_name, token) {
                    Ok(entity_key) => {
                        built_entities.insert(&entity.uid());
                        token_entities.insert(entity_key, entity);
                    },
                    Err(e) => {
                        log_service.log_any(
                            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                                LogLevel::ERROR,
                                None,
                            ))
                            .set_message(format!(
                                "Failed to generate entity key for token '{token_name}'"
                            ))
                            .set_error(e.to_string()),
                        );
                    },
                },
                Err(e) => {
                    log_service.log_any(
                        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                            LogLevel::ERROR,
                            None,
                        ))
                        .set_message(format!(
                            "Failed to build token entity for token '{token_name}'"
                        ))
                        .set_error(e.to_string()),
                    );
                },
            }
        }

        if token_entities.is_empty() {
            log_service.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::ERROR,
                    None,
                ))
                .set_message("No valid tokens found for multi-issuer authorization".to_string())
                .set_error("All tokens failed validation or entity building".to_string()),
            );
            return Err(MultiIssuerEntityError::NoValidTokens);
        }

        let issuers = self.iss_entities.values().cloned().collect();

        Ok(MultiIssuerSetupEntities {
            tokens: token_entities,
            issuers,
            default_entities: self.default_entities.clone(),
        })
    }

    /// Build a single token entity from a validated JWT
    fn build_single_token_entity(
        &self,
        token: &Token,
        built_entities: &BuiltEntities,
    ) -> Result<Entity, MultiIssuerEntityError> {
        // Determine entity type name using the same logic as regular entity builder
        let entity_type = determine_token_entity_type(token);

        // Resolve token_id from the trusted issuer's token_metadata config,
        // falling back to DEFAULT_TKN_ID when the issuer or metadata entry is not found.
        let token_id_claim: &str = token
            .iss
            .as_deref()
            .and_then(|iss| iss.token_metadata.get(&token.name))
            .map_or(DEFAULT_TKN_ID, |m| m.token_id.as_str());

        let entity_id_srcs = [EntityIdSrc::Token {
            token,
            claim: token_id_claim,
        }];
        let entity_id = get_first_valid_entity_id(&entity_id_srcs)
            .map_err(|e| MultiIssuerEntityError::InvalidEntityUid(e.to_string()))?
            .to_string();

        // Get attribute shape from schema if available
        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|schema| schema.get_entity_shape(&entity_type));

        let claims = token.claims_value();

        let validated_at_ts = chrono::Utc::now().timestamp();

        // Build entity attributes using the same logic as regular entity builder.
        //
        // The closure below is a **read-only data source** — it supplies raw
        // claim values to the schema-driven builder.  It MUST NOT pre-resolve
        // reserved claims (`jti`, `iss`, `exp`); that is the sole responsibility
        // of `add_reserved_claims` below.
        //
        // Schema path: borrow claims from the token and overlay the two
        // synthetic entries (`token_type`, `validated_at`) via the closure,
        // avoiding a full clone of every claim into a temporary HashMap.
        // No-schema path: fall back to the existing iter-all flow.
        let mut attrs = if let Some(shape) = attrs_shape {
            let token_type_val = Value::String(token.name.clone());
            let validated_at_val = Value::Number(validated_at_ts.into());
            // `jti` may be required by the schema but absent from the JWT
            // claims (e.g. when token_id is sourced from `sub`).  The
            // schema-path builder fails hard on missing required attrs,
            // so we fall back to `entity_id` here.  `add_reserved_claims`
            // overwrites the final value anyway.
            let jti_val = Value::String(entity_id.clone());
            super::build_entity_attrs::build_entity_attrs_with_shape_lookup(
                |name| match name {
                    "token_type" => Some(&token_type_val),
                    "validated_at" => Some(&validated_at_val),
                    "jti" => claims.get("jti").or(Some(&jti_val)),
                    other => claims.get(other),
                },
                built_entities,
                shape,
            )?
        } else {
            // Rare path: no schema. Materialize the merged map only here.
            let mut all_claims = HashMap::with_capacity(claims.len() + 2);
            all_claims.extend(claims.iter().map(|(k, v)| (k.clone(), v.clone())));
            all_claims.insert("token_type".to_string(), Value::String(token.name.clone()));
            all_claims.insert(
                "validated_at".to_string(),
                Value::Number(validated_at_ts.into()),
            );
            super::build_entity_attrs::build_entity_attrs(&all_claims, built_entities, None)?
        };

        // ── Reserved-claims overwrite ──────────────────────────────────
        // `add_reserved_claims` is the **single authority** that injects
        // `token_type`, `jti`, `iss`, `exp`, and `validated_at` into `attrs`
        // with their final, correctly-typed values.  Whatever the schema path
        // (or no-schema path) placed in `attrs` for these keys gets overwritten
        // here — this is by design.
        //
        // If you change the closure above to pre-resolve any of these keys
        // you will create dead work and confuse future readers: the value
        // will be computed twice and the second result (here) wins.
        add_reserved_claims(&mut attrs, token, &entity_id, attrs_shape, validated_at_ts)?;

        // Create entity tags for non-reserved JWT claims.
        let mut tags = HashMap::new();
        for (claim_key, claim_value) in token.claims_value() {
            if RESERVED_CLAIMS.contains(&claim_key.as_str()) {
                continue;
            }
            let value = convert_claim_to_string_set(claim_value);
            tags.insert(claim_key.clone(), value);
        }

        // Create the Cedar entity using the existing build_cedar_entity function
        // Note: build_cedar_entity doesn't support tags, so we need to use Entity::new_with_tags directly
        // but we can still reuse the UID creation logic
        let entity_type_name = EntityTypeName::from_str(&entity_type)
            .map_err(|e| MultiIssuerEntityError::InvalidEntityUid(e.to_string()))?;
        // EntityId::from_str returns Result<_, Infallible>, so parsing never fails
        let entity_id = EntityId::from_str(&entity_id).unwrap_or_else(|e| match e {});

        let uid = EntityUid::from_type_name_and_id(entity_type_name, entity_id);

        let entity = Entity::new_with_tags(uid, attrs, HashSet::new(), tags)
            .map_err(|e| MultiIssuerEntityError::EntityCreationFailed(e.to_string()))?;

        Ok(entity)
    }

    /// Generate a unique key for the token entity
    fn generate_entity_key(
        &self,
        token_name: &str,
        token: &Token,
    ) -> Result<String, MultiIssuerEntityError> {
        let issuer = token
            .extract_normalized_issuer()
            .ok_or(MultiIssuerEntityError::MissingIssuer)?;

        let issuer_simplified = self.resolve_issuer_name(&issuer);
        let token_type_simplified = simplify_token_type(token_name);

        Ok(format!("{issuer_simplified}_{token_type_simplified}"))
    }

    /// Resolve issuer name using trusted issuer metadata or fallback to hostname
    fn resolve_issuer_name(&self, issuer: &IssClaim) -> String {
        // First, try to find the issuer in trusted issuer metadata
        if let Some(trusted_issuer) = self.issuers_index.find(issuer) {
            return sanitize_issuer_name(&trusted_issuer.name);
        }

        // Fallback to hostname from JWT iss claim
        let issuer_str = issuer.as_str();
        let hostname = issuer_str
            .replace("https://", "")
            .replace("http://", "")
            .split('/')
            .next()
            .unwrap_or(issuer_str)
            .split(':')
            .next()
            .unwrap_or(issuer_str)
            .to_string();

        sanitize_issuer_name(&hostname)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::default_entities::DefaultEntities;
    use crate::common::policy_store::TrustedIssuer;
    use crate::common::policy_store::token_entity_metadata::TokenEntityMetadata;
    use crate::entity_builder::TrustedIssuerIndex;
    use crate::jwt::{Token, TokenClaims};
    use crate::log::NopLogger;
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;
    use url::Url;

    fn create_test_entity_builder() -> EntityBuilder {
        let mut trusted_issuers = HashMap::new();

        // Add Acme issuer
        let acme_issuer = TrustedIssuer::new(
            "Acme".to_string(),
            "Acme Corporation".to_string(),
            Url::parse("https://idp.acme.com/auth").unwrap(),
            HashMap::new(),
        );
        trusted_issuers.insert("acme".to_string(), acme_issuer);

        // Add Dolphin issuer
        let dolphin_issuer = TrustedIssuer::new(
            "Dolphin".to_string(),
            "Dolphin Sea Services".to_string(),
            Url::parse("https://idp.dolphin.sea/auth").unwrap(),
            HashMap::new(),
        );
        trusted_issuers.insert("dolphin".to_string(), dolphin_issuer);

        // Add Microsoft issuer
        let microsoft_issuer = TrustedIssuer::new(
            "Microsoft".to_string(),
            "Microsoft Azure AD".to_string(),
            Url::parse("https://login.microsoftonline.com/tenant").unwrap(),
            HashMap::new(),
        );
        trusted_issuers.insert("microsoft".to_string(), microsoft_issuer);

        // Add Company issuer
        let company_issuer = TrustedIssuer::new(
            "Company".to_string(),
            "Company Internal Auth".to_string(),
            Url::parse("https://auth.company.internal:8443/oauth").unwrap(),
            HashMap::new(),
        );
        trusted_issuers.insert("company".to_string(), company_issuer);

        EntityBuilder::new(
            TrustedIssuerIndex::new(&trusted_issuers, None),
            None,
            DefaultEntities::default(),
        )
        .unwrap()
    }

    fn create_test_token(
        issuer: &str,
        jti: &str,
        claims: HashMap<String, Value>,
        builder: &EntityBuilder,
    ) -> Token {
        let mut all_claims = claims;
        all_claims.insert("iss".to_string(), json!(issuer));
        all_claims.insert("jti".to_string(), json!(jti));
        all_claims.insert(
            "exp".to_string(),
            json!(chrono::Utc::now().timestamp() + 3600),
        );

        let trusted_issuer = builder.find_trusted_issuer_by_iss(issuer);

        let token_claims = TokenClaims::from(all_claims);
        Token::new("Jans::Access_Token", token_claims, trusted_issuer)
    }

    #[test]
    fn test_issuer_name_resolution_with_trusted_metadata() {
        let builder = create_test_entity_builder();

        // Test with trusted issuer metadata
        let result = builder.resolve_issuer_name(&IssClaim::new("https://idp.acme.com/auth"));
        assert_eq!(result, "acme");

        let result = builder.resolve_issuer_name(&IssClaim::new("https://idp.dolphin.sea/auth"));
        assert_eq!(result, "dolphin");

        let result =
            builder.resolve_issuer_name(&IssClaim::new("https://login.microsoftonline.com/tenant"));
        assert_eq!(result, "microsoft");
    }

    #[test]
    fn test_issuer_name_resolution_fallback_to_hostname() {
        let builder = create_test_entity_builder();

        // Test fallback to hostname for unknown issuer
        let result = builder.resolve_issuer_name(&IssClaim::new("https://unknown.issuer.com/auth"));
        assert_eq!(result, "unknown_issuer_com");
    }

    #[test]
    fn test_token_type_simplification() {
        // Test various token type mappings
        assert_eq!(simplify_token_type("Jans::Access_Token"), "access_token");
        assert_eq!(simplify_token_type("Jans::Id_Token"), "id_token");
        assert_eq!(simplify_token_type("Acme::DolphinToken"), "dolphintoken");
        assert_eq!(
            simplify_token_type("Custom::Employee_Token"),
            "employee_token"
        );
        assert_eq!(simplify_token_type("SimpleToken"), "simpletoken");
    }

    #[test]
    fn test_entity_key_generation() {
        let builder = create_test_entity_builder();

        let mut claims = HashMap::new();
        claims.insert("sub".to_string(), json!("user123"));
        let token = create_test_token("https://idp.acme.com/auth", "token123", claims, &builder);

        let key = builder
            .generate_entity_key("Jans::Access_Token", &token)
            .unwrap();
        assert_eq!(key, "acme_access_token");
    }

    #[test]
    fn test_entity_key_generation_with_unknown_issuer() {
        let builder = create_test_entity_builder();

        let mut claims = HashMap::new();
        claims.insert("sub".to_string(), json!("user123"));
        let token = create_test_token(
            "https://unknown.issuer.com/auth",
            "token123",
            claims,
            &builder,
        );

        let key = builder
            .generate_entity_key("Custom::Employee_Token", &token)
            .unwrap();
        assert_eq!(key, "unknown_issuer_com_employee_token");
    }

    #[test]
    fn test_multiple_tokens_same_issuer_different_names() {
        let builder = create_test_entity_builder();

        let mut tokens = HashMap::new();

        let mut claims1 = HashMap::new();
        claims1.insert("sub".to_string(), json!("user1"));
        let token_one = create_test_token("https://idp.acme.com/auth", "token1", claims1, &builder);
        tokens.insert("Jans::Access_Token".to_string(), token_one);

        let mut claims2 = HashMap::new();
        claims2.insert("sub".to_string(), json!("user2"));
        let token_two = create_test_token("https://idp.acme.com/auth", "token2", claims2, &builder);
        tokens.insert("Jans::Access_Token2".to_string(), token_two);

        let tokens: HashMap<String, Arc<Token>> =
            tokens.into_iter().map(|(k, v)| (k, Arc::new(v))).collect();

        let result =
            builder.build_multi_issuer_setup_entities(&tokens, &NopLogger);
        assert!(result.is_ok());

        let entities_data = result.unwrap();
        assert_eq!(entities_data.tokens.len(), 2);
        assert!(entities_data.tokens.contains_key("acme_access_token"));
        assert!(entities_data.tokens.contains_key("acme_access_token2"));
    }

    #[test]
    fn test_multiple_tokens_different_issuers_and_types() {
        let builder = create_test_entity_builder();

        let mut tokens = HashMap::new();

        // Token from Acme issuer
        let mut claims1 = HashMap::new();
        claims1.insert("sub".to_string(), json!("user1"));
        let token_one = create_test_token("https://idp.acme.com/auth", "token1", claims1, &builder);
        tokens.insert("Jans::Access_Token".to_string(), token_one);

        // Token from Dolphin issuer
        let mut claims2 = HashMap::new();
        claims2.insert("sub".to_string(), json!("user2"));
        let token_two =
            create_test_token("https://idp.dolphin.sea/auth", "token2", claims2, &builder);
        tokens.insert("Acme::DolphinToken".to_string(), token_two);

        // Token from same issuer but different type
        let mut claims3 = HashMap::new();
        claims3.insert("sub".to_string(), json!("user3"));
        claims3.insert("iss".to_string(), json!("https://idp.acme.com/auth"));
        claims3.insert("jti".to_string(), json!("token3"));
        claims3.insert(
            "exp".to_string(),
            json!(chrono::Utc::now().timestamp() + 3600),
        );
        let token_claims3 = TokenClaims::from(claims3);
        let token_three = Token::new("Jans::Id_Token", token_claims3, None);
        tokens.insert("Jans::Id_Token".to_string(), token_three);

        let tokens: HashMap<String, Arc<Token>> =
            tokens.into_iter().map(|(k, v)| (k, Arc::new(v))).collect();

        let result =
            builder.build_multi_issuer_setup_entities(&tokens, &NopLogger);
        assert!(result.is_ok());

        let entities_data = result.unwrap();
        assert_eq!(entities_data.tokens.len(), 3);
        assert!(entities_data.tokens.contains_key("acme_access_token"));
        assert!(entities_data.tokens.contains_key("dolphin_dolphintoken"));
        assert!(entities_data.tokens.contains_key("acme_id_token"));
    }

    #[test]
    fn test_token_entity_structure() {
        let builder = create_test_entity_builder();

        let mut claims = HashMap::new();
        claims.insert("sub".to_string(), json!("user123"));
        claims.insert("scope".to_string(), json!(["read:profile", "write:data"]));
        claims.insert("aud".to_string(), json!("my-client"));
        let token = create_test_token("https://idp.acme.com/auth", "token123", claims, &builder);

        let built_entities = BuiltEntities::from(&builder.iss_entities);
        let entity = builder
            .build_single_token_entity(&token, &built_entities)
            .unwrap();

        // Check entity type - should match the token name
        assert_eq!(entity.uid().type_name().to_string(), "Jans::Access_Token");

        // Check core attributes exist
        assert!(entity.attr("token_type").is_some());
        assert!(entity.attr("jti").is_some());
        assert!(entity.attr("iss").is_some());
        assert!(entity.attr("validated_at").is_some());
        assert!(entity.attr("exp").is_some());

        // Check JWT claims are present as tags
        assert!(entity.tag("sub").is_some());
        assert!(entity.tag("scope").is_some());
        assert!(entity.tag("aud").is_some());
    }

    #[test]
    fn test_missing_jti_claim_error() {
        let builder = create_test_entity_builder();

        let mut claims = HashMap::new();
        claims.insert("iss".to_string(), json!("https://test.issuer.com"));
        claims.insert("sub".to_string(), json!("user123"));
        // Note: no "jti" claim
        let token_claims = TokenClaims::from(claims);
        let token = Token::new("Jans::Access_Token", token_claims, None);

        let built_entities = BuiltEntities::from(&builder.iss_entities);
        let result = builder.build_single_token_entity(&token, &built_entities);

        assert!(
            matches!(
                result.unwrap_err(),
                MultiIssuerEntityError::InvalidEntityUid(_)
            ),
            "Should get InvalidEntityUid error due to missing jti claim"
        );
    }

    #[test]
    fn test_schema_based_processing() {
        // Test schema-based processing with a Token entity schema
        let schema_src = r#"
            namespace Jans {
                entity Token = {
                    sub: String,
                    scope: Set<String>,
                    aud: String,
                    custom_claim: Long
                };
                type Url = {"host": String, "path": String, "protocol": String};
                entity TrustedIssuer = {"issuer_entity_id": Url};
            }
        "#;

        let validator_schema = cedar_policy_core::validator::ValidatorSchema::from_str(schema_src)
            .expect("should parse schema");

        let ti = TrustedIssuer::new(
            "Jans".to_string(),
            String::new(),
            Url::parse("https://test.issuer.com").unwrap(),
            HashMap::default(),
        );
        let trusted_issuers = HashMap::from_iter(vec![("Jans".to_string(), ti)]);
        let builder = EntityBuilder::new(
            TrustedIssuerIndex::new(&trusted_issuers, None),
            Some(&validator_schema),
            DefaultEntities::default(),
        )
        .expect("could not build EntityBuilder");

        let iss = "https://test.issuer.com";
        let mut claims = HashMap::new();
        claims.insert("iss".to_string(), json!(iss));
        claims.insert("jti".to_string(), json!("test-jti-123"));
        claims.insert("sub".to_string(), json!("user123"));
        claims.insert("scope".to_string(), json!(["read:profile", "write:data"]));
        claims.insert("aud".to_string(), json!("my-client"));
        claims.insert("custom_claim".to_string(), json!(42));
        claims.insert(
            "exp".to_string(),
            json!(chrono::Utc::now().timestamp() + 3600),
        );
        let token_claims = TokenClaims::from(claims);
        let token = Token::new(
            "Jans::Access_Token",
            token_claims,
            builder.find_trusted_issuer_by_iss(iss),
        );

        let built_entities = BuiltEntities::from(&builder.iss_entities);
        let entity = builder
            .build_single_token_entity(&token, &built_entities)
            .unwrap();

        // Schema-based processing should preserve types according to schema
        // Check that tags exist for schema-defined claims
        assert!(entity.tag("sub").is_some());
        assert!(entity.tag("scope").is_some());
        assert!(entity.tag("aud").is_some());
        assert!(entity.tag("custom_claim").is_some());

        // Core attributes should still be present with correct values
        assert!(matches!(
            entity.attr("token_type").expect("token_type attribute should exist").expect("should be a valid value"),
            EvalResult::String(ref val) if *val == "Jans::Access_Token"
        ));
        assert!(matches!(
            entity.attr("jti").expect("jti attribute should exist").expect("should be a valid value"),
            EvalResult::String(ref val) if *val == "test-jti-123"
        ));
        // exp attribute should be present and be a future timestamp
        let current_time = chrono::Utc::now().timestamp();
        assert!(matches!(
            entity.attr("exp").expect("exp attribute should exist").expect("should be a valid value"),
            EvalResult::Long(exp) if exp > current_time
        ));
        // iss attribute should be present and be an entity reference
        let iss_value = entity
            .attr("iss")
            .expect("iss attribute should exist")
            .expect("should be a valid value");
        // iss should be an entity reference (EntityUid) when token has trusted issuer
        assert!(
            matches!(iss_value, EvalResult::EntityUid(_)),
            "iss should be an entity reference (EntityUid), got {iss_value:?}"
        );

        // Verify schema-defined claims are present as tags
        // (tags exist but values are stored as RestrictedExpression)

        // Reserved claims (iss, jti, exp) should NOT be in tags
        assert!(entity.tag("iss").is_none());
        assert!(entity.tag("jti").is_none());
        assert!(entity.tag("exp").is_none());
    }

    #[test]
    fn test_invalid_tokens_are_skipped() {
        let builder = create_test_entity_builder();

        let mut tokens = HashMap::new();

        // Valid token
        let mut claims1 = HashMap::new();
        claims1.insert("sub".to_string(), json!("user1"));
        let token_one = create_test_token("https://idp.acme.com/auth", "token1", claims1, &builder);
        tokens.insert("Jans::Access_Token".to_string(), token_one);

        // Invalid token (missing issuer)
        let mut claims2 = HashMap::new();
        claims2.insert("sub".to_string(), json!("user2"));
        // Note: no "iss" claim
        let token_claims2 = TokenClaims::from(claims2);
        let token_two = Token::new("Jans::Id_Token", token_claims2, None);
        tokens.insert("Jans::Id_Token".to_string(), token_two);

        // Another valid token
        let mut claims3 = HashMap::new();
        claims3.insert("sub".to_string(), json!("user3"));
        let token_three =
            create_test_token("https://idp.dolphin.sea/auth", "token3", claims3, &builder);
        tokens.insert("Acme::DolphinToken".to_string(), token_three);

        let tokens: HashMap<String, Arc<Token>> =
            tokens.into_iter().map(|(k, v)| (k, Arc::new(v))).collect();

        let result =
            builder.build_multi_issuer_setup_entities(&tokens, &NopLogger);
        assert!(result.is_ok());

        let entities_data = result.unwrap();
        // Only 2 valid tokens should be processed
        assert_eq!(entities_data.tokens.len(), 2);
        assert!(entities_data.tokens.contains_key("acme_access_token"));
        assert!(entities_data.tokens.contains_key("dolphin_dolphintoken"));
    }

    #[test]
    fn test_all_invalid_tokens_returns_error() {
        let builder = create_test_entity_builder();

        let mut tokens = HashMap::new();

        // Invalid token 1 (missing issuer)
        let mut claims1 = HashMap::new();
        claims1.insert("sub".to_string(), json!("user1"));
        let token_claims1 = TokenClaims::from(claims1);
        let token_one = Token::new("Jans::Access_Token", token_claims1, None);
        tokens.insert("Jans::Access_Token".to_string(), token_one);

        // Invalid token 2 (missing issuer)
        let mut claims2 = HashMap::new();
        claims2.insert("sub".to_string(), json!("user2"));
        let token_claims2 = TokenClaims::from(claims2);
        let token_two = Token::new("Jans::Id_Token", token_claims2, None);
        tokens.insert("Jans::Id_Token".to_string(), token_two);

        let tokens: HashMap<String, Arc<Token>> =
            tokens.into_iter().map(|(k, v)| (k, Arc::new(v))).collect();

        let result =
            builder.build_multi_issuer_setup_entities(&tokens, &NopLogger);

        assert!(
            matches!(result.unwrap_err(), MultiIssuerEntityError::NoValidTokens),
            "Should return NoValidTokens error when all tokens are invalid"
        );
    }

    #[test]
    fn test_schema_fallback_processing() {
        // Test that claims not in schema fall back to string conversion
        let schema_src = r#"
            namespace Jans {
                entity Token = {
                    sub: String,
                    scope: Set<String>
                };
                type Url = {"host": String, "path": String, "protocol": String};
                entity TrustedIssuer = {"issuer_entity_id": Url};
            }
        "#;

        let validator_schema = cedar_policy_core::validator::ValidatorSchema::from_str(schema_src)
            .expect("should parse schema");

        let ti = TrustedIssuer::new(
            "Jans".to_string(),
            String::new(),
            Url::parse("https://test.issuer.com/.well-known/openid-configuration")
                .expect("url should be parsed"),
            HashMap::new(),
        );

        let trusted_issuers = HashMap::from([("Jans".to_string(), ti)]);
        let builder = EntityBuilder::new(
            TrustedIssuerIndex::new(&trusted_issuers, None),
            Some(&validator_schema),
            DefaultEntities::default(),
        )
        .unwrap();

        let mut claims = HashMap::new();
        claims.insert("sub".to_string(), json!("user123"));
        claims.insert("scope".to_string(), json!(["read:profile", "write:data"]));
        claims.insert("unknown_claim".to_string(), json!("some_value")); // Not in schema
        claims.insert("another_unknown".to_string(), json!(123)); // Not in schema

        let token = create_test_token("https://test.issuer.com", "test-jti-123", claims, &builder);

        let built_entities = BuiltEntities::from(&builder.iss_entities);
        let entity = builder
            .build_single_token_entity(&token, &built_entities)
            .unwrap();

        // Schema-defined claims should be processed according to schema
        assert!(entity.tag("sub").is_some());
        assert!(entity.tag("scope").is_some());

        // Unknown claims should fall back to string conversion and become tags
        assert!(entity.tag("unknown_claim").is_some());
        assert!(entity.tag("another_unknown").is_some());

        // Reserved claims (iss, jti, exp) should NOT be in tags
        assert!(entity.tag("iss").is_none());
        assert!(entity.tag("jti").is_none());
        assert!(entity.tag("exp").is_none());

        // Core attributes should still be present with correct values
        assert!(matches!(
            entity.attr("token_type").expect("token_type attribute should exist").expect("should be a valid value"),
            EvalResult::String(ref val) if *val == "Jans::Access_Token"
        ));
        assert!(matches!(
            entity.attr("jti").expect("jti attribute should exist").expect("should be a valid value"),
            EvalResult::String(ref val) if *val == "test-jti-123"
        ));
        // exp attribute should be present and be a future timestamp
        let current_time = chrono::Utc::now().timestamp();
        assert!(matches!(
            entity.attr("exp").expect("exp attribute should exist").expect("should be a valid value"),
            EvalResult::Long(exp) if exp > current_time
        ));
        // iss attribute should be present and be an entity reference
        let iss_value = entity
            .attr("iss")
            .expect("iss attribute should exist")
            .expect("should be a valid value");
        // iss should be an entity reference (EntityUid)
        assert!(
            matches!(iss_value, EvalResult::EntityUid(_)),
            "iss should be an entity reference (EntityUid), got {iss_value:?}"
        );
    }

    #[test]
    fn test_jti_attr_uses_entity_id_not_raw_claim_without_schema() {
        // ── Setup: TrustedIssuer with token_id = "sub" ─────────────────
        // By default token_id = "jti", making entity_id identical to the
        // raw `jti` claim.  Setting token_id = "sub" decouples them so we
        // can prove that the final `jti` attribute comes from
        // `add_reserved_claims` (entity_id), not from the raw claim.
        let tkn_meta = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_Token".into())
            .token_id("sub".into())
            .build();
        let mut token_metadata = HashMap::new();
        token_metadata.insert("Jans::Access_Token".into(), tkn_meta);
        let ti = TrustedIssuer::new(
            "TestIssuer".to_string(),
            String::new(),
            Url::parse("https://test.issuer.com")
                .expect("should parse test issuer URL"),
            token_metadata,
        );
        let trusted_issuers = HashMap::from([("TestIssuer".to_string(), ti)]);
        let builder = EntityBuilder::new(
            TrustedIssuerIndex::new(&trusted_issuers, None),
            None, // no schema — exercises the no-schema path
            DefaultEntities::default(),
        )
        .expect("should create entity builder without schema");

        // ── Token: jti and sub are deliberately different ──────────────
        let iss = "https://test.issuer.com";
        let mut claims = HashMap::new();
        claims.insert("iss".to_string(), json!(iss));
        claims.insert("jti".to_string(), json!("raw-jti-from-jwt"));
        claims.insert("sub".to_string(), json!("entity-sub-uid"));
        claims.insert(
            "exp".to_string(),
            json!(chrono::Utc::now().timestamp() + 3600),
        );
        let token = Token::new(
            "Jans::Access_Token",
            TokenClaims::from(claims),
            builder.find_trusted_issuer_by_iss(iss),
        );
        let built_entities = BuiltEntities::from(&builder.iss_entities);
        let entity = builder
            .build_single_token_entity(&token, &built_entities)
            .expect("should build token entity without schema");

        // ── Verify jti attr = entity_id (NOT the raw jwt jti) ─────────
        // entity_id = "entity-sub-uid" (because token_id = "sub")
        // add_reserved_claims overwrites jti with entity_id.
        let jti_attr = entity
            .attr("jti")
            .expect("jti attribute should exist")
            .expect("jti should be a valid value");
        assert!(
            matches!(jti_attr, EvalResult::String(ref val) if val == "entity-sub-uid"),
            "Expected jti = entity_id ('entity-sub-uid'), proving \
             add_reserved_claims (not the data path) sets jti. \
             Got {jti_attr:?}"
        );

        // Reserved claims must NOT leak into tags
        assert!(
            entity.tag("iss").is_none(),
            "iss should not be a tag (reserved)"
        );
        assert!(
            entity.tag("jti").is_none(),
            "jti should not be a tag (reserved)"
        );
        assert!(
            entity.tag("exp").is_none(),
            "exp should not be a tag (reserved)"
        );

        // Non-reserved claims become tags
        assert!(
            entity.tag("sub").is_some(),
            "sub should be a tag (non-reserved)"
        );
    }

    #[test]
    fn test_reserved_claims_overwrite_schema_path() {
        // ── Schema: entity name MUST match token entity type name ─────
        // `determine_token_entity_type` returns the token's entity type
        // (resolved via trusted-issuer metadata, then fallbacks). For the
        // schema path to activate, that name must exist in the schema's
        // entity map.  We define `entity Access_Token` in `Jans` (full
        // name `Jans::Access_Token`) which matches the token name.
        let schema_src = r#"
            namespace Jans {
                entity Access_Token = {
                    jti: String,
                    sub: String,
                    iss: String,
                    exp: Long
                };
                type Url = {"host": String, "path": String, "protocol": String};
                entity TrustedIssuer = {"issuer_entity_id": Url};
            }
        "#;
        let validator_schema = cedar_policy_core::validator::ValidatorSchema::from_str(schema_src)
            .expect("should parse schema");

        // Same TrustedIssuer with token_id = "sub" as in the no-schema test.
        // The token_metadata entity_type_name must match the schema entity
        // so that `determine_token_entity_type` resolves to something the
        // schema can look up.
        let tkn_meta = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_Token".into())
            .token_id("sub".into())
            .build();
        let mut token_metadata = HashMap::new();
        token_metadata.insert("Jans::Access_Token".into(), tkn_meta);
        let ti = TrustedIssuer::new(
            "Jans".to_string(),
            String::new(),
            Url::parse("https://test.issuer.com")
                .expect("should parse test issuer URL"),
            token_metadata,
        );
        let trusted_issuers = HashMap::from([("Jans".to_string(), ti)]);
        let builder = EntityBuilder::new(
            TrustedIssuerIndex::new(&trusted_issuers, None),
            Some(&validator_schema),
            DefaultEntities::default(),
        )
        .expect("should create entity builder with schema");

        let iss = "https://test.issuer.com";
        let mut claims = HashMap::new();
        claims.insert("iss".to_string(), json!(iss));
        claims.insert("jti".to_string(), json!("raw-jti-from-jwt"));
        claims.insert("sub".to_string(), json!("entity-sub-uid"));
        claims.insert(
            "exp".to_string(),
            json!(chrono::Utc::now().timestamp() + 3600),
        );
        // This claim is deliberately NOT in the schema shape.  In the
        // schema path it stays out of `attrs`; in the no-schema path
        // it would become an attribute.  We assert it is absent below.
        claims.insert("extra_test_claim".to_string(), json!("should-not-be-attr"));
        let token = Token::new(
            "Jans::Access_Token",
            TokenClaims::from(claims),
            builder.find_trusted_issuer_by_iss(iss),
        );
        let built_entities = BuiltEntities::from(&builder.iss_entities);
        let entity = builder
            .build_single_token_entity(&token, &built_entities)
            .expect("should build token entity with schema path");

        // ── Prove the schema path was actually taken ──────────────────
        // `extra_test_claim` is NOT in the schema shape → in the schema
        // path it never enters `attrs`.  The no-schema path would add
        // every claim as an attribute.  If this assertion passes, the
        // schema path (not the fallback) was exercised.
        assert!(
            entity.attr("extra_test_claim").is_none(),
            "extra_test_claim should not be an attribute when the schema \
             path is active (it is not in the schema shape)"
        );

        // ── jti attr = entity_id, not the raw claim ───────────────────
        // The schema-path closure returned raw jti = "raw-jti-from-jwt",
        // then add_reserved_claims overwrote it with entity_id.
        let jti_attr = entity
            .attr("jti")
            .expect("jti attribute should exist")
            .expect("jti should be a valid value");
        assert!(
            matches!(jti_attr, EvalResult::String(ref val) if val == "entity-sub-uid"),
            "Expected jti = entity_id ('entity-sub-uid'), proving \
             add_reserved_claims overwrites the schema-built value. \
             Got {jti_attr:?}"
        );

        // ── iss attr = EntityUid, not the String from schema ──────────
        let iss_attr = entity
            .attr("iss")
            .expect("iss attribute should exist")
            .expect("iss should be a valid value");
        assert!(
            matches!(iss_attr, EvalResult::EntityUid(_)),
            "iss should be an EntityUid (overwritten by add_reserved_claims), \
             not a plain String. Got {iss_attr:?}"
        );

        // ── exp attr = Long, not String ───────────────────────────────
        let current_time = chrono::Utc::now().timestamp();
        let exp_attr = entity
            .attr("exp")
            .expect("exp attribute should exist")
            .expect("exp should be a valid value");
        assert!(
            matches!(exp_attr, EvalResult::Long(exp) if exp > current_time),
            "exp should be a future Long (overwritten by add_reserved_claims). \
             Got {exp_attr:?}"
        );

        // Reserved claims must NOT be in tags
        assert!(entity.tag("iss").is_none(), "iss should not be a tag");
        assert!(entity.tag("jti").is_none(), "jti should not be a tag");
        assert!(entity.tag("exp").is_none(), "exp should not be a tag");

        // Non-reserved claims become tags
        assert!(entity.tag("sub").is_some(), "sub should be a tag");
    }

    #[test]
    fn test_schema_path_fallback_to_entity_id_when_jti_missing() {
        // ── Edge case: schema requires `jti` but JWT has none ─────────
        // Previously the schema-path builder would fail with
        // "required claim missing" because `claims.get("jti")` returned
        // `None`.  The closure now falls back to `entity_id` so the
        // builder passes; `add_reserved_claims` then sets the final value.
        let schema_src = r#"
            namespace Jans {
                entity Access_Token = {
                    jti: String,
                    sub: String,
                    iss: String,
                    exp: Long
                };
                type Url = {"host": String, "path": String, "protocol": String};
                entity TrustedIssuer = {"issuer_entity_id": Url};
            }
        "#;
        let validator_schema = cedar_policy_core::validator::ValidatorSchema::from_str(schema_src)
            .expect("should parse schema");

        let tkn_meta = TokenEntityMetadata::builder()
            .entity_type_name("Jans::Access_Token".into())
            .token_id("sub".into())
            .build();
        let mut token_metadata = HashMap::new();
        token_metadata.insert("Jans::Access_Token".into(), tkn_meta);
        let ti = TrustedIssuer::new(
            "Jans".to_string(),
            String::new(),
            Url::parse("https://test.issuer.com")
                .expect("should parse test issuer URL"),
            token_metadata,
        );
        let trusted_issuers = HashMap::from([("Jans".to_string(), ti)]);
        let builder = EntityBuilder::new(
            TrustedIssuerIndex::new(&trusted_issuers, None),
            Some(&validator_schema),
            DefaultEntities::default(),
        )
        .expect("should create entity builder with schema for missing jti test");

        let iss = "https://test.issuer.com";
        let mut claims = HashMap::new();
        // No "jti" claim — deliberately missing to test the fallback.
        // `entity_id` will come from `sub` (token_id = "sub").
        claims.insert("iss".to_string(), json!(iss));
        claims.insert("sub".to_string(), json!("entity-sub-uid"));
        claims.insert(
            "exp".to_string(),
            json!(chrono::Utc::now().timestamp() + 3600),
        );

        let token = Token::new(
            "Jans::Access_Token",
            TokenClaims::from(claims),
            builder.find_trusted_issuer_by_iss(iss),
        );
        let built_entities = BuiltEntities::from(&builder.iss_entities);

        // This must NOT fail — the closure should fall back to entity_id
        // so the schema path doesn't choke on missing required `jti`.
        let entity = builder
            .build_single_token_entity(&token, &built_entities)
            .expect("should succeed even without jti claim — closure falls back to entity_id");

        // jti attribute should be set to entity_id by add_reserved_claims
        let jti_attr = entity
            .attr("jti")
            .expect("jti attribute should exist")
            .expect("jti should be a valid value");
        assert!(
            matches!(jti_attr, EvalResult::String(ref val) if val == "entity-sub-uid"),
            "Expected jti = entity_id ('entity-sub-uid') when raw jti claim \
             is absent. Got {jti_attr:?}"
        );

        // jti must NOT leak into tags (it is a reserved claim)
        assert!(entity.tag("jti").is_none(), "jti should not be a tag");
    }
}
