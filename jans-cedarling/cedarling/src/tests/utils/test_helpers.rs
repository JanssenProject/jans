// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::{EntityData, authz::request::RequestUnsigned};
use serde::Deserialize;
use serde_json::json;

/// Creates a test principal entity with the given type, id, and additional attributes
pub(crate) fn create_test_principal(
    entity_type: &str,
    id: &str,
    attributes: serde_json::Value,
) -> Result<EntityData, serde_json::Error> {
    let mut entity_json = json!({
        "cedar_entity_mapping": {
            "entity_type": entity_type,
            "id": id
        }
    });

    if let serde_json::Value::Object(mut attrs) = entity_json {
        if let serde_json::Value::Object(additional_attrs) = attributes {
            attrs.extend(additional_attrs);
        }
        entity_json = serde_json::Value::Object(attrs);
    }

    EntityData::deserialize(entity_json)
}

/// Creates a basic test request with the given action, principals, and resource
pub(crate) fn create_test_unsigned_request(
    action: &str,
    principals: Vec<EntityData>,
    resource: EntityData,
) -> RequestUnsigned {
    RequestUnsigned {
        action: action.to_string(),
        context: json!({}),
        principals,
        resource,
    }
}
