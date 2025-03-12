// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::{Entity, EntityTypeName};
use smol_str::{SmolStr, ToSmolStr};
use std::collections::HashMap;

/// Holds the entity ids of built entities
#[derive(Default)]
pub struct BuiltEntities(HashMap<SmolStr, SmolStr>);

impl BuiltEntities {
    pub fn insert(&mut self, entity: &Entity) {
        let type_name = entity.uid().type_name().to_smolstr();
        let id = entity.uid().id().escaped();
        self.0.insert(type_name, id);
    }

    pub fn get(&self, entity_type_name: &EntityTypeName) -> Option<&SmolStr> {
        self.0.get(&entity_type_name.to_smolstr())
    }

    pub fn from_iter<'a>(iter: impl Iterator<Item = &'a Entity>) -> Self {
        let mut built_entities = BuiltEntities::default();
        for entity in iter {
            built_entities.insert(entity);
        }
        built_entities
    }
}
