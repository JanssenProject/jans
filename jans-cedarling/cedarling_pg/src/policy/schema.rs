// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Schema compatibility checks for a `PostgreSQL` table vs Cedar schema text.

use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::io::Cursor;

use cedarling::bindings::cedar_policy;
use pgrx::prelude::*;
use serde_json::{json, Map, Value};
use thiserror::Error;

use crate::guc_config;
use crate::resource::schema_map;

#[derive(Debug, Clone)]
struct TableColumn {
    name: String,
    pg_type: String,
    typoid: pg_sys::Oid,
    typelem: Option<pg_sys::Oid>,
}

/// Module-level error for schema validation operations.
#[derive(Debug, Error)]
pub(crate) enum SchemaError {
    #[error("SPI error: {0}")]
    Spi(#[from] pgrx::spi::Error),
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
    #[error("validation failed: {0}")]
    Validation(String),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum PgKind {
    Long,
    String,
    Bool,
    NumericFloat,
    Temporal,
    Unknown,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum Compatibility {
    Match,
    Mismatch,
    Unknown,
}

/// Validate a `PostgreSQL` table shape against a Cedar schema file (text name variant).
///
/// By default (`cedarling.schema_validate_strict = true`) this uses Cedar's parser and performs
/// typed compatibility checks. When strict mode is disabled, it falls back to the historical
/// lexical identifier extraction behavior for compatibility.
#[pg_extern(stable, parallel_safe)]
pub fn cedarling_validate_schema(table_name: &str, cedar_schema_path: &str) -> pgrx::datum::JsonB {
    let table = table_name.trim();
    let schema_path = cedar_schema_path.trim();
    if table.is_empty() || schema_path.is_empty() {
        return error_envelope("table_name and cedar_schema_path are required");
    }

    if !guc_config::schema_validate_strict() {
        return validate_schema_lexical(table, schema_path);
    }

    let table_oid = match resolve_table_oid(table) {
        Ok(Some(oid)) => oid,
        Ok(None) => return error_envelope(&format!("unknown table: {table}")),
        Err(e) => return error_envelope(&e.to_string()),
    };
    match validate_schema_strict_inner(table_oid, schema_path) {
        Ok(v) => pgrx::datum::JsonB(v),
        Err(e) => error_envelope(&e.to_string()),
    }
}

/// Validate a `PostgreSQL` table shape against a Cedar schema file (regclass / OID variant).
///
/// Accepts the table as an OID so callers can write
/// `cedarling_validate_schema('mytable'::regclass::oid, '/path/schema')`.
/// Strict/lexical mode is controlled by `cedarling.schema_validate_strict` as in the text variant.
#[pg_extern(name = "cedarling_validate_schema", stable, parallel_safe)]
pub fn cedarling_validate_schema_by_oid(
    table_oid: pg_sys::Oid,
    cedar_schema_path: &str,
) -> pgrx::datum::JsonB {
    let schema_path = cedar_schema_path.trim();
    if table_oid == pg_sys::InvalidOid || schema_path.is_empty() {
        return error_envelope("table_oid and cedar_schema_path are required");
    }

    if !guc_config::schema_validate_strict() {
        let relname = match fetch_relname(table_oid) {
            Ok(n) => n,
            Err(e) => return error_envelope(&e.to_string()),
        };
        return validate_schema_lexical(&relname, schema_path);
    }

    match validate_schema_strict_inner(table_oid, schema_path) {
        Ok(v) => pgrx::datum::JsonB(v),
        Err(e) => error_envelope(&e.to_string()),
    }
}

fn validate_schema_strict_inner(table_oid: pg_sys::Oid, cedar_schema_path: &str) -> Result<Value, SchemaError> {
    let columns = fetch_table_columns_by_oid(table_oid)?;
    let relname = fetch_relname(table_oid)?;

    let mapping = schema_map::mapping_for_table_oid(table_oid)
        .map_err(|e| SchemaError::Validation(e.to_string()))?
        .unwrap_or(schema_map::EntityMapping {
            entity_type: schema_map::heuristic_entity_type(&relname),
            id_columns: Vec::new(),
        });

    let schema_text = fs::read_to_string(cedar_schema_path)?;
    let (schema, _warnings) = cedar_policy::Schema::from_cedarschema_file(Cursor::new(
        schema_text.as_bytes(),
    ))
    .map_err(|e| SchemaError::Validation(format!("schema parse failed: {e}")))?;

    let entity_type = resolve_entity_type_name(&schema, &mapping.entity_type)?;
    let (resolved_json, _warnings) = cedar_policy::schema_str_to_json_with_resolved_types(&schema_text)
        .map_err(|e| SchemaError::Validation(format!("schema type resolution failed: {e}")))?;
    let schema_attrs = extract_entity_attributes(&resolved_json, &entity_type).ok_or_else(|| {
        let root_keys: Vec<String> = resolved_json
            .as_object()
            .map(|o| o.keys().cloned().collect())
            .unwrap_or_default();
        SchemaError::Validation(format!(
            "entity type '{entity_type}' not found in resolved schema JSON (root keys: {root_keys:?})"
        ))
    })?;

    let table_cols_by_name: BTreeMap<String, TableColumn> = columns
        .iter()
        .cloned()
        .map(|c| (c.name.clone(), c))
        .collect();
    let table_col_names: BTreeSet<String> = table_cols_by_name.keys().cloned().collect();
    let schema_attr_names: BTreeSet<String> = schema_attrs.keys().cloned().collect();

    let missing_in_table: Vec<String> = schema_attr_names
        .difference(&table_col_names)
        .cloned()
        .collect();
    let missing_in_schema: Vec<String> = table_col_names
        .difference(&schema_attr_names)
        .cloned()
        .collect();

    let mut type_mismatches = Vec::new();
    for attr in schema_attr_names.intersection(&table_col_names) {
        let Some(pg_col) = table_cols_by_name.get(attr) else {
            continue;
        };
        let Some(cedar_ty) = schema_attrs.get(attr).and_then(cedar_type_from_attr) else {
            continue;
        };
        match compatibility_for_column(pg_col, cedar_ty.as_str()) {
            Compatibility::Mismatch => {
                type_mismatches.push(json!({
                    "column": attr,
                    "pg_type": pg_col.pg_type,
                    "cedar_type": cedar_ty
                }));
            },
            Compatibility::Unknown => {
                pgrx::warning!(
                    "cedarling_validate_schema: column '{}' has unrecognized Postgres type '{}' — skipping compatibility check",
                    attr,
                    pg_col.pg_type,
                );
            },
            Compatibility::Match => {},
        }
    }

    Ok(json!({
        "ok": missing_in_table.is_empty() && missing_in_schema.is_empty() && type_mismatches.is_empty(),
        "table_entity": entity_type,
        "missing_in_table": missing_in_table,
        "missing_in_schema": missing_in_schema,
        "type_mismatches": type_mismatches
    }))
}

fn validate_schema_lexical(table_name: &str, cedar_schema_path: &str) -> pgrx::datum::JsonB {
    let table_cols = match fetch_table_columns_by_name_legacy(table_name) {
        Ok(v) => v,
        Err(e) => return error_envelope(&format!("table introspection failed: {e}")),
    };
    let schema_txt = match fs::read_to_string(cedar_schema_path) {
        Ok(s) => s,
        Err(e) => return error_envelope(&format!("cannot read schema file: {e}")),
    };
    let schema_attrs = extract_schema_identifiers(&schema_txt);
    let missing_in_table: Vec<String> = schema_attrs.difference(&table_cols).cloned().collect();
    let missing_in_schema: Vec<String> = table_cols.difference(&schema_attrs).cloned().collect();
    pgrx::datum::JsonB(json!({
        "ok": missing_in_table.is_empty() && missing_in_schema.is_empty(),
        "table_entity": table_name,
        "missing_in_table": missing_in_table,
        "missing_in_schema": missing_in_schema,
        "type_mismatches": []
    }))
}

fn resolve_table_oid(table_name: &str) -> Result<Option<pg_sys::Oid>, SchemaError> {
    let oid = Spi::get_one_with_args::<pg_sys::Oid>(
        "SELECT to_regclass($1)::oid",
        &[table_name.into()],
    )?;
    Ok(oid.filter(|v| *v != pg_sys::InvalidOid))
}

fn fetch_relname(table_oid: pg_sys::Oid) -> Result<String, SchemaError> {
    let relname = Spi::get_one_with_args::<String>(
        "SELECT relname::text FROM pg_class WHERE oid = $1::oid",
        &[table_oid.into()],
    )?;
    relname.ok_or_else(|| SchemaError::Validation(format!("relation not found for oid {table_oid}")))
}

fn fetch_table_columns_by_oid(table_oid: pg_sys::Oid) -> Result<Vec<TableColumn>, SchemaError> {
    let mut out = Vec::new();
    Spi::connect(|client| {
        let rows = client.select(
            "SELECT a.attname::text AS attname,
                    a.atttypid::oid AS atttypid,
                    format_type(a.atttypid, a.atttypmod)::text AS pg_type,
                    t.typelem::oid AS typelem
             FROM pg_attribute a
             JOIN pg_type t ON t.oid = a.atttypid
             WHERE a.attrelid = $1::oid
               AND a.attnum > 0
               AND NOT a.attisdropped
             ORDER BY a.attnum",
            None,
            &[table_oid.into()],
        )?;
        for row in rows {
            let Some(name) = row.get_by_name::<String, _>("attname")? else {
                continue;
            };
            let Some(typoid) = row.get_by_name::<pg_sys::Oid, _>("atttypid")? else {
                continue;
            };
            let Some(pg_type) = row.get_by_name::<String, _>("pg_type")? else {
                continue;
            };
            let typelem = row
                .get_by_name::<pg_sys::Oid, _>("typelem")?
                .filter(|v| *v != pg_sys::InvalidOid);
            out.push(TableColumn {
                name,
                pg_type,
                typoid,
                typelem,
            });
        }
        Ok::<(), pgrx::spi::Error>(())
    })?;
    Ok(out)
}

fn fetch_table_columns_by_name_legacy(table_name: &str) -> Result<BTreeSet<String>, SchemaError> {
    let mut out = BTreeSet::new();
    Spi::connect(|client| {
        let rows = client.select(
            "SELECT a.attname::text AS attname
             FROM pg_attribute a
             JOIN pg_class c ON c.oid = a.attrelid
             JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE c.relname = $1
               AND a.attnum > 0
               AND NOT a.attisdropped",
            None,
            &[table_name.into()],
        )?;
        for row in rows {
            if let Some(name) = row.get_by_name::<String, _>("attname")? {
                out.insert(name);
            }
        }
        Ok::<(), pgrx::spi::Error>(())
    })?;
    Ok(out)
}

fn resolve_entity_type_name(
    schema: &cedar_policy::Schema,
    preferred_entity_type: &str,
) -> Result<String, SchemaError> {
    let schema_types: Vec<String> = schema.entity_types().map(ToString::to_string).collect();
    if schema_types.iter().any(|t| t == preferred_entity_type) {
        return Ok(preferred_entity_type.to_string());
    }

    let preferred_basename = preferred_entity_type
        .rsplit("::")
        .next()
        .unwrap_or(preferred_entity_type);
    let candidates: Vec<String> = schema_types
        .into_iter()
        .filter(|ty| ty.rsplit("::").next().is_some_and(|b| b == preferred_basename))
        .collect();

    match candidates.len() {
        1 => Ok(candidates[0].clone()),
        0 => Err(SchemaError::Validation(format!(
            "entity type '{preferred_entity_type}' not found in schema"
        ))),
        _ => Err(SchemaError::Validation(format!(
            "ambiguous entity type '{preferred_basename}' in schema; expected fully-qualified name"
        ))),
    }
}

fn extract_entity_attributes(
    schema_json: &Value,
    entity_type: &str,
) -> Option<BTreeMap<String, Value>> {
    let mut out = BTreeMap::new();
    let preferred_basename = entity_type.rsplit("::").next().unwrap_or(entity_type);
    let namespaces = if let Some(ns) = schema_json.get("namespaces").and_then(Value::as_object) {
        ns.clone()
    } else {
        schema_json.as_object()?.clone()
    };
    for (namespace_name, namespace_value) in namespaces {
        let Some(entity_types) = namespace_value.get("entityTypes").and_then(Value::as_object) else {
            continue;
        };
        for (short_name, entity_def) in entity_types {
            let full_name = if namespace_name.is_empty() {
                short_name.clone()
            } else {
                format!("{namespace_name}::{short_name}")
            };
            let full_basename = full_name.rsplit("::").next().unwrap_or(&full_name);
            if full_name != entity_type
                && short_name != entity_type
                && short_name != preferred_basename
                && full_basename != preferred_basename
            {
                continue;
            }

            let attrs = entity_def
                .get("shape")
                .and_then(|s| s.get("attributes"))
                .and_then(Value::as_object)
                .cloned()
                .unwrap_or_else(Map::new);
            for (k, v) in attrs {
                out.insert(k, v);
            }
            return Some(out);
        }
    }
    None
}

fn cedar_type_from_attr(attr: &Value) -> Option<String> {
    type_expr_to_string(attr)
}

fn type_expr_to_string(ty: &Value) -> Option<String> {
    let t = ty.get("type")?.as_str()?;
    match t {
        "EntityOrCommon" | "Entity" | "Common" => ty.get("name")?.as_str().map(ToString::to_string),
        "Set" => {
            let inner = ty.get("element").and_then(type_expr_to_string)?;
            Some(format!("Set<{inner}>"))
        },
        "Record" => Some("Record".to_string()),
        other => Some(other.to_string()),
    }
}

fn compatibility_for_column(col: &TableColumn, cedar_type: &str) -> Compatibility {
    if let Some(inner) = cedar_type
        .strip_prefix("Set<")
        .and_then(|s| s.strip_suffix('>'))
    {
        let Some(elem_oid) = col.typelem else {
            return Compatibility::Mismatch;
        };
        let pseudo = TableColumn {
            name: col.name.clone(),
            pg_type: col.pg_type.clone(),
            typoid: elem_oid,
            typelem: None,
        };
        return compatibility_for_column(&pseudo, inner);
    }

    let kind = pg_kind_for_oid(col.typoid);
    match kind {
        PgKind::Long => {
            if cedar_type == "Long" {
                Compatibility::Match
            } else {
                Compatibility::Mismatch
            }
        },
        PgKind::String | PgKind::Temporal => {
            if cedar_type == "String" {
                Compatibility::Match
            } else {
                Compatibility::Mismatch
            }
        },
        PgKind::Bool => {
            if cedar_type == "Bool" {
                Compatibility::Match
            } else {
                Compatibility::Mismatch
            }
        },
        PgKind::NumericFloat => {
            if cedar_type == "Long" || cedar_type == "String" || cedar_type == "Bool" {
                Compatibility::Mismatch
            } else {
                Compatibility::Unknown
            }
        },
        PgKind::Unknown => Compatibility::Unknown,
    }
}

fn pg_kind_for_oid(oid: pg_sys::Oid) -> PgKind {
    if matches!(
        oid,
        pg_sys::INT2OID | pg_sys::INT4OID | pg_sys::INT8OID | pg_sys::OIDOID
    ) {
        return PgKind::Long;
    }
    if matches!(
        oid,
        pg_sys::TEXTOID | pg_sys::VARCHAROID | pg_sys::BPCHAROID | pg_sys::NAMEOID | pg_sys::UUIDOID
    ) {
        return PgKind::String;
    }
    if oid == pg_sys::BOOLOID {
        return PgKind::Bool;
    }
    if matches!(oid, pg_sys::NUMERICOID | pg_sys::FLOAT4OID | pg_sys::FLOAT8OID) {
        return PgKind::NumericFloat;
    }
    if matches!(
        oid,
        pg_sys::TIMESTAMPOID
            | pg_sys::TIMESTAMPTZOID
            | pg_sys::DATEOID
            | pg_sys::TIMEOID
            | pg_sys::TIMETZOID
    ) {
        return PgKind::Temporal;
    }
    PgKind::Unknown
}

fn error_envelope(error: &str) -> pgrx::datum::JsonB {
    pgrx::datum::JsonB(json!({
        "ok": false,
        "error": error,
        "missing_in_table": [],
        "missing_in_schema": [],
        "type_mismatches": []
    }))
}

fn extract_schema_identifiers(schema_text: &str) -> BTreeSet<String> {
    let mut out = BTreeSet::new();
    for token in schema_text
        .split(|c: char| !(c.is_ascii_alphanumeric() || c == '_'))
        .filter(|t| !t.is_empty())
    {
        if matches!(
            token,
            "entity" | "type" | "namespace" | "String" | "Long" | "Bool" | "Set" | "Record"
        ) {
            continue;
        }
        if token.chars().next().is_some_and(|c| c.is_ascii_lowercase()) {
            out.insert(token.to_string());
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn extract_schema_identifiers_filters_keywords() {
        let s = r"entity User in Namespace { name: String, age: Long, is_ok: Bool }";
        let attrs = extract_schema_identifiers(s);
        assert!(attrs.contains("name"));
        assert!(attrs.contains("age"));
        assert!(attrs.contains("is_ok"));
        assert!(!attrs.contains("String"));
    }
}
