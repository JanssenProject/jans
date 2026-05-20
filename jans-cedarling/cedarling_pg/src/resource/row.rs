// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `AnyElement` -> Cedar resource conversion.

use pgrx::datum::{AnyElement, Datum, DatumWithOid, JsonB};
use pgrx::prelude::*;
use serde_json::{json, Value};
use thiserror::Error;

use crate::resource::schema_map;

/// Module-level error for row-to-JSON and resource-build operations.
#[derive(Debug, Error)]
pub(crate) enum RowBuildError {
    #[error("SPI error: {0}")]
    Spi(#[from] pgrx::spi::Error),
    #[error("JSON serialization error: {0}")]
    Json(#[from] serde_json::Error),
    #[error("row materialization failed: {0}")]
    Materialization(String),
}

pub(crate) fn build_resource_json_from_row(row: AnyElement) -> Result<String, RowBuildError> {
    let (mut value, table_oid) = row_to_json_and_table_oid(row)?;

    let Some(obj) = value.as_object_mut() else {
        value = json!({ "value": value });
        return Ok(serde_json::to_string(&value).unwrap_or_else(|_| "{}".to_string()));
    };

    if obj.contains_key("cedar_entity_mapping") {
        return Ok(serde_json::to_string(obj)?);
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
    Ok(serde_json::to_string(&value)?)
}

/// [`IntoDatum`] for [`AnyElement`] uses `PostgreSQL`'s `anyelement` type OID, so SPI would bind
/// `$1` as the `anyelement` pseudotype and `PostgreSQL` errors when evaluating `to_jsonb($1)`. Pass
/// the datum with the concrete type OID from [`AnyElement::oid`] instead.
/// # Safety
///
/// The returned [`DatumWithOid`] must not outlive `row`'s underlying heap tuple slot.
/// Callers must consume it inside the same `Spi::connect` (or equivalent) scope that owns
/// `row`.
unsafe fn datum_with_oid_for_spi(row: &AnyElement) -> DatumWithOid<'_> {
    const _: () = assert!(std::mem::size_of::<Datum>() == std::mem::size_of::<pg_sys::Datum>());
    DatumWithOid::new_from_datum(
        Some(std::mem::transmute_copy::<pg_sys::Datum, Datum<'_>>(&row.datum())),
        row.oid(),
    )
}

fn row_to_json_and_table_oid(
    row: AnyElement,
) -> Result<(Value, Option<pg_sys::Oid>), RowBuildError> {
    let mut out_json: Option<Value> = None;
    let mut out_oid: Option<pg_sys::Oid> = None;
    Spi::connect(|client| {
        let mut rows = client.select(
            "SELECT to_jsonb($1) AS row_json, t.typrelid::oid AS rel_oid
               FROM pg_type t
              WHERE t.oid = pg_typeof($1)::oid",
            None,
            &[unsafe { datum_with_oid_for_spi(&row) }],
        )?;
        if let Some(r) = rows.next() {
            let row_json = r
                .get_by_name::<JsonB, _>("row_json")?
                .map_or_else(|| json!({}), |j| j.0);
            let rel_oid = r
                .get_by_name::<pg_sys::Oid, _>("rel_oid")?
                .filter(|v| *v != pg_sys::InvalidOid);
            out_json = Some(row_json);
            out_oid = rel_oid;
        }
        Ok::<(), pgrx::spi::Error>(())
    })?;

    let json_value = out_json
        .ok_or_else(|| RowBuildError::Materialization("unable to materialize row as JSONB".into()))?;
    Ok((json_value, out_oid))
}
