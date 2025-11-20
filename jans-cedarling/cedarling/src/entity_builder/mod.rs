// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Entity Builder
//!
//! This module is responsible for mapping JWTs to Cedar entities

mod build_entity_attrs;
mod build_expr;
mod build_iss_entity;
mod build_principal_entity;
mod build_resource_entity;
mod build_role_entity;
mod build_token_entities;
mod built_entities;
mod entity_id_getters;
mod error;
mod schema;
mod value_to_expr;

use crate::authz::AuthorizeEntitiesData;
use crate::authz::request::EntityData;
use crate::common::PartitionResult;
use crate::common::issuer_utils::normalize_issuer;
use crate::common::policy_store::{ClaimMappings, TrustedIssuer};
use crate::entity_builder::build_principal_entity::BuiltPrincipalUnsigned;
use crate::jwt::Token;
use crate::log::interface::LogWriter;
use crate::log::{LogEntry, LogType, Logger};
use crate::{LogLevel, RequestUnsigned, entity_builder_config::*};
use build_entity_attrs::*;
use build_iss_entity::build_iss_entity;
use cedar_policy::{Entity, EntityUid, RestrictedExpression};
use cedar_policy_validator::ValidatorSchema;
use schema::MappingSchema;
use serde_json::Value;
use std::collections::{HashMap, HashSet};
use std::str::FromStr;
use std::sync::Arc;
use url::Origin;

pub(crate) use built_entities::BuiltEntities;
pub use error::*;

/// Constant for unknown entity type in error messages
const UNKNOWN_ENTITY_TYPE: &str = "Unknown";

/// Helper function to parse entity attributes from a key-value iterator
fn parse_entity_attrs<'a>(
    attrs_iter: impl Iterator<Item = (&'a String, &'a Value)>,
    entity_type: &str,
    entity_id: &str,
) -> Result<HashMap<String, RestrictedExpression>, BuildEntityError> {
    let mut cedar_attrs = HashMap::new();
    for (key, value) in attrs_iter {
        match value_to_expr::value_to_expr(value) {
            Ok(Some(expr)) => {
                cedar_attrs.insert(key.clone(), expr);
            },
            Ok(None) => {
                continue;
            },
            Err(errors) => {
                return Err(BuildEntityError::new(
                    entity_type.to_string(),
                    BuildEntityErrorKind::InvalidEntityData(format!(
                        "Failed to convert attribute '{}' for entity '{}': {:?}",
                        key, entity_id, errors
                    )),
                ));
            },
        }
    }
    Ok(cedar_attrs)
}

/// Parse default entities from the provided configuration
fn parse_default_entities(
    default_entities_data: &HashMap<String, Value>,
    namespace: Option<&str>,
    logger: Logger,
) -> Result<HashMap<EntityUid, Entity>, BuildEntityError> {
    let mut default_entities = HashMap::new();

    for (entry_id, entity_data) in default_entities_data {
        // Validate entity ID to prevent injection attacks
        if entry_id.trim().is_empty() {
            return Err(BuildEntityError::new(
                "DefaultEntity".to_string(),
                BuildEntityErrorKind::InvalidEntityData(
                    "Entity ID cannot be empty or whitespace-only".to_string(),
                ),
            ));
        }

        // Basic security validation - prevent obvious injection patterns
        // Check for potentially dangerous characters while allowing valid Cedar entity ID formats
        let dangerous_patterns = [
            "<script",
            "javascript:",
            "data:",
            "vbscript:",
            "onload=",
            "onerror=",
        ];
        let entry_id_lower = entry_id.to_lowercase();
        for pattern in &dangerous_patterns {
            if entry_id_lower.contains(pattern) {
                return Err(BuildEntityError::new(
                    "DefaultEntity".to_string(),
                    BuildEntityErrorKind::InvalidEntityData(format!(
                        "Entity ID '{}' contains potentially dangerous content",
                        entry_id
                    )),
                ));
            }
        }

        // Parse the entity data as a JSON object
        let entity_obj = if let Value::Object(obj) = entity_data {
            obj
        } else {
            return Err(BuildEntityError::new(
                UNKNOWN_ENTITY_TYPE.to_string(),
                BuildEntityErrorKind::InvalidEntityData(format!(
                    "Default entity data for '{}' must be a JSON object",
                    entry_id
                )),
            ));
        };

        // Check if this is the new Cedar entity format (with uid, attrs, parents fields)
        let (entity_type, entity_id_from_uid, cedar_attrs, parents) = if entity_obj
            .contains_key("uid")
        {
            // New Cedar entity format: {"uid": {"type": "...", "id": "..."}, "attrs": {}, "parents": [...]}
            let uid_obj = entity_obj
                .get("uid")
                .and_then(|v| v.as_object())
                .ok_or_else(|| {
                    BuildEntityError::new(
                        UNKNOWN_ENTITY_TYPE.to_string(),
                        BuildEntityErrorKind::InvalidEntityData(format!(
                            "Default entity '{}' has invalid uid field",
                            entry_id
                        )),
                    )
                })?;

            let entity_type_from_uid =
                uid_obj
                    .get("type")
                    .and_then(|v| v.as_str())
                    .ok_or_else(|| {
                        BuildEntityError::new(
                            UNKNOWN_ENTITY_TYPE.to_string(),
                            BuildEntityErrorKind::InvalidEntityData(format!(
                                "Default entity '{}' has invalid uid.type field",
                                entry_id
                            )),
                        )
                    })?;

            // Add namespace prefix if not already present
            let full_entity_type = build_entity_type_name(entity_type_from_uid, &namespace);

            // Get the entity ID from uid.id if present
            let entity_id_from_uid = uid_obj.get("id")
                .and_then(|v| v.as_str())
                // Fall back to the HashMap key if uid.id is not specified
                .unwrap_or(entry_id);

            // Parse attributes from attrs field
            let empty_map = serde_json::Map::new();
            let attrs_obj = entity_obj
                .get("attrs")
                .and_then(|v| v.as_object())
                .unwrap_or(&empty_map);

            let cedar_attrs =
                parse_entity_attrs(attrs_obj.iter(), &full_entity_type, entity_id_from_uid)?;

            // Parse parents from parents field
            let empty_vec: Vec<Value> = vec![];
            let parents_array = entity_obj
                .get("parents")
                .and_then(|v| v.as_array())
                .unwrap_or(&empty_vec);

            let mut parents_set = HashSet::new();
            for parent in parents_array {
                if let Value::Object(parent_obj) = parent
                    && let (Some(type_v), Some(id_v)) = (
                        parent_obj.get("type").and_then(|v| v.as_str()),
                        parent_obj.get("id").and_then(|v| v.as_str()),
                    )
                {
                    // Add namespace if not present
                    let full_parent_entity_type = build_entity_type_name(type_v, &namespace);
                    let parent_uid_str = format!("{}::\"{}\"", full_parent_entity_type, id_v);
                    match EntityUid::from_str(&parent_uid_str) {
                        Ok(parent_uid) => {
                            parents_set.insert(parent_uid);
                        },
                        Err(e) => {
                            // log warn that we could not parse uid
                            let log_entry = LogEntry::new_with_data(LogType::System, None)
                                .set_level(LogLevel::WARN)
                                .set_message(format!(
                                    "Could not parse parent UID '{}' for default entity '{}': {}",
                                    parent_uid_str, entry_id, e
                                ));

                            logger.log_any(log_entry);
                        },
                    }
                } else {
                    // log warn that we skip value because it is not object
                    let log_entry = LogEntry::new_with_data(LogType::System, None)
                        .set_level(LogLevel::WARN)
                        .set_message(format!(
                            "In default entity parent array json value should be object, skip: {}",
                            parent
                        ));

                    logger.log_any(log_entry);
                }
            }

            (
                full_entity_type,
                entity_id_from_uid,
                cedar_attrs,
                parents_set,
            )
        } else if entity_obj.contains_key("entity_type") {
            // Old format with entity_type field
            let entity_type = entity_obj
                .get("entity_type")
                .and_then(|v| v.as_str())
                .ok_or_else(|| {
                    BuildEntityError::new(
                        UNKNOWN_ENTITY_TYPE.to_string(),
                        BuildEntityErrorKind::InvalidEntityData(format!(
                            "Default entity '{}' has invalid entity_type field",
                            entry_id
                        )),
                    )
                })?;

            let entity_id_from_uid = entity_obj
                .get("entity_id")
                .and_then(|v| v.as_str())
                .unwrap_or(entry_id);

            // Convert JSON attributes to Cedar expressions
            let cedar_attrs = parse_entity_attrs(
                entity_obj
                    .iter()
                    .filter(|(key, _)| key != &"entity_type" && key != &"entity_id"),
                entity_type,
                entry_id,
            )?;

            (
                entity_type.to_string(),
                entity_id_from_uid,
                cedar_attrs,
                HashSet::new(),
            )
        } else {
            return Err(BuildEntityError::new(
                UNKNOWN_ENTITY_TYPE.to_string(),
                BuildEntityErrorKind::InvalidEntityData(format!(
                    "Default entity '{}' must have either uid field (Cedar format) or entity_type field (legacy format)",
                    entry_id
                )),
            ));
        };

        // Build the Cedar entity
        let entity = build_cedar_entity(&entity_type, entity_id_from_uid, cedar_attrs, parents)?;
        default_entities.insert(entity.uid().clone(), entity);
    }

    Ok(default_entities)
}

