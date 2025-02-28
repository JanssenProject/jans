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

        let uid = EntityUid::from_str(&format!("{}::\"{}\"", resource_type_name, resource.id))
            .expect("TODO: return error");

        let attrs = build_entity_attrs((&resource.payload).into());
        let resource = Entity::new(uid, attrs, HashSet::new())
            .map_err(|e| BuildEntityErrorKind::from(e).while_building(resource_type_name))?;

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
        let builder = EntityBuilder::new(EntityNames::default(), false, false, &HashMap::new());
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
        let builder = EntityBuilder::new(EntityNames::default(), false, false, &HashMap::new());
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
