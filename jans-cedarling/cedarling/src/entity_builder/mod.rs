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
mod build_multi_issuer_entity;
mod build_principal_entity;
mod build_resource_entity;
mod built_entities;
mod entity_id_getters;
mod error;
mod schema;
mod trusted_issuer_index;
pub(crate) mod value_to_expr;

use crate::authz::request::EntityData;
use crate::common::PartitionResult;
use crate::common::default_entities::DefaultEntities;
use crate::common::issuer_utils::IssClaim;
#[cfg(test)]
use crate::common::policy_store::TrustedIssuer;
use crate::entity_builder::build_principal_entity::BuiltPrincipalUnsigned;
use crate::{
    RequestUnsigned,
    entity_builder_config::{
        DEFAULT_ACCESS_TKN_ENTITY_NAME, DEFAULT_ENTITY_TYPE_NAME, DEFAULT_ID_TKN_ENTITY_NAME,
        DEFAULT_USERINFO_TKN_ENTITY_NAME, EntityBuilderConfig,
    },
};
use build_entity_attrs::build_entity_attrs;
use build_iss_entity::build_iss_entity;
use cedar_policy::{Entity, EntityId, EntityTypeName, EntityUid, RestrictedExpression};
use cedar_policy_core::validator::ValidatorSchema;
use schema::MappingSchema;
use serde_json::Value;
use std::collections::{HashMap, HashSet};
use std::str::FromStr;
#[cfg(test)]
use std::sync::Arc;
use url::Origin;

pub(crate) use crate::entity_builder::trusted_issuer_index::TrustedIssuerIndex;
pub(crate) use build_multi_issuer_entity::MultiIssuerEntityError;
pub(crate) use built_entities::BuiltEntities;

pub(crate) use error::*;

pub(crate) struct EntityBuilder {
    config: EntityBuilderConfig,
    iss_entities: HashMap<Origin, Entity>,
    schema: Option<MappingSchema>,
    default_entities: DefaultEntities,
    issuers_index: TrustedIssuerIndex,
}

impl EntityBuilder {
    pub(super) fn new(
        config: EntityBuilderConfig,
        issuers_index: TrustedIssuerIndex,
        schema: Option<&ValidatorSchema>,
        default_entities: DefaultEntities,
    ) -> Result<Self, InitEntityBuilderError> {
        let schema = schema.map(MappingSchema::try_from).transpose()?;

        let (ok, errs) = issuers_index
            .values()
            .map(|iss| {
                let iss_id = iss.iss_claim();

                let iss_type_name = Self::trusted_issuer_typename(&iss.name);
                build_iss_entity(
                    &iss_type_name.clone(),
                    iss_id.as_str(),
                    iss,
                    schema.as_ref(),
                )
            })
            .partition_result();

        if !errs.is_empty() {
            return Err(InitEntityBuilderError::BuildIssEntities(errs.into()));
        }

        let iss_entities = ok.into_iter().collect::<HashMap<Origin, Entity>>();

        Ok(Self {
            config,
            iss_entities,
            schema,
            default_entities,
            issuers_index,
        })
    }

    /// Builds the entities using the unsigned interface
    pub(super) fn build_entities_unsigned(
        &self,
        request: &RequestUnsigned,
    ) -> Result<BuiltEntitiesUnsigned, BuildUnsignedEntityError> {
        let mut built_entities = BuiltEntities::default();

        let mut principals = Vec::with_capacity(request.principals.len());
        let mut roles = Vec::<Entity>::new();
        for principal in &request.principals {
            let BuiltPrincipalUnsigned { principal, parents } =
                self.build_principal_unsigned(principal, &built_entities)?;

            built_entities.insert(&principal.uid());
            for role in &roles {
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

    pub(super) fn trusted_issuer_typename(namespace: &str) -> String {
        format!("{namespace}::TrustedIssuer")
    }

    pub(super) fn trusted_issuer_cedar_uid(
        namespace: &str,
        iss_url: &IssClaim,
    ) -> Result<EntityUid, BuildEntityError> {
        build_cedar_uid(&format!("{namespace}::TrustedIssuer"), iss_url.as_str())
    }

    #[cfg(test)]
    // is used only for testing to get trusted issuer
    fn find_trusted_issuer_by_iss(&self, issuer: &str) -> Option<Arc<TrustedIssuer>> {
        self.issuers_index.find(issuer).cloned()
    }
}

pub(super) struct BuiltEntitiesUnsigned {
    pub principals: Vec<Entity>,
    pub roles: Vec<Entity>,
    pub resource: Entity,
    pub built_entities: BuiltEntities,
}

fn build_cedar_uid(type_name: &str, id: &str) -> Result<EntityUid, BuildEntityError> {
    let entity_type_name = EntityTypeName::from_str(type_name)
        .map_err(|e| BuildEntityErrorKind::from(Box::new(e)).while_building(type_name))?;
    // EntityId::from_str returns Result<_, Infallible>, so parsing never fails
    let entity_id = EntityId::from_str(id).unwrap_or_else(|e| match e {});

    Ok(EntityUid::from_type_name_and_id(
        entity_type_name,
        entity_id,
    ))
}

pub(super) fn build_cedar_entity(
    type_name: &str,
    id: &str,
    attrs: HashMap<String, RestrictedExpression>,
    parents: HashSet<EntityUid>,
) -> Result<Entity, BuildEntityError> {
    let uid = build_cedar_uid(type_name, id)?;
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
pub(super) struct TokenPrincipalMappings(HashMap<String, Vec<(String, RestrictedExpression)>>);

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
pub(super) struct TokenPrincipalMapping {
    /// The principal where token will be inserted
    principal: String,
    /// The name of the attribute of the token
    attr_name: String,
    /// An `EntityUID` reference to the token
    expr: RestrictedExpression,
}

impl TokenPrincipalMappings {
    fn get(&self, principal: &str) -> Option<&Vec<(String, RestrictedExpression)>> {
        self.0.get(principal)
    }

    fn apply(&self, principal_type: &str, attributes: &mut HashMap<String, RestrictedExpression>) {
        let Some(mappings) = self.get(principal_type) else {
            return;
        };

        attributes.extend(mappings.iter().cloned());
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use cedar_policy::{Entities, Schema};
    use serde_json::Value;
    use std::collections::HashMap;
    use std::sync::LazyLock;
    use test_utils::assert_eq;

    pub(super) static CEDARLING_VALIDATOR_SCHEMA: LazyLock<ValidatorSchema> = LazyLock::new(|| {
        ValidatorSchema::from_str(include_str!("../../../schema/cedarling_core.cedarschema"))
            .expect("should be a valid Cedar validator schema")
    });

    pub(super) static CEDARLING_API_SCHEMA: LazyLock<Schema> = LazyLock::new(|| {
        Schema::from_str(include_str!("../../../schema/cedarling_core.cedarschema"))
            .expect("should be a valid Cedar schema")
    });

    /// Helper function for asserting entities for better error readability
    #[track_caller]
    pub(super) fn assert_entity_eq(entity: &Entity, expected: &Value, schema: Option<&Schema>) {
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
        for (name, expected_val) in &expected_attrs {
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
}
