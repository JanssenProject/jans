// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{
    BuildEntityError, BuildEntityErrorKind, BuiltEntities, Entity, EntityBuilder, HashSet,
    build_cedar_entity, build_entity_attrs,
};
use crate::EntityData;

impl EntityBuilder {
    pub(super) fn build_resource_entity(
        &self,
        resource_data: &EntityData,
    ) -> Result<Entity, BuildEntityError> {
        let resource_type_name = &resource_data.cedar_mapping.entity_type;

        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|s| s.get_entity_shape(resource_type_name));
        let attrs = build_entity_attrs(
            &resource_data.attributes,
            &BuiltEntities::default(),
            attrs_shape,
            None,
        )
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(resource_type_name))?;

        let mut resource = build_cedar_entity(
            resource_type_name,
            &resource_data.cedar_mapping.id,
            attrs,
            HashSet::new(),
        )?;

        if let Some(resource_default_entity) = self.default_entities.get(&resource.uid())
            && resource_data.attributes.is_empty()
        {
            resource = resource_default_entity.clone();
        }

        Ok(resource)
    }
}

#[cfg(test)]
mod test {
    use super::super::test::*;
    use super::super::*;
    use crate::CedarEntityMapping;
    use serde_json::json;

    #[test]
    fn can_build_entity() {
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default(),
            TrustedIssuerIndex::new(&HashMap::new(), None),
            Some(&CEDARLING_VALIDATOR_SCHEMA),
            DefaultEntities::default(),
        )
        .expect("should init entity builder");
        let resource_data = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::HTTP_Request".to_string(),
                id: "some_request".to_string(),
            },
            attributes: HashMap::from([
                ("header".to_string(), json!({"Accept": "test"})),
                (
                    "url".to_string(),
                    json!({"host": "protected.host", "protocol": "http", "path": "/protected"}),
                ),
            ]),
        };
        let entity = builder
            .build_resource_entity(&resource_data)
            .expect("expected to build resource entity");

        assert_entity_eq(
            &entity,
            &json!({
                "uid": {"type": "Jans::HTTP_Request", "id": "some_request"},
                "attrs": {
                    "url": {
                        "host": "protected.host",
                        "protocol": "http",
                        "path": "/protected",
                    },
                    "header": {
                        "Accept": "test",
                    }
                },
                "parents": [],
            }),
            Some(&CEDARLING_API_SCHEMA),
        );
    }

    #[test]
    fn can_build_entity_with_optional_attr() {
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default(),
            TrustedIssuerIndex::new(&HashMap::new(), None),
            Some(&CEDARLING_VALIDATOR_SCHEMA),
            DefaultEntities::default(),
        )
        .expect("should init entity builder");
        let resource_data = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::HTTP_Request".to_string(),
                id: "some_request".to_string(),
            },
            attributes: HashMap::new(),
        };
        let entity = builder
            .build_resource_entity(&resource_data)
            .expect("expected to build resource entity");

        assert!(
            entity.attr("url").is_none(),
            "entity should not have a `url` attribute"
        );
    }
}
