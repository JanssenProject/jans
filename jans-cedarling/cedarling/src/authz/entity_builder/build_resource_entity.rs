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

        let attrs = build_entity_attrs(vec![(&resource.payload).into()]);
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
    use super::super::*;
    use super::*;
    use cedar_policy::EvalResult;
    use serde_json::json;

    #[test]
    fn can_build_entity() {
        let builder = EntityBuilder::new(EntityNames::default(), false, false, &HashMap::new());
        let resource_data = ResourceData {
            resource_type: "Jans::HttpRequest".to_string(),
            id: "request-123".to_string(),
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

        let url = entity
            .attr("url")
            .expect("entity must have an `url` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = url {
            assert_eq!(record.len(), 3);
            assert_eq!(
                record
                    .get("host")
                    .expect("expected `url` to have a `host` attribute"),
                &EvalResult::String("protected.host".to_string())
            );
            assert_eq!(
                record
                    .get("protocol")
                    .expect("expected `url` to have a `domain` attribute"),
                &EvalResult::String("http".to_string())
            );
            assert_eq!(
                record
                    .get("path")
                    .expect("expected `url` to have a `path` attribute"),
                &EvalResult::String("/protected".to_string())
            );
        } else {
            panic!(
                "expected the attribute `url` to be a record, got: {:?}",
                url
            );
        }

        let header = entity
            .attr("header")
            .expect("entity must have an `header` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = header {
            assert_eq!(record.len(), 1);
            assert_eq!(
                record
                    .get("Accept")
                    .expect("expected `url` to have an `Accept` attribute"),
                &EvalResult::String("test".to_string())
            );
        } else {
            panic!(
                "expected the attribute `header` to be a record, got: {:?}",
                header
            );
        }
    }

    #[test]
    fn can_build_entity_with_optional_attr() {
        let builder = EntityBuilder::new(EntityNames::default(), false, false, &HashMap::new());
        let resource_data = ResourceData {
            resource_type: "Jans::HttpRequest".to_string(),
            id: "request-123".to_string(),
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

    #[test]
    fn can_build_entity_with_optional_record_attr() {
        let builder = EntityBuilder::new(EntityNames::default(), false, false, &HashMap::new());
        let resource_data = ResourceData {
            resource_type: "Jans::HttpRequest".to_string(),
            id: "request-123".to_string(),
            payload: HashMap::from([
                (
                    "url".to_string(),
                    json!({"host": "protected.host", "protocol": "http", "path": "/protected"}),
                ),
                ("header".to_string(), json!({})),
            ]),
        };
        let entity = builder
            .build_resource_entity(&resource_data)
            .expect("expected to build resource entity");

        let url = entity
            .attr("url")
            .expect("entity must have an `url` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = url {
            assert_eq!(record.len(), 3);
            assert_eq!(
                record
                    .get("host")
                    .expect("expected `url` to have a `host` attribute"),
                &EvalResult::String("protected.host".to_string())
            );
            assert_eq!(
                record
                    .get("protocol")
                    .expect("expected `url` to have a `domain` attribute"),
                &EvalResult::String("http".to_string())
            );
            assert_eq!(
                record
                    .get("path")
                    .expect("expected `url` to have a `path` attribute"),
                &EvalResult::String("/protected".to_string())
            );
        } else {
            panic!(
                "expected the attribute `url` to be a record, got: {:?}",
                url
            );
        }

        let header = entity
            .attr("header")
            .expect("entity must have an `header` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = header {
            assert_eq!(record.len(), 0, "the header attribute must be empty");
        } else {
            panic!(
                "expected the attribute `header` to be a record, got: {:?}",
                header
            );
        }
    }
}
