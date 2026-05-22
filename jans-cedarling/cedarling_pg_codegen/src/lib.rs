// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Pure-function helpers for `cedarling-pg-codegen`: type mapping, table-to-entity
//! name inference, and `.cedarschema` rendering. Kept separate from `main.rs` so the
//! logic can be unit-tested without a live Postgres.

use std::fmt::Write as _;

/// A column read from `pg_attribute` via `format_type(...)`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Column {
    pub name: String,
    /// Result of `pg_catalog.format_type(atttypid, atttypmod)` — e.g. `integer`,
    /// `text`, `character varying(255)`, `text[]`, `numeric(10,2)`.
    pub pg_type: String,
}

/// Map a `format_type` string to a Cedar primitive or `Set<...>`.
///
/// Returns `None` for types that have no safe Cedar mapping (the caller should
/// either skip the column or emit a warning).
#[must_use]
pub fn cedar_type_for_pg_type(pg_type: &str) -> Option<String> {
    let pg = pg_type.trim();

    // Array form: "text[]" / "integer[]" / "uuid[][]" etc.  Cedar only models
    // one-dimensional sets, so we treat any array as `Set<inner>` and pass the
    // element type through this same mapping.
    if let Some(stripped) = pg.strip_suffix("[]") {
        let inner = cedar_type_for_pg_type(stripped)?;
        return Some(format!("Set<{inner}>"));
    }

    // Strip precision/length modifiers: `character varying(255)` → `character varying`.
    let base = pg.split_once('(').map_or(pg, |(b, _)| b).trim();

    // Cedar text schema has three primitives we can map onto: `Long`, `String`, `Bool`.
    // Numeric/float and bytea have no safe text-schema mapping; return None so the caller
    // can skip the column or warn. Temporal + jsonb fold into String as ISO-8601 / serialised text.
    let mapped = match base {
        "smallint" | "integer" | "bigint" | "oid" => "Long",
        "text"
        | "character varying"
        | "character"
        | "name"
        | "uuid"
        | "timestamp without time zone"
        | "timestamp with time zone"
        | "date"
        | "time without time zone"
        | "time with time zone"
        | "json"
        | "jsonb" => "String",
        "boolean" => "Bool",
        _ => return None,
    };
    Some(mapped.to_string())
}

/// Convert a `snake_case` table name to a singular `PascalCase` Cedar entity name.
///
/// Mirrors the heuristic used by `cedarling_pg`'s entity-map inference so that
/// generated schemas line up with the default lookup. Example: `student_records`
/// → `StudentRecord`.
#[must_use]
pub fn entity_name_for_table(table: &str) -> String {
    let parts: Vec<&str> = table.split('_').filter(|s| !s.is_empty()).collect();
    let mut out = String::new();
    for (i, part) in parts.iter().enumerate() {
        let depluralised = if i + 1 == parts.len() {
            depluralise(part)
        } else {
            (*part).to_string()
        };
        let mut chars = depluralised.chars();
        if let Some(first) = chars.next() {
            out.extend(first.to_uppercase());
            out.extend(chars.flat_map(char::to_lowercase));
        }
    }
    if out.is_empty() {
        "Entity".to_string()
    } else {
        out
    }
}

fn depluralise(word: &str) -> String {
    if let Some(stem) = word.strip_suffix("ies") {
        // companies → company
        return format!("{stem}y");
    }
    // Sibilant plurals (drop `es`, keep the stem): addresses → address, boxes → box.
    if word.ends_with("ses")
        || word.ends_with("xes")
        || word.ends_with("zes")
        || word.ends_with("shes")
        || word.ends_with("ches")
    {
        return word[..word.len() - 2].to_string();
    }
    // Generic `s` plural (avoid double-s like `class`).
    if word.ends_with('s') && !word.ends_with("ss") && word.len() > 1 {
        return word[..word.len() - 1].to_string();
    }
    word.to_string()
}

/// Outcome of rendering one table.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EntityRender {
    pub entity_name: String,
    pub cedar_text: String,
    pub unmapped_columns: Vec<String>,
}

/// Render a single entity declaration from a table's columns.
#[must_use]
pub fn render_entity(table: &str, columns: &[Column]) -> EntityRender {
    let entity_name = entity_name_for_table(table);
    let mut attrs: Vec<(String, String)> = Vec::new();
    let mut unmapped: Vec<String> = Vec::new();
    for col in columns {
        match cedar_type_for_pg_type(&col.pg_type) {
            Some(cedar_ty) => attrs.push((col.name.clone(), cedar_ty)),
            None => unmapped.push(col.name.clone()),
        }
    }

let mut body = String::new();
    if attrs.is_empty() {
        writeln!(body, "  entity {entity_name};").ok();
    } else {
        writeln!(body, "  entity {entity_name} = {{").ok();
        for (name, ty) in &attrs {
            writeln!(body, "    \"{name}\": {ty},").ok();
        }
        body.push_str("  };\n");
    }

    EntityRender {
        entity_name,
        cedar_text: body,
        unmapped_columns: unmapped,
    }
}

