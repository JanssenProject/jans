// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! AnyElement -> Cedar resource conversion.

use pgrx::datum::{AnyElement, Datum, DatumWithOid, JsonB};
use pgrx::prelude::*;
use serde_json::{json, Value};

use crate::error::CedarlingError;
use crate::resource::schema_map;

pub(crate) fn build_resource_json_from_row(row: AnyElement) -> Result<String, CedarlingError> {
    let (mut value, table_oid) = row_to_json_and_table_oid(row)?;

    let Some(obj) = value.as_object_mut() else {
        value = json!({ "value": value });
        return Ok(serde_json::to_string(&value).unwrap_or_else(|_| "{}".to_string()));
    };

    if obj.contains_key("cedar_entity_mapping") {
        return serde_json::to_string(obj).map_err(|e| CedarlingError::JsonParsing(e.to_string()));
    }

    let mapping = table_oid
        .and_then(|oid| schema_map::mapping_for_table_oid(oid).ok().flatten())
        .unwrap_or_else(|| schema_map::EntityMapping {
            entity_type: "Resource".to_string(),
            id_columns: Vec::new(),
        });
    let entity_id = schema_map::build_entity_id(obj, &mapping);
    obj.insert(
        "cedar_entity_mapping".to_string(),
        json!({
            "entity_type": mapping.entity_type,
            "id": entity_id,
        }),
    );
    serde_json::to_string(&value).map_err(|e| CedarlingError::JsonParsing(e.to_string()))
}

/// [`IntoDatum`] for [`AnyElement`] uses PostgreSQL's `anyelement` type OID, so SPI would bind
/// `$1` as the `anyelement` pseudotype and PostgreSQL errors when evaluating `to_jsonb($1)`. Pass
/// the datum with the concrete type OID from [`AnyElement::oid`] instead.
fn datum_with_oid_for_spi(row: &AnyElement) -> DatumWithOid<'_> {
    const _: () = assert!(std::mem::size_of::<Datum>() == std::mem::size_of::<pg_sys::Datum>());
    unsafe {
        DatumWithOid::new_from_datum(
            Some(std::mem::transmute_copy::<pg_sys::Datum, Datum<'_>>(&row.datum())),
            row.oid(),
        )
    }
}

fn row_to_json_and_table_oid(row: AnyElement) -> Result<(Value, Option<pg_sys::Oid>), CedarlingError> {
    let mut out_json: Option<Value> = None;
    let mut out_oid: Option<pg_sys::Oid> = None;
    Spi::connect(|client| {
        let rows = client.select(
            "SELECT to_jsonb($1) AS row_json, t.typrelid::oid AS rel_oid
               FROM pg_type t
              WHERE t.oid = pg_typeof($1)::oid",
            None,
            &[datum_with_oid_for_spi(&row)],
        )?;
        for r in rows {
            let row_json = r
                .get_by_name::<JsonB, _>("row_json")?
                .map(|j| j.0)
                .unwrap_or_else(|| json!({}));
            let rel_oid = r
                .get_by_name::<pg_sys::Oid, _>("rel_oid")?
                .filter(|v| *v != pg_sys::InvalidOid);
            out_json = Some(row_json);
            out_oid = rel_oid;
            break;
        }
        Ok::<(), pgrx::spi::Error>(())
    })
    .map_err(|e| CedarlingError::Database(e.to_string()))?;

    let json_value = out_json.ok_or_else(|| {
        CedarlingError::ResourceConstruction("unable to materialize row as JSONB".to_string())
    })?;
    Ok((json_value, out_oid))
}
