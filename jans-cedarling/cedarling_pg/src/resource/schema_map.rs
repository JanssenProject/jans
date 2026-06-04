// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Table -> Cedar entity mapping helpers for row-based authorization.

use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};

use pgrx::prelude::*;
use serde_json::{Map, Value};

use crate::functions::error::CedarlingError;

#[derive(Debug, Clone)]
pub(crate) struct EntityMapping {
    pub(crate) entity_type: String,
    pub(crate) id_columns: Vec<String>,
}

pub(crate) fn mapping_for_table_oid(table_oid: pg_sys::Oid) -> Result<Option<EntityMapping>, CedarlingError> {
    if table_oid == pg_sys::InvalidOid {
        return Ok(None);
    }

    if let Some(mapped) = lookup_explicit_mapping(table_oid)? {
        return Ok(Some(mapped));
    }

    let relname = lookup_relname(table_oid)?;
    let id_columns = lookup_primary_key_columns(table_oid)?;
    Ok(Some(EntityMapping {
        entity_type: heuristic_entity_type(&relname),
        id_columns,
    }))
}

fn lookup_explicit_mapping(table_oid: pg_sys::Oid) -> Result<Option<EntityMapping>, CedarlingError> {
    let mut found: Option<EntityMapping> = None;
    Spi::connect(|client| {
        let mut rows = client.select(
            "SELECT entity_type, id_columns
             FROM cedarling.entity_map
             WHERE table_oid = $1::oid",
            None,
            &[table_oid.into()],
        )?;
        if let Some(row) = rows.next() {
            let entity_type = row
                .get_by_name::<String, _>("entity_type")?
                .unwrap_or_default()
                .trim()
                .to_string();
            let id_columns_raw = row.get_by_name::<Vec<String>, _>("id_columns")?;
            if !entity_type.is_empty() {
                found = Some(EntityMapping {
                    entity_type,
                    id_columns: sanitize_id_columns(id_columns_raw.unwrap_or_default()),
                });
            }
        }
        Ok::<(), pgrx::spi::Error>(())
    })
    .map_err(|e| CedarlingError::Database(e.to_string()))?;
    Ok(found)
}

fn lookup_relname(table_oid: pg_sys::Oid) -> Result<String, CedarlingError> {
    let mut relname: Option<String> = None;
    Spi::connect(|client| {
        let mut rows = client.select(
            "SELECT relname::text AS relname FROM pg_class WHERE oid = $1::oid",
            None,
            &[table_oid.into()],
        )?;
        if let Some(row) = rows.next() {
            relname = row.get_by_name::<String, _>("relname")?;
        }
        Ok::<(), pgrx::spi::Error>(())
    })
    .map_err(|e| CedarlingError::Database(e.to_string()))?;
    relname.ok_or_else(|| CedarlingError::Database(format!("relation not found for oid {table_oid}")))
}

fn lookup_primary_key_columns(table_oid: pg_sys::Oid) -> Result<Vec<String>, CedarlingError> {
    let mut cols = Vec::new();
    Spi::connect(|client| {
        // Expand indkey in index order; `attnum = ANY(i.indkey)` is unreliable for SPI /
        // indkey typing and can yield no rows, which forces a hashed fallback entity id.
        let rows = client.select(
            "SELECT a.attname::text AS attname
             FROM pg_index i
             JOIN LATERAL unnest(i.indkey) WITH ORDINALITY AS u(attnum, ord) ON true
             JOIN pg_attribute a
               ON a.attrelid = i.indrelid
              AND a.attnum = u.attnum
             WHERE i.indrelid = $1::oid
               AND i.indisprimary
               AND NOT a.attisdropped
               AND a.attnum > 0
             ORDER BY u.ord",
            None,
            &[table_oid.into()],
        )?;
        for row in rows {
            if let Some(attname) = row.get_by_name::<String, _>("attname")? {
                cols.push(attname);
            }
        }
        Ok::<(), pgrx::spi::Error>(())
    })
    .map_err(|e| CedarlingError::Database(e.to_string()))?;
    Ok(cols)
}

pub(crate) fn build_entity_id(attrs: &Map<String, Value>, mapping: &EntityMapping) -> String {
    if !mapping.id_columns.is_empty() {
        let mut parts = Vec::with_capacity(mapping.id_columns.len());
        for col in &mapping.id_columns {
            let Some(v) = attrs.get(col) else {
                return fallback_hashed_entity_id(attrs);
            };
            let s = match v {
                Value::String(s) => s.clone(),
                Value::Null => "null".to_string(),
                _ => v.to_string(),
            };
            parts.push(s);
        }
        if parts.len() == 1 {
            return parts.into_iter().next().expect("checked len above");
        }
        return serde_json::to_string(&parts).unwrap_or_else(|_| fallback_hashed_entity_id(attrs));
    }
    fallback_hashed_entity_id(attrs)
}

