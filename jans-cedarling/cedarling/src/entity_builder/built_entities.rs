// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::{Entity, EntityUid};
use smol_str::{SmolStr, ToSmolStr};
use std::collections::{HashMap, hash_map::Entry};
use url::Origin;

/// Holds the entity ids of built entities
#[derive(Default)]
pub struct OldBuiltEntities(HashMap<SmolStr, SmolStr>);

impl OldBuiltEntities {
    pub fn get(&self, entity_type_name: &cedar_policy::EntityTypeName) -> Option<&SmolStr> {
        self.0.get(&entity_type_name.to_smolstr())
    }
}

impl From<HashMap<SmolStr, SmolStr>> for OldBuiltEntities {
    fn from(value: HashMap<SmolStr, SmolStr>) -> Self {
        Self(value)
    }
}

#[derive(Default)]
pub struct BuiltEntities {
    singles: HashMap<SmolStr, SmolStr>,
    multiples: HashMap<SmolStr, Vec<SmolStr>>,
}

impl From<&HashMap<Origin, Entity>> for BuiltEntities {
    fn from(src: &HashMap<Origin, Entity>) -> Self {
        src.values().fold(Self::default(), |mut acc, e| {
            acc.insert(&e.uid());
            acc
        })
    }
}

impl BuiltEntities {
    pub fn insert(&mut self, uid: &EntityUid) {
        let name = uid.type_name().to_smolstr();
        let id = uid.id().escaped();
        match self.singles.entry(name.clone()) {
            Entry::Occupied(entry) => {
                let exsisting_eid = entry.remove();
                self.multiples
                    .entry(name.clone())
                    .or_default()
                    .extend([exsisting_eid, id]);
            },
            Entry::Vacant(entry) => {
                entry.insert(id);
            },
        }
    }

    pub fn get_single(&self, type_name: &str) -> Option<&str> {
        self.singles.get(type_name).map(|v| &**v)
    }

    pub fn get_multiple(&self, type_name: &str) -> Option<&[SmolStr]> {
        self.multiples.get(type_name).map(|v| &**v)
    }
}