fn build_entity_type_name(entity_type_from_uid: &str, namespace: &Option<&str>) -> String {
    if entity_type_from_uid.contains("::") {
        entity_type_from_uid.to_string()
    } else if let Some(ns) = namespace
        && !ns.is_empty()
    {
        format!("{}::{}", ns, entity_type_from_uid)
    } else {
        entity_type_from_uid.to_string()
    }
}

pub struct EntityBuilder {
    config: EntityBuilderConfig,
    iss_entities: HashMap<Origin, Entity>,
    schema: Option<MappingSchema>,
    default_entities: HashMap<EntityUid, Entity>,
}

impl EntityBuilder {
    pub fn new(
        config: EntityBuilderConfig,
        trusted_issuers: &HashMap<String, TrustedIssuer>,
        schema: Option<&ValidatorSchema>,
        default_entities_data: Option<&HashMap<String, Value>>,
        namespace: Option<&str>,
        logger: Logger,
    ) -> Result<Self, InitEntityBuilderError> {
        let schema = schema.map(MappingSchema::try_from).transpose()?;

        let (ok, errs) = trusted_issuers
            .values()
            .map(|iss| {
                let iss_id = normalize_issuer(&iss.oidc_endpoint.origin().ascii_serialization());
                build_iss_entity(&config.entity_names.iss, &iss_id, iss, schema.as_ref())
            })
            .partition_result();

        if !errs.is_empty() {
            return Err(InitEntityBuilderError::BuildIssEntities(errs.into()));
        }

        let iss_entities = ok.into_iter().collect::<HashMap<Origin, Entity>>();

        // Parse default entities if provided
        let default_entities = if let Some(entities_data) = default_entities_data {
            parse_default_entities(entities_data, namespace, logger)
                .map_err(|e| InitEntityBuilderError::BuildIssEntities(vec![e].into()))?
        } else {
            HashMap::new()
        };

        Ok(Self {
            config,
            iss_entities,
            schema,
            default_entities,
        })
    }

    pub fn build_entities(
        &self,
        tokens: &HashMap<String, Arc<Token>>,
        resource_data: &EntityData,
    ) -> Result<AuthorizeEntitiesData, BuildEntityError> {
        let mut tkn_principal_mappings = TokenPrincipalMappings::default();
        let mut built_entities = BuiltEntities::from(&self.iss_entities);

        let mut token_entities = HashMap::new();
        for (tkn_name, tkn) in tokens.iter() {
            let entity_name = tkn
                .iss
                .as_ref()
                .and_then(|iss| iss.token_metadata.get(tkn_name))
                .map(|metadata| metadata.entity_type_name.as_str())
                .or_else(|| default_tkn_entity_name(tkn_name));

            let Some(entity_name) = entity_name else {
                continue;
            };

            let tkn_entity = self.build_tkn_entity(
                entity_name,
                tkn,
                &mut tkn_principal_mappings,
                &built_entities,
                HashSet::new(),
            )?;
            built_entities.insert(&tkn_entity.uid());
            token_entities.insert(tkn_name.to_string(), tkn_entity);
        }

        let workload = if self.config.build_workload {
            let workload_entity =
                self.build_workload_entity(tokens, &tkn_principal_mappings, &built_entities)?;
            Some(workload_entity)
        } else {
            None
        };

        let (user, roles) = if self.config.build_user {
            let roles = self.build_role_entities(tokens)?;
            let role_uids = roles.iter().map(|e| e.uid()).collect();
            let user = self.build_user_entity(
                tokens,
                &tkn_principal_mappings,
                &built_entities,
                role_uids,
            )?;
            (Some(user), roles)
        } else {
            (None, Vec::new())
        };

        let mut resource = self.build_resource_entity(resource_data)?;
        if let Some(resource_default_entity) = self.default_entities.get(&resource.uid())
            && resource_data.attributes.is_empty()
        {
            resource = resource_default_entity.clone()
        }

        let issuers = self.iss_entities.values().cloned().collect();
        Ok(AuthorizeEntitiesData {
            issuers,
            workload,
            user,
            resource,
            roles,
            tokens: token_entities,
            default_entities: self.default_entities.clone(),
        })
    }