/// Wrap one or more entity bodies in a Cedar namespace.
#[must_use]
pub fn wrap_namespace(namespace: &str, entity_bodies: &[String]) -> String {
let mut out = String::new();
    writeln!(out, "namespace {namespace} {{").ok();
    for body in entity_bodies {
        out.push_str(body);
    }
    out.push_str("}\n");
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn maps_basic_scalar_types() {
        assert_eq!(cedar_type_for_pg_type("integer").as_deref(), Some("Long"));
        assert_eq!(cedar_type_for_pg_type("bigint").as_deref(), Some("Long"));
        assert_eq!(cedar_type_for_pg_type("text").as_deref(), Some("String"));
        assert_eq!(
            cedar_type_for_pg_type("character varying").as_deref(),
            Some("String")
        );
        assert_eq!(cedar_type_for_pg_type("boolean").as_deref(), Some("Bool"));
        assert_eq!(cedar_type_for_pg_type("uuid").as_deref(), Some("String"));
    }

    #[test]
    fn strips_length_modifiers() {
        assert_eq!(
            cedar_type_for_pg_type("character varying(255)").as_deref(),
            Some("String")
        );
        assert_eq!(
            cedar_type_for_pg_type("character(10)").as_deref(),
            Some("String")
        );
    }

    #[test]
    fn temporal_types_become_string() {
        assert_eq!(
            cedar_type_for_pg_type("timestamp with time zone").as_deref(),
            Some("String")
        );
        assert_eq!(cedar_type_for_pg_type("date").as_deref(), Some("String"));
    }

    #[test]
    fn arrays_become_sets() {
        assert_eq!(
            cedar_type_for_pg_type("text[]").as_deref(),
            Some("Set<String>")
        );
        assert_eq!(
            cedar_type_for_pg_type("integer[]").as_deref(),
            Some("Set<Long>")
        );
    }

    #[test]
    fn numeric_and_bytea_are_unmapped() {
        assert!(cedar_type_for_pg_type("numeric").is_none());
        assert!(cedar_type_for_pg_type("real").is_none());
        assert!(cedar_type_for_pg_type("bytea").is_none());
    }

    #[test]
    fn entity_name_pascal_cases_and_depluralises_last_word() {
        assert_eq!(entity_name_for_table("students"), "Student");
        assert_eq!(entity_name_for_table("user_accounts"), "UserAccount");
        assert_eq!(entity_name_for_table("companies"), "Company");
        assert_eq!(entity_name_for_table("addresses"), "Address");
        assert_eq!(entity_name_for_table("boxes"), "Box");
        assert_eq!(entity_name_for_table("classes"), "Class");
        assert_eq!(entity_name_for_table("status"), "Statu"); // false plural; user can override
    }

    #[test]
    fn render_entity_emits_attributes_with_quoted_names() {
        let cols = vec![
            Column {
                name: "id".into(),
                pg_type: "integer".into(),
            },
            Column {
                name: "name".into(),
                pg_type: "text".into(),
            },
            Column {
                name: "tags".into(),
                pg_type: "text[]".into(),
            },
        ];
        let r = render_entity("students", &cols);
        assert_eq!(r.entity_name, "Student");
        assert!(r.cedar_text.contains("entity Student = {"));
        assert!(r.cedar_text.contains("\"id\": Long,"));
        assert!(r.cedar_text.contains("\"name\": String,"));
        assert!(r.cedar_text.contains("\"tags\": Set<String>,"));
        assert!(r.unmapped_columns.is_empty());
    }

    #[test]
    fn render_entity_reports_unmapped_columns() {
        let cols = vec![
            Column {
                name: "id".into(),
                pg_type: "integer".into(),
            },
            Column {
                name: "score".into(),
                pg_type: "numeric(10,2)".into(),
            },
            Column {
                name: "image".into(),
                pg_type: "bytea".into(),
            },
        ];
        let r = render_entity("students", &cols);
        assert!(r.cedar_text.contains("\"id\": Long,"));
        assert!(!r.cedar_text.contains("score"));
        assert!(!r.cedar_text.contains("image"));
        assert_eq!(r.unmapped_columns, vec!["score", "image"]);
    }

    #[test]
    fn empty_table_renders_bare_entity() {
        let r = render_entity("empty", &[]);
        assert_eq!(r.cedar_text, "  entity Empty;\n");
    }

    #[test]
    fn wrap_namespace_combines_entity_bodies() {
        let body_a = "  entity A;\n".to_string();
        let body_b = "  entity B = {\n    \"x\": Long,\n  };\n".to_string();
        let out = wrap_namespace("Jans", &[body_a, body_b]);
        assert!(out.starts_with("namespace Jans {\n"));
        assert!(out.ends_with("}\n"));
        assert!(out.contains("entity A;"));
        assert!(out.contains("entity B = {"));
    }
}
