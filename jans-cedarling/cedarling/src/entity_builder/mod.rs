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
mod build_resource_entity;
mod build_role_entity;
mod build_token_entities;
mod build_user_entity;
mod build_workload_entity;
mod built_entities;
mod entity_id_getters;
mod error;
mod schema;
mod value_to_expr;

use crate::ResourceData;
use crate::authz::AuthorizeEntitiesData;
use crate::common::policy_store::{ClaimMappings, TrustedIssuer};
use crate::entity_builder_config::*;
use crate::jwt::Token;
use build_entity_attrs::*;
use build_iss_entity::build_iss_entity;
use built_entities::BuiltEntities;
use cedar_policy::{Entity, EntityUid, RestrictedExpression};
use cedar_policy_validator::ValidatorSchema;
use schema::MappingSchema;
use serde_json::Value;
use std::collections::{HashMap, HashSet};
use std::str::FromStr;
use url::Origin;

pub(crate) use built_entities::OldBuiltEntities;
pub use error::*;

pub struct EntityBuilder {
    config: EntityBuilderConfig,
    iss_entities: HashMap<Origin, Entity>,
    schema: Option<MappingSchema>,
}

impl EntityBuilder {
    pub fn new(
        config: EntityBuilderConfig,
        trusted_issuers: &HashMap<String, TrustedIssuer>,
        schema: Option<&ValidatorSchema>,
    ) -> Result<Self, InitEntityBuilderError> {
        let schema = schema.map(MappingSchema::try_from).transpose()?;

        let (ok, errs): (Vec<_>, Vec<_>) = trusted_issuers
            .iter()
            .map(|(iss_id, iss)| {
                build_iss_entity(&config.entity_names.iss, iss_id, iss, schema.as_ref())
            })
            .partition(|result| result.is_ok());

        if !errs.is_empty() {
            let errs = errs
                .into_iter()
                .map(|e| e.unwrap_err())
                .collect::<Vec<BuildEntityError>>();
            return Err(InitEntityBuilderError::BuildIssEntities(errs.into()));
        }

        let iss_entities = ok
            .into_iter()
            .flatten()
            .collect::<HashMap<Origin, Entity>>();

        Ok(Self {
            config,
            iss_entities,
            schema,
        })
    }

    pub fn build_entities(
        &self,
        tokens: &HashMap<String, Token>,
        resource: &ResourceData,
    ) -> Result<AuthorizeEntitiesData, BuildEntityError> {
        let mut tkn_principal_mappings = TokenPrincipalMappings::default();
        let mut built_entities = BuiltEntities::from(&self.iss_entities);

        let mut token_entities = HashMap::new();
        for (tkn_name, tkn) in tokens.iter() {
            let entity_name = tkn
                .iss
                .and_then(|iss| iss.tokens_metadata.get(tkn_name))
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

        let resource = self.build_resource_entity(resource)?;

        let issuers = self.iss_entities.values().cloned().collect();
        Ok(AuthorizeEntitiesData {
            issuers,
            workload,
            user,
            resource,
            roles,
            tokens: token_entities,
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

pub fn build_cedar_entity(
    type_name: &str,
    id: &str,
    attrs: HashMap<String, RestrictedExpression>,
    parents: HashSet<EntityUid>,
) -> Result<Entity, BuildEntityError> {
    let uid = EntityUid::from_str(&format!("{}::\"{}\"", type_name, id))
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(type_name))?;
    let entity = Entity::new(uid, attrs, parents)
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(type_name))?;

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
    use crate::common::policy_store::TokenEntityMetadata;
    use cedar_policy::{Entities, Schema};
    use serde_json::{Value, json};
    use std::{collections::HashMap, sync::OnceLock};
    use test_utils::assert_eq;

    pub fn cedarling_validator_schema<'a>() -> &'a ValidatorSchema {
        static CEDARLING_VALIDATOR_SCHEMA: OnceLock<ValidatorSchema> = OnceLock::new();
        CEDARLING_VALIDATOR_SCHEMA.get_or_init(|| {
            ValidatorSchema::from_str(include_str!("../../../schema/cedarling_core.cedarschema"))
                .expect("should be a valid Cedar validator schema")
        })
    }

    pub fn cedarling_schema<'a>() -> &'a Schema {
        static CEDARLING_SCHEMA: OnceLock<Schema> = OnceLock::new();
        CEDARLING_SCHEMA.get_or_init(|| {
            Schema::from_str(include_str!("../../../schema/cedarling_core.cedarschema"))
                .expect("should be a valid Cedar schema")
        })
    }

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
        Entities::from_entities([entity.clone()], schema).expect(&format!(
            "{} entity should conform to the schema",
            entity.uid().to_string()
        ));
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
        let schema = Schema::from_str(&schema_src).expect("parse schema");
        let validator_schema =
            ValidatorSchema::from_str(&schema_src).expect("parse validation schema");

        // Set the custom workload name in the config
        let mut config = EntityBuilderConfig::default().with_workload();
        config.entity_names.workload = "Jans::CustomWorkload".into();

        // Set the custom token names in the IDP metadata
        let mut iss = TrustedIssuer::default();
        iss.tokens_metadata = HashMap::from([
            (
                "access_token".to_string(),
                TokenEntityMetadata::builder()
                    .entity_type_name("Jans::CustomAccessToken".to_string())
                    .principal_mapping(["Jans::CustomWorkload".to_string()].into_iter().collect())
                    .build(),
            ),
            (
                "custom_token".to_string(),
                TokenEntityMetadata::builder()
                    .entity_type_name("AnotherNamespace::CustomToken".to_string())
                    .principal_mapping(["Jans::CustomWorkload".to_string()].into_iter().collect())
                    .build(),
            ),
        ]);
        let issuers = HashMap::from([("some_iss".into(), iss)]);
        let tokens = HashMap::from([
            (
                "access_token".into(),
                Token::new(
                    "access_token",
                    json!({"jti": "some_jti", "aud": "some_aud"}).into(),
                    Some(&issuers.get("some_iss").unwrap()),
                ),
            ),
            (
                "custom_token".into(),
                Token::new(
                    "custom_token",
                    json!({"jti": "some_jti"}).into(),
                    Some(&issuers.get("some_iss").unwrap()),
                ),
            ),
        ]);

        let entity_builder = EntityBuilder::new(config, &issuers, Some(&validator_schema))
            .expect("init entity builder");

        let entities = entity_builder
            .build_entities(&tokens, &ResourceData {
                resource_type: "Jans::Resource".into(),
                id: "some_id".into(),
                payload: HashMap::new(),
            })
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
}