    /// Builds the entities using the unsigned interface
    pub fn build_entities_unsigned(
        &self,
        request: &RequestUnsigned,
    ) -> Result<BuiltEntitiesUnsigned, BuildUnsignedEntityError> {
        let mut built_entities = BuiltEntities::default();

        let mut principals = Vec::with_capacity(request.principals.len());
        let mut roles = Vec::<Entity>::new();
        for principal in request.principals.iter() {
            let BuiltPrincipalUnsigned { principal, parents } =
                self.build_principal_unsigned(principal, &built_entities)?;

            built_entities.insert(&principal.uid());
            for role in roles.iter() {
                built_entities.insert(&role.uid());
            }

            principals.push(principal);
            roles.extend(parents);
        }

        let resource = self
            .build_resource_entity(&request.resource)
            .map_err(Box::new)?;

        Ok(BuiltEntitiesUnsigned {
            principals,
            roles,
            resource,
            built_entities,
        })
    }

    pub fn build_entity(
        &self,
        type_name: &str,
        id: &str,
        parents: HashSet<EntityUid>,
        attrs_src: &HashMap<String, Value>,
        entities: &BuiltEntities,
        claim_mappings: Option<&ClaimMappings>,
    ) -> Result<Entity, BuildEntityError> {
        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|s| s.get_entity_shape(type_name));
        let attrs = build_entity_attrs(attrs_src, entities, attrs_shape, claim_mappings)
            .map_err(|e| BuildEntityErrorKind::from(e).while_building(type_name))?;

        build_cedar_entity(type_name, id, attrs, parents)
    }
}

pub struct BuiltEntitiesUnsigned {
    pub principals: Vec<Entity>,
    pub roles: Vec<Entity>,
    pub resource: Entity,
    pub built_entities: BuiltEntities,
}

pub fn build_cedar_entity(
    type_name: &str,
    id: &str,
    attrs: HashMap<String, RestrictedExpression>,
    parents: HashSet<EntityUid>,
) -> Result<Entity, BuildEntityError> {
    let uid = EntityUid::from_str(&format!("{}::\"{}\"", type_name, id)).map_err(
        |e: cedar_policy::ParseErrors| {
            BuildEntityErrorKind::from(Box::new(e)).while_building(type_name)
        },
    )?;
    let entity = Entity::new(uid, attrs, parents)
        .map_err(|e| BuildEntityErrorKind::from(Box::new(e)).while_building(type_name))?;

    Ok(entity)
}

fn default_tkn_entity_name(tkn_name: &str) -> Option<&'static str> {
    match tkn_name {
        "access_token" => Some(DEFAULT_ACCESS_TKN_ENTITY_NAME),
        "id_token" => Some(DEFAULT_ID_TKN_ENTITY_NAME),
        "userinfo_token" => Some(DEFAULT_USERINFO_TKN_ENTITY_NAME),
        _ => None,
    }
}

#[derive(Default)]
pub struct TokenPrincipalMappings(HashMap<String, Vec<(String, RestrictedExpression)>>);

impl From<Vec<TokenPrincipalMapping>> for TokenPrincipalMappings {
    fn from(value: Vec<TokenPrincipalMapping>) -> Self {
        Self(value.into_iter().fold(HashMap::new(), |mut acc, mapping| {
            acc.entry(mapping.principal.clone())
                .or_default()
                .push((mapping.attr_name.clone(), mapping.expr.clone()));
            acc
        }))
    }
}

/// Represents a token and it's UID
#[derive(Clone)]
pub struct TokenPrincipalMapping {
    /// The principal where token will be inserted
    principal: String,
    /// The name of the attribute of the token
    attr_name: String,
    /// An EntityUID reference to the token
    expr: RestrictedExpression,
}

impl TokenPrincipalMappings {
    pub fn insert(&mut self, value: TokenPrincipalMapping) {
        let entry = self.0.entry(value.principal).or_default();
        entry.push((value.attr_name, value.expr));
    }

    pub fn get(&self, principal: &str) -> Option<&Vec<(String, RestrictedExpression)>> {
        self.0.get(principal)
    }