fn sanitize_id_columns(id_columns: Vec<String>) -> Vec<String> {
    id_columns
        .into_iter()
        .map(|col| col.trim().to_string())
        .filter(|col| !col.is_empty())
        .collect()
}

fn fallback_hashed_entity_id(attrs: &Map<String, Value>) -> String {
    let mut h = DefaultHasher::new();
    serde_json::to_string(attrs)
        .unwrap_or_else(|_| "{}".to_string())
        .hash(&mut h);
    format!("row-{:016x}", h.finish())
}

fn depluralise_word(word: &str) -> String {
    if let Some(stem) = word.strip_suffix("ies") {
        return format!("{stem}y");
    }
    if word.ends_with("ses")
        || word.ends_with("xes")
        || word.ends_with("zes")
        || word.ends_with("shes")
        || word.ends_with("ches")
    {
        return word[..word.len() - 2].to_string();
    }
    if word.ends_with('s') && !word.ends_with("ss") && word.len() > 1 {
        let stem = &word[..word.len() - 1];
        if !stem.ends_with('u') {
            return stem.to_string();
        }
    }
    word.to_string()
}

pub(crate) fn heuristic_entity_type(relname: &str) -> String {
    let parts: Vec<&str> = relname.split('_').filter(|s| !s.is_empty()).collect();
    let mut out = String::new();
    for (i, part) in parts.iter().enumerate() {
        let word = if i + 1 == parts.len() {
            depluralise_word(part)
        } else {
            (*part).to_string()
        };
        let mut chars = word.chars();
        if let Some(first) = chars.next() {
            out.push(first.to_ascii_uppercase());
            out.extend(chars);
        }
    }
    out
}

/// Register or update an explicit table -> entity mapping.
#[pg_extern(volatile, parallel_unsafe)]
pub fn cedarling_register_entity_map(
    table: pg_sys::Oid,
    entity_type: &str,
    id_columns: Vec<String>,
) -> bool {
    let entity_type = entity_type.trim();
    if table == pg_sys::InvalidOid || entity_type.is_empty() {
        return false;
    }
    let id_columns = sanitize_id_columns(id_columns);
    let ok = Spi::connect_mut(|client| {
        client.update(
            "INSERT INTO cedarling.entity_map(table_oid, entity_type, id_columns)
             VALUES ($1::oid, $2, $3)
             ON CONFLICT (table_oid)
             DO UPDATE SET entity_type = EXCLUDED.entity_type,
                           id_columns  = EXCLUDED.id_columns",
            None,
            &[
                table.into(),
                entity_type.into(),
                id_columns.into(),
            ],
        )?;
        Ok::<(), pgrx::spi::Error>(())
    })
    .is_ok();
    // entity_map controls which Cedar entity a row is evaluated as; cached
    // decisions taken under the old mapping must not survive the change.
    if ok {
        crate::authz::cache::global_cache().clear_all();
    }
    ok
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn heuristic_singular_pascal_case() {
        assert_eq!(heuristic_entity_type("students"), "Student", "trailing s should depluralize");
        assert_eq!(
            heuristic_entity_type("student_profiles"),
            "StudentProfile",
            "snake_case segments should become PascalCase"
        );
        assert_eq!(heuristic_entity_type("user"), "User", "single segment should capitalize");
        assert_eq!(
            heuristic_entity_type("status"),
            "Status",
            "singular words ending in s should not be truncated"
        );
    }

    #[test]
    fn build_entity_id_uses_json_array_for_composite_keys() {
        use serde_json::json;

        let attrs = json!({"id1": "a-b", "id2": "c"}).as_object().unwrap().clone();
        let mapping = EntityMapping {
            entity_type: "T".into(),
            id_columns: vec!["id1".into(), "id2".into()],
        };
        assert_eq!(
            build_entity_id(&attrs, &mapping),
            r#"["a-b","c"]"#,
            "composite keys must encode as JSON array to avoid join collisions"
        );
    }

    #[test]
    fn build_entity_id_single_column_stays_plain() {
        use serde_json::json;

        let attrs = json!({"id": 42}).as_object().unwrap().clone();
        let mapping = EntityMapping {
            entity_type: "T".into(),
            id_columns: vec!["id".into()],
        };
        assert_eq!(
            build_entity_id(&attrs, &mapping),
            "42",
            "single-column ids should remain a plain string"
        );
    }
}
