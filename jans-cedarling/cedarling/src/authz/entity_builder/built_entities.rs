// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::EntityTypeName;
use smol_str::{SmolStr, ToSmolStr};
use std::collections::HashMap;

/// Holds the entity ids of built entities
#[derive(Default)]
pub struct BuiltEntities(HashMap<SmolStr, SmolStr>);

impl BuiltEntities {
    pub fn get(&self, entity_type_name: &EntityTypeName) -> Option<&SmolStr> {
        self.0.get(&entity_type_name.to_smolstr())
    }
}

impl From<HashMap<SmolStr, SmolStr>> for BuiltEntities {
    fn from(value: HashMap<SmolStr, SmolStr>) -> Self {
        Self(value)
    }
}