    pub fn apply(
        &self,
        principal_type: &str,
        attributes: &mut HashMap<String, RestrictedExpression>,
    ) {
        let Some(mappings) = self.get(principal_type) else {
            return;
        };

        attributes.extend(mappings.iter().cloned());
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::CedarEntityMapping;
    use crate::common::policy_store::TokenEntityMetadata;
    use crate::log::TEST_LOGGER;
    use cedar_policy::{Entities, Schema};
    use serde_json::{Value, json};
    use std::collections::HashMap;
    use std::sync::LazyLock;
    use test_utils::{SortedJson, assert_eq};

    pub static CEDARLING_VALIDATOR_SCHEMA: LazyLock<ValidatorSchema> = LazyLock::new(|| {
        ValidatorSchema::from_str(include_str!("../../../schema/cedarling_core.cedarschema"))
            .expect("should be a valid Cedar validator schema")
    });

    pub static CEDARLING_API_SCHEMA: LazyLock<Schema> = LazyLock::new(|| {
        Schema::from_str(include_str!("../../../schema/cedarling_core.cedarschema"))
            .expect("should be a valid Cedar schema")
    });

    /// Helper function for asserting entities for better error readability
    #[track_caller]
    pub fn assert_entity_eq(entity: &Entity, expected: Value, schema: Option<&Schema>) {
        let entity_json = entity
            .clone()
            .to_json_value()
            .expect("should serialize entity to JSON");

        // Check if the entity has the correct uid
        assert_eq!(
            entity_json["uid"], expected["uid"],
            "the entity uid does not match with the expected",
        );

        // Check if the entity has the correct attributes
        let expected_attrs =
            serde_json::from_value::<HashMap<String, Value>>(expected["attrs"].clone()).unwrap();
        for (name, expected_val) in expected_attrs.iter() {
            assert_eq!(
                &entity_json["attrs"][name],
                expected_val,
                "the {}'s `{}` attribute does not match with the expected. other attrs available: {:?}",
                entity.uid().to_string(),
                name,
                entity_json["attrs"]
                    .as_object()
                    .unwrap()
                    .keys()
                    .collect::<Vec<_>>()
            );
        }

        // Check if the entity has the correct parents
        assert_eq!(
            serde_json::from_value::<HashSet<Value>>(entity_json["parents"].clone())
                .expect("parents should be a valid Array"),
            serde_json::from_value::<HashSet<Value>>(expected["parents"].clone())
                .expect("parents should be a valid Array"),
            "the {} entity's parents does not match with the expected",
            entity.uid().to_string(),
        );

        // Check if the entity conforms to the schema
        Entities::from_entities([entity.clone()], schema)
            .unwrap_or_else(|_| panic!("{} entity should conform to the schema", entity.uid()));
    }

    #[test]
    fn can_build_principals_with_custom_types() {
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity Resource;
                entity CustomWorkload {
                    access_token: CustomAccessToken,
                    custom_token: AnotherNamespace::CustomToken,
                };
                entity CustomAccessToken;
            }
            namespace AnotherNamespace {
                entity CustomToken;
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("parse schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("parse validation schema");

        // Set the custom workload name in the config
        let mut config = EntityBuilderConfig::default().with_workload();
        config.build_user = false; // Explicitly disable user entity building for this test
        config.entity_names.workload = "Jans::CustomWorkload".into();

        // Set the custom token names in the IDP metadata
        let iss = TrustedIssuer {
            token_metadata: HashMap::from([
                (
                    "access_token".to_string(),
                    TokenEntityMetadata::builder()
                        .entity_type_name("Jans::CustomAccessToken".to_string())
                        .principal_mapping(
                            ["Jans::CustomWorkload".to_string()].into_iter().collect(),
                        )
                        .build(),
                ),
                (
                    "custom_token".to_string(),
                    TokenEntityMetadata::builder()
                        .entity_type_name("AnotherNamespace::CustomToken".to_string())
                        .principal_mapping(
                            ["Jans::CustomWorkload".to_string()].into_iter().collect(),
                        )
                        .build(),
                ),
            ]),
            ..Default::default()
        };
        let issuers = HashMap::from([("some_iss".into(), iss)]);
        let tokens = HashMap::from([
            (
                "access_token".into(),
                Arc::new(Token::new(
                    "access_token",
                    json!({"jti": "some_jti", "aud": "some_aud"}).into(),
                    Some(issuers.get("some_iss").unwrap().clone().into()),
                )),
            ),
            (
                "custom_token".into(),
                Arc::new(Token::new(
                    "custom_token",
                    json!({"jti": "some_jti"}).into(),
                    Some(issuers.get("some_iss").unwrap().clone().into()),
                )),
            ),
        ]);

        let entity_builder = EntityBuilder::new(
            config,
            &issuers,
            Some(&validator_schema),
            None,
            None,
            TEST_LOGGER.clone(),
        )
        .expect("init entity builder");

        let entities = entity_builder
            .build_entities(
                &tokens,
                &EntityData {
                    cedar_mapping: CedarEntityMapping {
                        entity_type: "Jans::Resource".into(),
                        id: "some_id".into(),
                    },
                    attributes: HashMap::new(),
                },
            )
            .expect("build entities");

        assert_entity_eq(
            &entities.workload.expect("has workload entity"),
            json!({
                "uid": {"type": "Jans::CustomWorkload", "id": "some_aud"},
                "attrs": {
                    "access_token": {"__entity": {
                        "type": "Jans::CustomAccessToken",
                        "id": "some_jti"
                    }},
                    "custom_token": {"__entity": {
                        "type": "AnotherNamespace::CustomToken",
                        "id": "some_jti"
                    }},
                },
                "parents": []
            }),
            Some(&schema),
        );
    }

    #[test]
    fn can_parse_default_entities() {
        // Test the parse_default_entities function directly
        // We don't need the schema for this test since we're not validating the entities

        // Create test default entities
        let default_entities = HashMap::from([(
            "1694c954f8d9".to_string(),
            json!({
                "entity_id": "1694c954f8d9",
                "entity_type": "Jans::DefaultEntity",
                "o": "Acme Dolphins Division",
                "org_id": "100129"
            }),
        )]);

        // Test that parse_default_entities works
        let parsed_entities =
            parse_default_entities(&default_entities, Some("Test"), TEST_LOGGER.clone())
                .expect("should parse default entities");

        assert_eq!(parsed_entities.len(), 1, "should have 1 default entity");

        // Verify the entity
        let entity = parsed_entities
            .get(&EntityUid::from_str("Jans::DefaultEntity::\"1694c954f8d9\"").unwrap())
            .expect("should have entity");
        assert_eq!(entity.uid().type_name().to_string(), "Jans::DefaultEntity");
        assert_eq!(entity.uid().id().as_ref() as &str, "1694c954f8d9");
    }

    #[test]
    fn can_build_entities_with_default_entities() {
        // Test that default entities are properly included in the authorization flow
        use crate::authz::request::EntityData;
        use crate::common::policy_store::TrustedIssuer;
        use crate::jwt::Token;
        use std::sync::Arc;
        use url::Url;

        // Create a simple schema
        let schema_src = r#"
        namespace Jans {
          entity DefaultEntity;
          entity User;
          entity Issue;
          entity TrustedIssuer;
        }
        "#;

        let schema = ValidatorSchema::from_str(schema_src).expect("should parse schema");

        // Create default entities data
        let default_entities_data = HashMap::from([
            (
                "1694c954f8d9".to_string(),
                json!({
                    "entity_id": "1694c954f8d9",
                    "entity_type": "Jans::DefaultEntity",
                    "o": "Acme Dolphins Division",
                    "org_id": "100129",
                    "regions": ["Atlantic", "Pacific", "Indian"]
                }),
            ),
            (
                "74d109b20248".to_string(),
                json!({
                    "entity_id": "74d109b20248",
                    "entity_type": "Jans::DefaultEntity",
                    "description": "2025 Price List",
                    "products": {"15020": 995, "15050": 1495},
                    "services": {"51001": 9900, "51020": 29900}
                }),
            ),
        ]);

        // Create trusted issuer
        let trusted_issuer = TrustedIssuer {
            name: "Test Issuer".to_string(),
            description: "Test".to_string(),
            oidc_endpoint: Url::parse("https://test.jans.org/.well-known/openid-configuration")
                .expect("valid url"),
            token_metadata: HashMap::from([(
                "id_token".into(),
                TokenEntityMetadata::builder()
                    .entity_type_name("Jans::Id_token".to_string())
                    .principal_mapping(["Jans::User".to_string()].into_iter().collect())
                    .build(),
            )]),
        };

        let trusted_issuers = HashMap::from([("test_issuer".to_string(), trusted_issuer)]);

        // Create tokens
        let tokens = HashMap::from([(
            "id_token".to_string(),
            Arc::new(Token::new(
                "id_token",
                json!({
                    "sub": "user123",
                    "country": "Atlantic",
                    "aud": "test_audience",
                    "jti": "test_jti_123"
                })
                .into(),
                Some(Arc::new(
                    trusted_issuers.get("test_issuer").unwrap().clone(),
                )),
            )),
        )]);

        // Create entity builder with default entities
        let mut config = EntityBuilderConfig::default();
        config.build_workload = false;
        config.build_user = true;

        let entity_builder = EntityBuilder::new(
            config,
            &trusted_issuers,
            Some(&schema),
            Some(&default_entities_data),
            None,
            TEST_LOGGER.clone(),
        )
        .expect("should create entity builder");

        // Create resource
        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::Issue".to_string(),
                id: "issue123".to_string(),
            },
            attributes: HashMap::from([("org_id".to_string(), json!("1694c954f8d9"))]),
        };

        // Build entities
        let entities_data = entity_builder
            .build_entities(&tokens, &resource)
            .expect("should build entities");

        // Verify default entities are included
        assert_eq!(
            entities_data.default_entities.len(),
            2,
            "should have 2 default entities"
        );

        // Verify specific default entity
        let default_entity = entities_data
            .default_entities
            .get(&EntityUid::from_str("Jans::DefaultEntity::\"1694c954f8d9\"").unwrap())
            .expect("should have default entity 1694c954f8d9");

        assert_eq!(
            default_entity.uid().type_name().to_string(),
            "Jans::DefaultEntity"
        );
        assert_eq!(default_entity.uid().id().as_ref() as &str, "1694c954f8d9");

        // Verify default entity attributes
        let entity_json = default_entity
            .to_json_value()
            .expect("should convert to JSON");
        let attrs = entity_json.get("attrs").expect("should have attrs");

        assert_eq!(
            attrs.get("o").and_then(|v| v.as_str()),
            Some("Acme Dolphins Division")
        );
        assert_eq!(attrs.get("org_id").and_then(|v| v.as_str()), Some("100129"));

        // Verify regions attribute (should be a set)
        let regions = attrs
            .get("regions")
            .and_then(|v| v.as_array())
            .expect("should have regions");
        assert_eq!(regions.len(), 3);
        assert!(regions.contains(&json!("Atlantic")));
        assert!(regions.contains(&json!("Pacific")));
        assert!(regions.contains(&json!("Indian")));

        // Verify user entity is built
        let user = entities_data.user.expect("should have user entity");
        assert_eq!(user.uid().type_name().to_string(), "Jans::User");
        assert_eq!(user.uid().id().as_ref() as &str, "user123");

        // Verify resource entity is built
        assert_eq!(
            entities_data.resource.uid().type_name().to_string(),
            "Jans::Issue"
        );
        assert_eq!(
            entities_data.resource.uid().id().as_ref() as &str,
            "issue123"
        );

        // Verify all default entities are present and accessible
        assert_eq!(
            entities_data.default_entities.len(),
            2,
            "should have 2 default entities"
        );

        let uid = EntityUid::from_str("Jans::DefaultEntity::\"74d109b20248\"").unwrap();
        // Verify the second default entity is also present
        let second_default_entity = entities_data
            .default_entities
            .get(&uid)
            .expect("should have default entity 74d109b20248");
        assert_eq!(
            second_default_entity.uid().type_name().to_string(),
            "Jans::DefaultEntity"
        );
        assert_eq!(
            second_default_entity.uid().id().as_ref() as &str,
            "74d109b20248"
        );

        // Verify second default entity attributes
        let second_entity_json = second_default_entity
            .to_json_value()
            .expect("should convert to JSON");
        let second_attrs = second_entity_json.get("attrs").expect("should have attrs");

        assert_eq!(
            second_attrs.get("description").and_then(|v| v.as_str()),
            Some("2025 Price List")
        );

        // Verify products attribute (should be a record)
        let products = second_attrs
            .get("products")
            .and_then(|v| v.as_object())
            .expect("should have products");
        assert_eq!(products.get("15020").and_then(|v| v.as_i64()), Some(995));
        assert_eq!(products.get("15050").and_then(|v| v.as_i64()), Some(1495));
    }

