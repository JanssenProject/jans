// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod unsigned;

use super::entity_id_getters::{EntityIdSrc, get_first_valid_entity_id};
use super::schema::AttrsShape;
use super::{
    BuildAttrsErrorVec, BuildEntityError, BuildEntityErrorKind, BuildUnsignedEntityError,
    BuiltEntities, EntityBuilder, EntityData, EntityUid, HashMap, PartitionResult,
    RestrictedExpression, TokenPrincipalMappings, Value, build_cedar_entity, build_entity_attrs,
};
use cedar_policy::Entity;
use smol_str::SmolStr;
use std::collections::HashSet;

#[derive(Clone, Copy)]
pub(super) enum AttrSrc<'a> {
    Unsigned(&'a HashMap<String, Value>),
}

impl EntityBuilder {
    fn build_principal_entity(
        &self,
        type_name: &str,
        id_srcs: &[EntityIdSrc],
        attrs_srcs: Vec<AttrSrc>,
        tkn_principal_mappings: &TokenPrincipalMappings,
        built_entities: &BuiltEntities,
        parents: HashSet<EntityUid>,
    ) -> Result<Entity, BuildEntityError> {
        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|s| s.get_entity_shape(type_name));

        let entity_id =
            get_first_valid_entity_id(id_srcs).map_err(|e| e.while_building(type_name))?;

        // Extract attributes from sources
        let ExtractedAttrsResult { attrs, errs } =
            extract_attrs_from_sources(attrs_srcs, built_entities, attrs_shape);

        let mut entity_attrs: HashMap<String, RestrictedExpression> = if errs.is_empty() {
            // what should happen if claims have the same name but different values?
            attrs.into_iter().flatten().collect::<HashMap<_, _>>()
        } else {
            let errs: Vec<_> = errs
                .into_iter()
                .flat_map(super::error::BuildAttrsErrorVec::into_inner)
                .collect();
            return Err(BuildEntityErrorKind::from(errs).while_building(type_name));
        };

        // Apply token mappings if the schema/shape is not present since that's the only
        // time it's really necessary
        if attrs_shape.is_none() {
            tkn_principal_mappings.apply(type_name, &mut entity_attrs);
        }

        let entity = build_cedar_entity(type_name, &entity_id, entity_attrs, parents)?;

        Ok(entity)
    }
}

fn extract_attrs_from_sources(
    srcs: Vec<AttrSrc<'_>>,
    built_entities: &BuiltEntities,
    attrs_shape: Option<&HashMap<SmolStr, AttrsShape>>,
) -> ExtractedAttrsResult {
    let (attrs, errs) = srcs
        .into_iter()
        .map(|src| match src {
            AttrSrc::Unsigned(src) => build_entity_attrs(src, built_entities, attrs_shape),
        })
        .partition_result();
    ExtractedAttrsResult { attrs, errs }
}

struct ExtractedAttrsResult {
    attrs: Vec<HashMap<String, RestrictedExpression>>,
    errs: Vec<BuildAttrsErrorVec>,
}

pub(crate) struct BuiltPrincipalUnsigned {
    pub principal: Entity,
    pub parents: Vec<Entity>,
}
