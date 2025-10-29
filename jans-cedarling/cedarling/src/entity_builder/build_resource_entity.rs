// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use crate::EntityData;

impl EntityBuilder {
    pub fn build_resource_entity(&self, resource: &EntityData) -> Result<Entity, BuildEntityError> {
        let resource_type_name = &resource.cedar_mapping.entity_type;

        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|s| s.get_entity_shape(resource_type_name));
        let attrs = build_entity_attrs(
            &resource.attributes,
            &BuiltEntities::default(),
            attrs_shape,
            None,
        )
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(resource_type_name))?;

        let resource = build_cedar_entity(
            resource_type_name,
            &resource.cedar_mapping.id,
            attrs,
            HashSet::new(),
        )?;

        Ok(resource)
    }
}

#[cfg(test)]
mod test {
    use super::super::test::*;
    use super::super::*;
    use super::*;
    use crate::CedarEntityMapping;
    use crate::log::TEST_LOGGER;
    use serde_json::json;

    #[test]
    fn can_build_entity() {
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default(),
            &HashMap::new(),
            Some(&CEDARLING_VALIDATOR_SCHEMA),
            None,
            None,
            TEST_LOGGER.clone(),
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
            json!({
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
            &HashMap::new(),
            Some(&CEDARLING_VALIDATOR_SCHEMA),
            None,
            None,
            TEST_LOGGER.clone(),
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
