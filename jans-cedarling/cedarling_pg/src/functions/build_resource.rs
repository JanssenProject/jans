// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Build Cedarling `EntityData` JSON strings from JSONB documents or composite row types.

use pgrx::datum::AnyElement;
use pgrx::prelude::*;
use serde_json::json;

use crate::resource;

/// Build a Cedarling `EntityData` JSON string from a JSONB document.
///
/// When both `entity_type` and `entity_id` are provided they are injected as
/// `cedar_entity_mapping` (overwriting any existing mapping in the JSONB). When either is `NULL`
/// the caller must embed `cedar_entity_mapping` in the JSONB itself.
///
/// Raises a `PostgreSQL` `ERROR` if the resulting document is not a valid `EntityData`.
///
/// **Warning:** this helper aborts the current SQL statement on invalid input. Do not call it
/// from RLS `USING` / `WITH CHECK` bodies — a single malformed row will fail the whole query.
#[pg_extern(stable, parallel_safe)]
pub fn cedarling_build_resource(
    resource: pgrx::datum::JsonB,
    entity_type: Option<&str>,
    entity_id: Option<&str>,
) -> String {
    let mut value = resource.0;
    if let (Some(et), Some(eid)) = (entity_type, entity_id) {
        if let Some(obj) = value.as_object_mut() {
            obj.insert(
                "cedar_entity_mapping".to_string(),
                json!({ "entity_type": et, "id": eid }),
            );
        }
    }
    let json_str = match serde_json::to_string(&value) {
        Ok(s) => s,
        Err(e) => pgrx::error!("cedarling_build_resource: serialization error: {e}"),
    };
    if let Err(e) = resource::resource_entity_data_from_json_str(&json_str) {
        pgrx::error!("cedarling_build_resource: invalid EntityData: {e}");
    }
    json_str
}

/// Build a Cedarling `EntityData` JSON string from a composite row (`anyelement`).
///
/// SQL name is `cedarling_build_resource_row` so it does not overload `cedarling_build_resource`
/// (`jsonb`, …): `PostgreSQL` cannot reliably resolve two `cedarling_build_resource` targets when
/// one uses polymorphic `anyelement`.
///
/// Same abort-on-error semantics as [`cedarling_build_resource`] — not safe inside RLS policies.
#[pg_extern(name = "cedarling_build_resource_row", stable, parallel_safe)]
pub fn cedarling_build_resource_anyelement(record: AnyElement) -> String {
    match crate::resource::row::build_resource_json_from_row(record) {
        Ok(json_str) => {
            if let Err(e) = resource::resource_entity_data_from_json_str(&json_str) {
                pgrx::error!("cedarling_build_resource_row: invalid EntityData: {e}");
            }
            json_str
        },
        Err(e) => pgrx::error!("cedarling_build_resource_row: {e}"),

    }
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;

    #[test]
    fn build_resource_logic_injects_cedar_mapping() {
        let mut value = json!({ "owner": "alice", "level": 3 });
        if let Some(obj) = value.as_object_mut() {
            obj.insert(
                "cedar_entity_mapping".to_string(),
                json!({ "entity_type": "Jans::Document", "id": "doc-42" }),
            );
        }
        let json_str = serde_json::to_string(&value).expect("serialize");
        let entity = resource::resource_entity_data_from_json_str(&json_str)
            .expect("should parse with injected mapping");
        assert_eq!(entity.cedar_mapping.entity_type, "Jans::Document");
        assert_eq!(entity.cedar_mapping.id, "doc-42");
        assert_eq!(entity.attributes.get("owner"), Some(&json!("alice")));
    }
}
