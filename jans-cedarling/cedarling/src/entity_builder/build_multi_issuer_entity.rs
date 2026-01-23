// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::entity_id_getters::{EntityIdSrc, get_first_valid_entity_id};
use super::*;
use crate::common::issuer_utils::IssClaim;
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LogEntry, LogLevel};
use cedar_policy::{Entity, EntityUid, RestrictedExpression};
use std::collections::{HashMap, HashSet};
use std::str::FromStr;

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

/// Filter out reserved JWT claims that shouldn't be used as entity tags
/// Reserved claims: iss (issuer), jti (JWT ID), exp (expiration)
fn filter_reserved_claims(claims: &HashMap<String, Value>) -> HashMap<String, Value> {
    claims
        .iter()
        .filter(|(key, _)| key.as_str() != "iss" && key.as_str() != "jti" && key.as_str() != "exp")
        .map(|(k, v)| (k.clone(), v.clone()))
        .collect()
}

/// Add reserved claims to entity attributes based on schema shape
fn add_reserved_claims(
    attrs: &mut HashMap<String, RestrictedExpression>,
    token: &Token,
    entity_id: &str,
    attrs_shape_opt: Option<&HashMap<smol_str::SmolStr, schema::AttrsShape>>,
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
                            .and_then(|v| v.value().as_str().map(|s| s.to_string()))
                            .unwrap_or_else(|| UNDEFINED_ISSUER.to_string()),
                    ),
                );
            }
        }

        // add exp claim
        if let Some(shape) = attrs_shape.get(EXP_CLAIM) {
            if let Some(exp) = token.get_claim_val(EXP_CLAIM).and_then(|v| v.as_i64()) {
                attrs.insert(EXP_CLAIM.to_string(), RestrictedExpression::new_long(exp));
            } else if shape.is_required() {
                // exp is required but missing in token
                return Err(MultiIssuerEntityError::MissingExpClaim);
            }
        }

        // add validated_at claim
        if attrs_shape.contains_key(VALIDATED_AT_CLAIM) {
            let validated_at = chrono::Utc::now().timestamp();
            attrs.insert(
                VALIDATED_AT_CLAIM.to_string(),
                RestrictedExpression::new_long(validated_at),
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

        if let Some(exp) = token.get_claim_val(EXP_CLAIM).and_then(|v| v.as_i64()) {
            attrs.insert(EXP_CLAIM.to_string(), RestrictedExpression::new_long(exp));
        }

        let validated_at = chrono::Utc::now().timestamp();
        attrs.insert(
            VALIDATED_AT_CLAIM.to_string(),
            RestrictedExpression::new_long(validated_at),
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

impl EntityBuilder {
    /// Build all entities for multi-issuer authorization (tokens, principals, resource, roles)
    pub(crate) fn build_multi_issuer_entities(
        &self,
        tokens: &HashMap<String, Arc<Token>>,
        resource: &EntityData,
        log_service: &impl LogWriter,
    ) -> Result<AuthorizeEntitiesData, MultiIssuerEntityError> {
        let mut built_entities = BuiltEntities::from(&self.iss_entities);

        // Build token entities using the existing multi-issuer logic
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
                                "Failed to generate entity key for token '{}'",
                                token_name
                            ))
                            .set_error(e.to_string()),
                        );
                        continue;
                    },
                },
                Err(e) => {
                    log_service.log_any(
                        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                            LogLevel::ERROR,
                            None,
                        ))
                        .set_message(format!(
                            "Failed to build token entity for token '{}'",
                            token_name
                        ))
                        .set_error(e.to_string()),
                    );
                    continue;
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

        // Build resource entity
        let resource = self
            .build_resource_entity(resource)
            .inspect_err(|e| {
                log_service.log_any(
                    LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                        LogLevel::ERROR,
                        None,
                    ))
                    .set_message(
                        "Failed to build resource entity for multi-issuer authorization"
                            .to_string(),
                    )
                    .set_error(e.to_string()),
                );
            })
            .map_err(|e| MultiIssuerEntityError::EntityCreationFailed(e.to_string()))?;

        let issuers = self.iss_entities.values().cloned().collect();

        Ok(AuthorizeEntitiesData {
            issuers,
            tokens: token_entities,
            workload: None,
            user: None,
            roles: Vec::new(),
            resource,
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

        // Generate entity ID using the same logic as the regular entity builder
        // This ensures consistent behavior between regular and multi-issuer entity builders
        let entity_id_srcs = vec![EntityIdSrc::Token {
            token,
            claim: "jti",
        }];
        let entity_id = get_first_valid_entity_id(&entity_id_srcs)
            .map_err(|e| MultiIssuerEntityError::InvalidEntityUid(e.to_string()))?
            .to_string();

        // Get attribute shape from schema if available
        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|schema| schema.get_entity_shape(&entity_type));

        // Filter out reserved claims before building attributes
        // iss, jti, exp are handled separately (iss as entity reference, jti as entity ID, exp as timestamp)
        let filtered_claims = filter_reserved_claims(token.claims_value());

        // Build entity attributes using the same logic as regular entity builder
        // This handles schema-based processing, claim mappings, and entity references
        let mut attrs = super::build_entity_attrs::build_entity_attrs(
            &filtered_claims,
            built_entities,
            attrs_shape,
            token.claim_mappings(),
        )?;

        // Add reserved claims to attributes
        add_reserved_claims(&mut attrs, token, &entity_id, attrs_shape)?;

        // Create entity tags for non-reserved JWT claims
        let mut tags = HashMap::new();
        for (claim_key, claim_value) in filtered_claims {
            let value = convert_claim_to_string_set(&claim_value);
            tags.insert(claim_key, value);
        }

        // Create the Cedar entity using the existing build_cedar_entity function
        // Note: build_cedar_entity doesn't support tags, so we need to use Entity::new_with_tags directly
        // but we can still reuse the UID creation logic
        let uid = EntityUid::from_str(&format!("{}::\"{}\"", entity_type, entity_id))
            .map_err(|e| MultiIssuerEntityError::InvalidEntityUid(e.to_string()))?;

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
            .get_claim_val("iss")
            .and_then(|iss| iss.as_str())
            .ok_or(MultiIssuerEntityError::MissingIssuer)?;

        let issuer_simplified = self.resolve_issuer_name(issuer)?;
        let token_type_simplified = simplify_token_type(token_name);

        Ok(format!("{}_{}", issuer_simplified, token_type_simplified))
    }

    /// Resolve issuer name using trusted issuer metadata or fallback to hostname
    fn resolve_issuer_name(&self, issuer: &str) -> Result<String, MultiIssuerEntityError> {
        // First, try to find the issuer in trusted issuer metadata
        if let Some(trusted_issuer) = self.issuers_index.find(issuer) {
            return Ok(sanitize_issuer_name(&trusted_issuer.name));
        }

        // Fallback to hostname from JWT iss claim
        let hostname = issuer
            .replace("https://", "")
            .replace("http://", "")
            .split('/')
            .next()
            .unwrap_or(issuer)
            .split(':')
            .next()
            .unwrap_or(issuer)
            .to_string();

        Ok(sanitize_issuer_name(&hostname))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::authz::request::CedarEntityMapping;
    use crate::common::policy_store::TrustedIssuer;
    use crate::entity_builder_config::{EntityBuilderConfig, EntityNames, UnsignedRoleIdSrc};
    use crate::jwt::{Token, TokenClaims};
    use crate::log::NopLogger;
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;
    use url::Url;

    fn create_test_entity_builder() -> EntityBuilder {
        let config = EntityBuilderConfig {
            entity_names: EntityNames {
                user: "User".to_string(),
                role: "Role".to_string(),
                workload: "Workload".to_string(),
            },
            build_user: false,
            build_workload: false,
            unsigned_role_id_src: UnsignedRoleIdSrc::default(),
        };

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
            config,
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

    fn create_test_resource() -> EntityData {
        EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::Resource".to_string(),
                id: "test_resource".to_string(),
            },
            attributes: HashMap::new(),
        }
    }

    #[test]
    fn test_issuer_name_resolution_with_trusted_metadata() {
        let builder = create_test_entity_builder();

        // Test with trusted issuer metadata
        let result = builder
            .resolve_issuer_name("https://idp.acme.com/auth")
            .unwrap();
        assert_eq!(result, "acme");

        let result = builder
            .resolve_issuer_name("https://idp.dolphin.sea/auth")
            .unwrap();
        assert_eq!(result, "dolphin");

        let result = builder
            .resolve_issuer_name("https://login.microsoftonline.com/tenant")
            .unwrap();
        assert_eq!(result, "microsoft");
    }

    #[test]
    fn test_issuer_name_resolution_fallback_to_hostname() {
        let builder = create_test_entity_builder();

        // Test fallback to hostname for unknown issuer
        let result = builder
            .resolve_issuer_name("https://unknown.issuer.com/auth")
            .unwrap();
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
        let token1 = create_test_token("https://idp.acme.com/auth", "token1", claims1, &builder);
        tokens.insert("Jans::Access_Token".to_string(), token1);

        let mut claims2 = HashMap::new();
        claims2.insert("sub".to_string(), json!("user2"));
        let token2 = create_test_token("https://idp.acme.com/auth", "token2", claims2, &builder);
        tokens.insert("Jans::Access_Token2".to_string(), token2);

        let tokens: HashMap<String, Arc<Token>> =
            tokens.into_iter().map(|(k, v)| (k, Arc::new(v))).collect();

        let result =
            builder.build_multi_issuer_entities(&tokens, &create_test_resource(), &NopLogger);
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
        let token1 = create_test_token("https://idp.acme.com/auth", "token1", claims1, &builder);
        tokens.insert("Jans::Access_Token".to_string(), token1);

        // Token from Dolphin issuer
        let mut claims2 = HashMap::new();
        claims2.insert("sub".to_string(), json!("user2"));
        let token2 = create_test_token("https://idp.dolphin.sea/auth", "token2", claims2, &builder);
        tokens.insert("Acme::DolphinToken".to_string(), token2);

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
        let token3 = Token::new("Jans::Id_Token", token_claims3, None);
        tokens.insert("Jans::Id_Token".to_string(), token3);

        let tokens: HashMap<String, Arc<Token>> =
            tokens.into_iter().map(|(k, v)| (k, Arc::new(v))).collect();

        let result =
            builder.build_multi_issuer_entities(&tokens, &create_test_resource(), &NopLogger);
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
                entity TrustedIssuer = {"issuer_entity_id": String};
            }
        "#;

        let validator_schema = cedar_policy_core::validator::ValidatorSchema::from_str(schema_src)
            .expect("should parse schema");

        let config = EntityBuilderConfig {
            entity_names: EntityNames {
                // user, role and workload is unused
                user: "User".to_string(),
                role: "Role".to_string(),
                workload: "Workload".to_string(),
            },
            build_user: false,
            build_workload: false,
            unsigned_role_id_src: UnsignedRoleIdSrc::default(),
        };

        let ti = TrustedIssuer::new(
            "Jans".to_string(),
            String::new(),
            Url::parse("https://test.issuer.com").unwrap(),
            HashMap::default(),
        );
        let trusted_issuers = HashMap::from_iter(vec![("Jans".to_string(), ti)]);
        let builder = EntityBuilder::new(
            config,
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
            "iss should be an entity reference (EntityUid), got {:?}",
            iss_value
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
        let token1 = create_test_token("https://idp.acme.com/auth", "token1", claims1, &builder);
        tokens.insert("Jans::Access_Token".to_string(), token1);

        // Invalid token (missing issuer)
        let mut claims2 = HashMap::new();
        claims2.insert("sub".to_string(), json!("user2"));
        // Note: no "iss" claim
        let token_claims2 = TokenClaims::from(claims2);
        let token2 = Token::new("Jans::Id_Token", token_claims2, None);
        tokens.insert("Jans::Id_Token".to_string(), token2);

        // Another valid token
        let mut claims3 = HashMap::new();
        claims3.insert("sub".to_string(), json!("user3"));
        let token3 = create_test_token("https://idp.dolphin.sea/auth", "token3", claims3, &builder);
        tokens.insert("Acme::DolphinToken".to_string(), token3);

        let tokens: HashMap<String, Arc<Token>> =
            tokens.into_iter().map(|(k, v)| (k, Arc::new(v))).collect();

        let result =
            builder.build_multi_issuer_entities(&tokens, &create_test_resource(), &NopLogger);
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
        let token1 = Token::new("Jans::Access_Token", token_claims1, None);
        tokens.insert("Jans::Access_Token".to_string(), token1);

        // Invalid token 2 (missing issuer)
        let mut claims2 = HashMap::new();
        claims2.insert("sub".to_string(), json!("user2"));
        let token_claims2 = TokenClaims::from(claims2);
        let token2 = Token::new("Jans::Id_Token", token_claims2, None);
        tokens.insert("Jans::Id_Token".to_string(), token2);

        let tokens: HashMap<String, Arc<Token>> =
            tokens.into_iter().map(|(k, v)| (k, Arc::new(v))).collect();

        let result =
            builder.build_multi_issuer_entities(&tokens, &create_test_resource(), &NopLogger);

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
                entity TrustedIssuer = {"issuer_entity_id": String};
            }
        "#;

        let validator_schema = cedar_policy_core::validator::ValidatorSchema::from_str(schema_src)
            .expect("should parse schema");

        let config = EntityBuilderConfig {
            entity_names: EntityNames {
                user: "User".to_string(),
                role: "Role".to_string(),
                workload: "Workload".to_string(),
            },
            build_user: false,
            build_workload: false,
            unsigned_role_id_src: UnsignedRoleIdSrc::default(),
        };

        let ti = TrustedIssuer::new(
            "Jans".to_string(),
            String::new(),
            Url::parse("https://test.issuer.com/.well-known/openid-configuration")
                .expect("url should be parsed"),
            HashMap::new(),
        );

        let trusted_issuers = HashMap::from([("Jans".to_string(), ti)]);
        let builder = EntityBuilder::new(
            config,
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
            "iss should be an entity reference (EntityUid), got {:?}",
            iss_value
        );
    }
}