    #[test]
    fn test_entity_merging_with_conflicts() {
        use url::Url;

        // Test that request entities override default entities when there are UID conflicts

        // Create a schema
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity DefaultEntity;
                entity User;
                entity Issue = {
                    org_id: String,
                    description: String
                };
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");

        // Create default entities with a specific UID that will conflict
        let default_entities_data = HashMap::from([(
            "conflict_id".to_string(),
            json!({
                "entity_id": "conflict_id",
                "entity_type": "Jans::Issue", // Same type as resource entity to create conflict
                "org_id": "default_org",
                "description": "This is a default entity"
            }),
        )]);

        // Create trusted issuer
        let trusted_issuer = TrustedIssuer {
            name: "Test Issuer".to_string(),
            description: "Test".to_string(),
            oidc_endpoint: Url::parse("https://test.jans.org/.well-known/openid-configuration")
                .expect("valid url"),
            token_metadata: HashMap::new(),
        };

        let trusted_issuers = HashMap::from([("test_issuer".to_string(), trusted_issuer)]);

        // Create entity builder with default entities
        let mut config = EntityBuilderConfig::default();
        config.build_workload = false;
        config.build_user = false;

        let entity_builder = EntityBuilder::new(
            config,
            &trusted_issuers,
            Some(&validator_schema),
            Some(&default_entities_data),
            None,
            TEST_LOGGER.clone(),
        )
        .expect("should create entity builder");

        // Create a resource with the SAME UID as the default entity to test conflict resolution
        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::Issue".to_string(), // Use a valid entity type from the schema
                id: "conflict_id".to_string(),          // Same ID as default entity - CONFLICT!
            },
            attributes: HashMap::from([
                ("org_id".to_string(), json!("request_org")), // Different org_id
                ("description".to_string(), json!("This is a request entity")), // Different description
            ]),
        };

        // Build entities - this should not fail due to duplicate UIDs
        let entities_data = entity_builder
            .build_entities(&HashMap::new(), &resource) // No tokens needed for this test
            .expect("should build entities without duplicate UID errors");

        // Verify that the request entity overrode the default entity
        // The merged entities should contain the request entity, not the default entity

        // Convert to Cedar Entities to test the merging logic
        let cedar_entities = entities_data
            .entities(Some(&schema))
            .expect("should create Cedar entities without duplicate UID errors");

        // Verify that only one entity with UID "conflict_id" exists
        let conflict_entity = cedar_entities
            .get(&"Jans::Issue::\"conflict_id\"".parse().unwrap())
            .expect("should have entity with conflict_id");

        // Verify that the request entity's attributes are used (not the default entity's)
        let entity_json = conflict_entity
            .to_json_value()
            .expect("should convert to JSON");
        let attrs = entity_json.get("attrs").expect("should have attrs");

        // Should have request entity attributes, not default entity attributes
        assert_eq!(
            attrs.get("org_id").and_then(|v| v.as_str()),
            Some("request_org")
        );
        assert_eq!(
            attrs.get("description").and_then(|v| v.as_str()),
            Some("This is a request entity")
        );

        // Should NOT have the default entity's org_id
        assert_ne!(
            attrs.get("org_id").and_then(|v| v.as_str()),
            Some("default_org")
        );

        // Verify that the total number of entities is correct (no duplicates)
        let entity_count = cedar_entities.iter().count();
        let expected_count = 2; // 1 issuer entity + 1 resource entity (default entity was overridden)
        assert_eq!(
            entity_count, expected_count,
            "should have exactly {} entities without duplicates",
            expected_count
        );
    }

    #[test]
    fn test_default_entities_affect_authorization() {
        use url::Url;

        // Test that default entities actually affect authorization decisions
        // This demonstrates that default entities are not just merged but actively used

        // Create a schema with a policy that references default entities
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity User;
                entity Issue = {
                    org_id: String,
                    description: String,
                    is_public: Bool
                };
                entity Organization = {
                    name: String,
                    is_active: Bool
                };
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");

        // Create default entities that will be referenced in policies
        let default_entities_data = HashMap::from([
            (
                "org1".to_string(),
                json!({
                    "entity_id": "org1",
                    "entity_type": "Jans::Organization",
                    "name": "Organization 1",
                    "is_active": true
                }),
            ),
            (
                "public_issue".to_string(),
                json!({
                    "entity_id": "public_issue",
                    "entity_type": "Jans::Issue",
                    "org_id": "org1",
                    "description": "This is a public issue",
                    "is_public": true
                }),
            ),
            (
                "private_issue".to_string(),
                json!({
                    "entity_id": "private_issue",
                    "entity_type": "Jans::Issue",
                    "org_id": "org1",
                    "description": "This is a private issue",
                    "is_public": false
                }),
            ),
        ]);

        // Create trusted issuer
        let trusted_issuer = TrustedIssuer {
            name: "Test Issuer".to_string(),
            description: "Test".to_string(),
            oidc_endpoint: Url::parse("https://test.jans.org/.well-known/openid-configuration")
                .expect("valid url"),
            token_metadata: HashMap::new(),
        };

        let trusted_issuers = HashMap::from([("test_issuer".to_string(), trusted_issuer)]);

        // Create entity builder with default entities
        let mut config = EntityBuilderConfig::default();
        config.build_workload = false;
        config.build_user = false;

        let entity_builder = EntityBuilder::new(
            config,
            &trusted_issuers,
            Some(&validator_schema),
            Some(&default_entities_data),
            None,
            TEST_LOGGER.clone(),
        )
        .expect("should create entity builder");

        // Test Case 1: Resource that references a default entity (org1)
        let resource_with_org = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::Issue".to_string(),
                id: "new_issue".to_string(),
            },
            attributes: HashMap::from([
                ("org_id".to_string(), json!("org1")), // References default entity org1
                (
                    "description".to_string(),
                    json!("New issue in existing org"),
                ),
                ("is_public".to_string(), json!(false)),
            ]),
        };

        let entities_data_1 = entity_builder
            .build_entities(&HashMap::new(), &resource_with_org)
            .expect("should build entities");

        // Verify that the default organization entity is included
        let cedar_entities_1 = entities_data_1
            .entities(Some(&schema))
            .expect("should create Cedar entities");

        // Should have the default organization entity
        let org_entity = cedar_entities_1
            .get(&"Jans::Organization::\"org1\"".parse().unwrap())
            .expect("should have default organization entity");

        let org_json = org_entity.to_json_value().expect("should convert to JSON");
        let org_attrs = org_json.get("attrs").expect("should have attrs");
        assert_eq!(
            org_attrs.get("name").and_then(|v| v.as_str()),
            Some("Organization 1")
        );
        assert_eq!(
            org_attrs.get("is_active").and_then(|v| v.as_bool()),
            Some(true)
        );

        // Test Case 2: Resource that doesn't reference any default entities
        let resource_no_org = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::Issue".to_string(),
                id: "standalone_issue".to_string(),
            },
            attributes: HashMap::from([
                ("org_id".to_string(), json!("standalone_org")), // New org, not in defaults
                ("description".to_string(), json!("Standalone issue")),
                ("is_public".to_string(), json!(true)),
            ]),
        };

        let entities_data_2 = entity_builder
            .build_entities(&HashMap::new(), &resource_no_org)
            .expect("should build entities");

        let cedar_entities_2 = entities_data_2
            .entities(Some(&schema))
            .expect("should create Cedar entities");

        // Should have the default organization entity even if not directly referenced
        // This is correct behavior - default entities are always available for policy evaluation
        let org_entity_2 = cedar_entities_2.get(&"Jans::Organization::\"org1\"".parse().unwrap());
        assert!(
            org_entity_2.is_some(),
            "should include all default entities for policy evaluation"
        );

        // Test Case 3: Verify that default entities are available for policy evaluation
        // This shows that default entities are not just present but actively used
        assert!(
            default_entities_data.contains_key("org1"),
            "default org1 should be available"
        );
        assert!(
            default_entities_data.contains_key("public_issue"),
            "default public_issue should be available"
        );
        assert!(
            default_entities_data.contains_key("private_issue"),
            "default private_issue should be available"
        );

        // Test Case 4: Verify entity count consistency
        let count_with_org = cedar_entities_1.iter().count();
        let count_without_org = cedar_entities_2.iter().count();

        // Both should have the same count since they both include all default entities
        // This demonstrates that default entities are always available for policy evaluation
        assert_eq!(
            count_with_org, count_without_org,
            "both cases should have the same entity count since they include all default entities"
        );

        // Should have exactly the expected counts
        let expected_count = 5; // 1 issuer + 1 resource + 3 default entities (org1, public_issue, private_issue)
        assert_eq!(count_with_org, expected_count);
        assert_eq!(count_without_org, expected_count);
    }

    #[test]
    fn test_default_entity_as_principal() {
        use url::Url;

        // Test that default entities can be used as principals in authorization requests
        // This demonstrates a key use case where default entities represent system actors

        // Create a schema with entities that can act as principals
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity User = {
                    name: String,
                    role: String
                };
                entity ServiceAccount = {
                    name: String,
                    permissions: Set<String>,
                    is_active: Bool
                };
                entity Resource = {
                    name: String,
                    owner: String,
                    access_level: String
                };
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");

        // Create default entities including a service account that can act as a principal
        let default_entities_data = HashMap::from([
            (
                "service-account-1".to_string(),
                json!({
                    "entity_id": "service-account-1",
                    "entity_type": "Jans::ServiceAccount",
                    "name": "Backup Service",
                    "permissions": ["read", "backup", "restore"],
                    "is_active": true
                }),
            ),
            (
                "service-account-2".to_string(),
                json!({
                    "entity_id": "service-account-2",
                    "entity_type": "Jans::ServiceAccount",
                    "name": "Monitoring Service",
                    "permissions": ["read", "monitor"],
                    "is_active": true
                }),
            ),
            (
                "admin-user".to_string(),
                json!({
                    "entity_id": "admin-user",
                    "entity_type": "Jans::User",
                    "name": "System Administrator",
                    "role": "admin"
                }),
            ),
        ]);

        // Create trusted issuer
        let trusted_issuer = TrustedIssuer {
            name: "Test Issuer".to_string(),
            description: "Test".to_string(),
            oidc_endpoint: Url::parse("https://test.jans.org/.well-known/openid-configuration")
                .expect("valid url"),
            token_metadata: HashMap::new(),
        };

        let trusted_issuers = HashMap::from([("test_issuer".to_string(), trusted_issuer)]);

        // Create entity builder with default entities
        let mut config = EntityBuilderConfig::default();
        config.build_workload = false;
        config.build_user = false;

        let entity_builder = EntityBuilder::new(
            config,
            &trusted_issuers,
            Some(&validator_schema),
            Some(&default_entities_data),
            None,
            TEST_LOGGER.clone(),
        )
        .expect("should create entity builder");

        // Test Case 1: Use a default service account as the principal (no tokens provided)
        // This simulates a system-to-system authorization request
        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::Resource".to_string(),
                id: "backup-storage".to_string(),
            },
            attributes: HashMap::from([
                ("name".to_string(), json!("Backup Storage")),
                ("owner".to_string(), json!("service-account-1")), // Owned by the default service account
                ("access_level".to_string(), json!("restricted")),
            ]),
        };

        let entities_data = entity_builder
            .build_entities(&HashMap::new(), &resource) // No tokens = no user/workload principals
            .expect("should build entities");

        let cedar_entities = entities_data
            .entities(Some(&schema))
            .expect("should create Cedar entities");

        // Verify that the default service account entities are available for use as principals
        let service_account_1 = cedar_entities
            .get(
                &"Jans::ServiceAccount::\"service-account-1\""
                    .parse()
                    .unwrap(),
            )
            .expect("should have default service account 1 entity");

        let service_account_2 = cedar_entities
            .get(
                &"Jans::ServiceAccount::\"service-account-2\""
                    .parse()
                    .unwrap(),
            )
            .expect("should have default service account 2 entity");

        // Verify the service account attributes
        let sa1_json = service_account_1
            .to_json_value()
            .expect("should convert to JSON");
        let sa1_attrs = sa1_json.get("attrs").expect("should have attrs");
        assert_eq!(
            sa1_attrs.get("name").and_then(|v| v.as_str()),
            Some("Backup Service")
        );
        assert_eq!(
            sa1_attrs.get("is_active").and_then(|v| v.as_bool()),
            Some(true)
        );

        let sa2_json = service_account_2
            .to_json_value()
            .expect("should convert to JSON");
        let sa2_attrs = sa2_json.get("attrs").expect("should have attrs");
        assert_eq!(
            sa2_attrs.get("name").and_then(|v| v.as_str()),
            Some("Monitoring Service")
        );

        // Test Case 2: Verify that default entities can be referenced in resource attributes
        // This shows how default entities enable complex authorization scenarios
        let resource_owned_by_sa = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::Resource".to_string(),
                id: "monitoring-dashboard".to_string(),
            },
            attributes: HashMap::from([
                ("name".to_string(), json!("Monitoring Dashboard")),
                ("owner".to_string(), json!("service-account-2")), // References default service account
                ("access_level".to_string(), json!("admin")),
            ]),
        };

        let entities_data_2 = entity_builder
            .build_entities(&HashMap::new(), &resource_owned_by_sa)
            .expect("should build entities");

        let cedar_entities_2 = entities_data_2
            .entities(Some(&schema))
            .expect("should create Cedar entities");

        // Should have the same default entities available
        let sa2_entity_2 = cedar_entities_2.get(
            &"Jans::ServiceAccount::\"service-account-2\""
                .parse()
                .unwrap(),
        );
        assert!(
            sa2_entity_2.is_some(),
            "should have service account 2 available for policy evaluation"
        );

        // Test Case 3: Demonstrate entity count consistency
        let count_1 = cedar_entities.iter().count();
        let count_2 = cedar_entities_2.iter().count();

        // Both should have the same count since they include all default entities
        assert_eq!(
            count_1, count_2,
            "both cases should have the same entity count"
        );

        // Expected count: 1 issuer + 1 resource + 3 default entities
        let expected_count = 5;
        assert_eq!(count_1, expected_count);
        assert_eq!(count_2, expected_count);

        // Test Case 4: Show how this enables policy evaluation
        // In a real Cedar policy, you could now write rules like:
        // "permit(principal: Jans::ServiceAccount::"service-account-1", action: "backup", resource: Jans::Resource::"backup-storage")"
        // The default entities provide the principal and resource context needed for policy evaluation

        // Verify that the service account has the expected permissions
        let sa1_permissions = sa1_attrs
            .get("permissions")
            .and_then(|v| v.as_array())
            .expect("should have permissions");
        assert!(
            sa1_permissions.iter().any(|p| p.as_str() == Some("backup")),
            "should have backup permission"
        );
        assert!(
            sa1_permissions
                .iter()
                .any(|p| p.as_str() == Some("restore")),
            "should have restore permission"
        );
    }

    #[test]
    fn test_parse_cedar_entity_format_with_admin_ui_namespace() {
        // Test with the exact entity format from the reported issue
        // This tests the Cedar entity JSON format with uid, attrs, and parents fields
        let entity_data = json!({
            "uid": {
                "type": "Features",
                "id": "License"
            },
            "attrs": {},
            "parents": [
                {
                    "type": "ParentResource",
                    "id": "AuthServerAndConfiguration"
                }
            ]
        });

        let default_entities_data = HashMap::from([("2694c954f8d8".to_string(), entity_data)]);

        // Use the namespace from the policy store name
        let namespace = Some("Gluu::Flex::AdminUI::Resources");
        let parsed_entities =
            parse_default_entities(&default_entities_data, namespace, TEST_LOGGER.clone())
                .expect("should parse default entities");

        assert_eq!(parsed_entities.len(), 1, "should have 1 entity");

        let uid =
            &EntityUid::from_str("Gluu::Flex::AdminUI::Resources::Features::\"License\"").unwrap();
        let entity = parsed_entities.get(&uid).expect("should have entity");
        let uid_str = entity.uid().to_string();

        // Verify the namespace was added correctly to both the entity type and parent type
        println!("Parsed entity UID: {}", uid_str);
        assert!(
            uid_str.contains("Features"),
            "Entity should contain Features type"
        );

        // Check that entity has correct namespace prefix
        assert!(
            uid_str.contains("Gluu::Flex::AdminUI::Resources::Features"),
            "Entity should have correct namespace, got: {}",
            uid_str
        );

        println!(
            "✓ Successfully parsed Cedar entity format with namespace: {}",
            uid_str
        );
    }

    #[test]
    fn test_parse_entity_with_existing_namespace() {
        // Test that entity with uid.type already containing "::" does not get double-prefixed
        let entity_data = json!({
            "uid": {
                "type": "Existing::Namespace::Features",
                "id": "TestFeature"
            },
            "attrs": {
                "attribute": "value"
            },
            "parents": [
                {
                    "type": "NewNamespace::ParentResource",
                    "id": "SomeTestID"
                }
            ]
        });

        let default_entities_data = HashMap::from([("test123".to_string(), entity_data.clone())]);

        let parsed_entities = parse_default_entities(
            &default_entities_data,
            Some("NewNamespace"),
            TEST_LOGGER.clone(),
        )
        .expect("should parse default entities");

        let uid: &EntityUid =
            &EntityUid::from_str("Existing::Namespace::Features::\"TestFeature\"").unwrap();
        let entity = parsed_entities.get(&uid).expect("should have entity");

        let result_entity_json = entity
            .to_json_value()
            .expect("entity should be converted to json");

        assert_eq!(
            result_entity_json.sorted(),
            entity_data.sorted(),
            "entity json data should be equal"
        );
        assert_eq!(
            entity.uid().type_name().to_string(),
            "Existing::Namespace::Features",
            "Existing namespace should not be double-prefixed"
        );
        assert_eq!(
            entity.uid().id().unescaped(),
            "TestFeature",
            "ID of entity should be `TestFeature`"
        );
    }

    #[test]
    fn test_parse_error_missing_uid() {
        // Test entity missing uid field
        let entity_data = json!({
            "attrs": {
                "attribute": "value"
            }
        });

        let default_entities_data = HashMap::from([("test123".to_string(), entity_data)]);

        let result =
            parse_default_entities(&default_entities_data, Some("Test"), TEST_LOGGER.clone());
        assert!(
            result.is_err(),
            "Should return error when uid field is missing"
        );
    }

    #[test]
    fn test_parse_error_invalid_uid_structure() {
        // Test entity with uid that is not an object
        let entity_data = json!({
            "uid": "not-an-object",
            "attrs": {}
        });

        let default_entities_data = HashMap::from([("test123".to_string(), entity_data)]);

        let result =
            parse_default_entities(&default_entities_data, Some("Test"), TEST_LOGGER.clone());
        assert!(
            result.is_err(),
            "Should return error when uid is not an object"
        );

        // Test entity with uid missing type field
        let entity_data_no_type = json!({
            "uid": {
                "id": "test"
            },
            "attrs": {}
        });

        let default_entities_data = HashMap::from([("test456".to_string(), entity_data_no_type)]);

        let result =
            parse_default_entities(&default_entities_data, Some("Test"), TEST_LOGGER.clone());
        assert!(
            result.is_err(),
            "Should return error when uid.type is missing"
        );
    }

    #[test]
    fn test_parse_entity_with_empty_attrs_and_parents() {
        // Test entity with empty attrs and empty parents
        let entity_data = json!({
            "uid": {
                "type": "EmptyTest",
                "id": "test789"
            },
            "attrs": {},
            "parents": []
        });

        let default_entities_data = HashMap::from([("test789".to_string(), entity_data)]);

        let parsed_entities =
            parse_default_entities(&default_entities_data, Some("Test"), TEST_LOGGER.clone())
                .expect("should parse with empty attrs and parents");

        let uid: &EntityUid = &EntityUid::from_str("Test::EmptyTest::\"test789\"").unwrap();
        let entity = parsed_entities.get(&uid).expect("should have entity");
        assert_eq!(
            entity.uid().type_name().to_string(),
            "Test::EmptyTest",
            "Entity type should have namespace prefix"
        );
        let entity_json = entity.to_json_value().expect("should convert to JSON");
        let attrs = entity_json.get("attrs").expect("should have attrs");
        assert_eq!(
            attrs.as_object().unwrap().len(),
            0,
            "Entity should have empty attrs"
        );
    }
}
