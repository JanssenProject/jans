// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{
    AttrSrc, BuiltEntities, EntityBuilder, EntityData, EntityIdSrc, TokenPrincipalMappings,
};
use crate::entity_builder::BuildUnsignedEntityError;
use cedar_policy::Entity;
use std::collections::HashSet;

impl EntityBuilder {
    pub(crate) fn build_principal_unsigned(
        &self,
        principal: &EntityData,
        built_entities: &BuiltEntities,
    ) -> Result<Entity, BuildUnsignedEntityError> {
        let type_name: &str = &principal.cedar_mapping.entity_type;
        let id_srcs = vec![EntityIdSrc::String(&principal.cedar_mapping.id)];
        let attrs_srcs = vec![AttrSrc::Unsigned(&principal.attributes)];

        let principal = self
            .build_principal_entity(
                type_name,
                &id_srcs,
                attrs_srcs,
                &TokenPrincipalMappings::default(),
                built_entities,
                HashSet::new(),
            )
            .map_err(Box::new)?;

        Ok(principal)
    }
}
