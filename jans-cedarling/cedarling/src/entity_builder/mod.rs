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
pub(crate) mod value_to_expr;

use crate::authz::request::EntityData;
use crate::common::PartitionResult;
use crate::common::default_entities::DefaultEntities;
use crate::common::issuer_utils::IssClaim;
use crate::common::policy_store::{IssuerName, TokenTypes, TrustedIssuer};
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

pub(crate) use build_multi_issuer_entity::{MultiIssuerEntityError, MultiIssuerSetupEntities};
pub(crate) use built_entities::BuiltEntities;

pub(crate) use error::*;

pub(crate) struct EntityBuilder {
    /// One per issuer identity `{name}::TrustedIssuer::"<iss>"`, so issuers
    /// sharing an origin or an `iss` do not overwrite each other.
    iss_entities: HashMap<EntityUid, Entity>,
    schema: Option<MappingSchema>,
    default_entities: DefaultEntities,
    /// Every configured token type, for its `context.tokens` key and token id.
    token_types: TokenTypes,
}

impl EntityBuilder {
    pub(super) fn new(
        token_types: &TokenTypes,
        schema: Option<&ValidatorSchema>,
        default_entities: DefaultEntities,
    ) -> Result<Self, InitEntityBuilderError> {
        let schema = schema.map(MappingSchema::try_from).transpose()?;

        // Sorted so the same misconfiguration is always reported the same way.
        let mut issuers: Vec<&TrustedIssuer> = token_types
            .jwt_issuers()
            .values()
            .map(AsRef::as_ref)
            .collect();
        issuers.sort_by(|a, b| a.name.cmp(&b.name));

        if let Some(schema) = schema.as_ref() {
            Self::validate_iss_types(token_types, &issuers, schema)?;
        }

        let (ok, errs) = issuers
            .iter()
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

        let iss_entities = ok
            .into_iter()
            .map(|entity: Entity| (entity.uid(), entity))
            .collect::<HashMap<EntityUid, Entity>>();

        Ok(Self {
            iss_entities,
            schema,
            default_entities,
            token_types: token_types.clone(),
        })
    }

    /// Build just the principal entity for an unsigned request, without a resource.
    ///
    /// Callers that need to amortize principal construction across many items
    /// (batch authorization) call this once and then invoke
    /// [`Self::build_resource_entity`] per item.
    pub(crate) fn build_unsigned_principal(
        &self,
        principal: Option<&EntityData>,
    ) -> Result<UnsignedPrincipalBuild, BuildUnsignedEntityError> {
        let mut built_entities = BuiltEntities::default();
        let principal = match principal {
            Some(principal) => {
                let entity = self.build_principal_unsigned(principal, &built_entities)?;
                built_entities.insert(&entity.uid());
                Some(entity)
            },
            None => None,
        };
        Ok(UnsignedPrincipalBuild {
            principal,
            built_entities,
        })
    }

    /// Rejects a token whose schema `iss` type cannot match what the builder emits.
    fn validate_iss_types(
        token_types: &TokenTypes,
        issuers: &[&TrustedIssuer],
        schema: &MappingSchema,
    ) -> Result<(), InitEntityBuilderError> {
        const ISS_CLAIM: &str = "iss";

        for iss in issuers {
            let expected_iss_type = Self::trusted_issuer_typename(&iss.name);

            for (entity_type_name, _) in token_types.jwt_tokens_of(&iss.name) {
                let entity_type_name = entity_type_name.as_str();
                let Some(shape) = schema.get_entity_shape(entity_type_name) else {
                    continue;
                };
                let Some(iss_shape) = shape.get(ISS_CLAIM) else {
                    continue;
                };
                // Only an entity-reference `iss` constrains the type. A `String`
                // `iss` is not checked here: for JWT tokens `add_iss_claim` still
                // overwrites it with an `EntityUid`, so such a declaration would be
                // a silent schema mismatch rather than an accepted value.
                if let schema::AttrSrc::EntityRef(schema::EntityRefAttrSrc(schema_iss_type)) =
                    iss_shape.src()
                    && schema_iss_type.as_str() != expected_iss_type
                {
                    return Err(InitEntityBuilderError::IssTypeMismatch {
                        issuer_name: iss.name.to_string(),
                        schema_iss_type: schema_iss_type.to_string(),
                        expected_iss_type,
                    });
                }
            }
        }

        Ok(())
    }

    pub(super) fn trusted_issuer_typename(namespace: &IssuerName) -> String {
        format!("{namespace}::TrustedIssuer")
    }

    pub(super) fn trusted_issuer_cedar_uid(
        namespace: &IssuerName,
        iss_url: &IssClaim,
    ) -> Result<EntityUid, BuildEntityError> {
        build_cedar_uid(&format!("{namespace}::TrustedIssuer"), iss_url.as_str())
    }

    #[cfg(test)]
    // is used only for testing to get trusted issuer
    fn find_trusted_issuer_by_iss(&self, issuer: &str) -> Option<Arc<TrustedIssuer>> {
        let issuer = IssClaim::new(issuer);
        self.token_types
            .jwt_issuers()
            .values()
            .find(|iss| iss.iss_claim() == issuer)
            .cloned()
    }
}

/// Output of [`EntityBuilder::build_unsigned_principal`].
pub(crate) struct UnsignedPrincipalBuild {
    pub principal: Option<Entity>,
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

#[derive(Default)]
pub(super) struct TokenPrincipalMappings(HashMap<String, Vec<(String, RestrictedExpression)>>);

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
    use crate::common::policy_store::{TrustedIssuer, token_entity_metadata::TokenEntityMetadata};
    use cedar_policy::{Entities, Schema};
    use serde_json::Value;
    use std::collections::HashMap;
    use std::sync::LazyLock;
    use test_utils::assert_eq;
    use url::Url;

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

    #[test]
    fn rejects_iss_type_from_foreign_namespace() {
        const SCHEMA: &str = r"
        namespace Jans {
            entity TrustedIssuer;
            entity Access_token = { iss: TrustedIssuer };
        }
        namespace Alpha {
            entity TrustedIssuer;
            entity Access_token = { iss: TrustedIssuer };
        }";

        let iss_name: &str = "Alpha";
        let token_metadata = HashMap::from([(
            "Jans::Access_token".to_string(),
            TokenEntityMetadata::builder().build(),
        )]);
        let issuers = HashMap::from([(
            iss_name.to_lowercase(),
            TrustedIssuer::discovery_at(
                iss_name,
                Url::parse("https://idp.example/auth").expect("valid url"),
            )
            .with_tokens(token_metadata),
        )]);

        let schema = ValidatorSchema::from_str(SCHEMA).expect("valid schema");
        let err = EntityBuilder::new(
            &TokenTypes::build(issuers.into_values(), []).expect("test token types should build"),
            Some(&schema),
            DefaultEntities::default(),
        )
        .map(|_| ())
        .expect_err("mismatched iss namespace must be rejected at startup");

        assert!(
            matches!(
                err,
                InitEntityBuilderError::IssTypeMismatch {
                    ref schema_iss_type,
                    ref expected_iss_type,
                    ..
                } if schema_iss_type == "Jans::TrustedIssuer"
                    && expected_iss_type == "Alpha::TrustedIssuer"
            ),
            "expected mismatched iss namespace to be rejected, got: {err}"
        );
    }
}
