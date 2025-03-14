// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use crate::ResourceData;

impl EntityBuilder {
    pub fn build_resource_entity(
        &self,
        resource: &ResourceData,
    ) -> Result<Entity, BuildEntityError> {
        let resource_type_name = &resource.resource_type;

        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|s| s.get_entity_shape(resource_type_name));
        let attrs = build_entity_attrs(
            &resource.payload,
            &BuiltEntities::default(),
            attrs_shape,
            None,
        )
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(resource_type_name))?;

        let resource = build_cedar_entity(resource_type_name, &resource.id, attrs, HashSet::new())?;

        Ok(resource)
    }
}

#[derive(Debug, thiserror::Error, PartialEq)]
#[error("JSON value type mismatch: expected '{expected_type}', but found '{actual_type}'")]
pub struct JsonTypeError {
    pub expected_type: String,
    pub actual_type: String,
}

#[cfg(test)]
mod test {
    use super::super::test::*;
    use super::super::*;
    use super::*;
    use serde_json::json;

    #[test]
    fn can_build_entity() {
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default(),
            &HashMap::new(),
            Some(cedarling_validator_schema()),
        )
        .expect("should init entity builder");
        let resource_data = ResourceData {
            resource_type: "Jans::HTTP_Request".to_string(),
            id: "some_request".to_string(),
            payload: HashMap::from([
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
            Some(&cedarling_schema()),
        );
    }

    #[test]
    fn can_build_entity_with_optional_attr() {
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default(),
            &HashMap::new(),
            Some(cedarling_validator_schema()),
        )
        .expect("should init entity builder");
        let resource_data = ResourceData {
            resource_type: "Jans::HTTP_Request".to_string(),
            id: "some_request".to_string(),
            payload: HashMap::new(),
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
