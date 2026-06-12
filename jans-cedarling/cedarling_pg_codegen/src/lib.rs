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

    if let Some(stripped) = pg.strip_suffix("[]") {
        let inner = cedar_type_for_pg_type(stripped)?;
        return Some(format!("Set<{inner}>"));
    }

    let base = pg.split_once('(').map_or(pg, |(b, _)| b).trim();

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
        // Avoid truncating singular words like "status" -> "statu" (stem ends in 'u').
        if !stem.ends_with('u') {
            return stem.to_string();
        }
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

/// Escape a `PostgreSQL` attribute name for Cedar schema double-quoted keys.
fn escape_cedar_attribute_name(name: &str) -> String {
    name.replace('\\', "\\\\").replace('"', "\\\"")
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
            let escaped = escape_cedar_attribute_name(name);
            writeln!(body, "    \"{escaped}\": {ty},").ok();
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

/// Sanitize a table identifier for use as a single filesystem path component.
///
/// `pg_class.relname` stores quoted-identifier spellings without the surrounding `"`,
/// so names like `../../../evil` are possible. This strips path separators, control
/// characters, and `..` segments before building output filenames.
#[must_use]
pub fn sanitize_table_filename(table: &str) -> String {
    let mut name = table.trim().to_string();
    if name.len() >= 2 && name.starts_with('"') && name.ends_with('"') {
        name = name[1..name.len() - 1].replace("\"\"", "\"");
    }

    let mut sanitized: String = name
        .chars()
        .map(|c| {
            if c == '/' || c == '\\' || c == '\0' || c.is_control() {
                '_'
            } else {
                c
            }
        })
        .collect();

    while sanitized.contains("..") {
        sanitized = sanitized.replace("..", "_");
    }

    sanitized = sanitized.trim_matches(['.', ' ']).to_string();
    if sanitized.is_empty() {
        "_table".to_string()
    } else {
        sanitized
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn maps_basic_scalar_types() {
        assert_eq!(
            cedar_type_for_pg_type("integer").as_deref(),
            Some("Long"),
            "integer should map to Long"
        );
        assert_eq!(
            cedar_type_for_pg_type("bigint").as_deref(),
            Some("Long"),
            "bigint should map to Long"
        );
        assert_eq!(
            cedar_type_for_pg_type("text").as_deref(),
            Some("String"),
            "text should map to String"
        );
        assert_eq!(
            cedar_type_for_pg_type("character varying").as_deref(),
            Some("String"),
            "character varying should map to String"
        );
        assert_eq!(
            cedar_type_for_pg_type("boolean").as_deref(),
            Some("Bool"),
            "boolean should map to Bool"
        );
        assert_eq!(
            cedar_type_for_pg_type("uuid").as_deref(),
            Some("String"),
            "uuid should map to String"
        );
    }

    #[test]
    fn strips_length_modifiers() {
        assert_eq!(
            cedar_type_for_pg_type("character varying(255)").as_deref(),
            Some("String"),
            "varchar length modifier should be stripped"
        );
        assert_eq!(
            cedar_type_for_pg_type("character(10)").as_deref(),
            Some("String"),
            "char length modifier should be stripped"
        );
    }

    #[test]
    fn temporal_types_become_string() {
        assert_eq!(
            cedar_type_for_pg_type("timestamp with time zone").as_deref(),
            Some("String"),
            "timestamptz should map to String"
        );
        assert_eq!(
            cedar_type_for_pg_type("date").as_deref(),
            Some("String"),
            "date should map to String"
        );
    }

    #[test]
    fn arrays_become_sets() {
        assert_eq!(
            cedar_type_for_pg_type("text[]").as_deref(),
            Some("Set<String>"),
            "text[] should map to Set<String>"
        );
        assert_eq!(
            cedar_type_for_pg_type("integer[]").as_deref(),
            Some("Set<Long>"),
            "integer[] should map to Set<Long>"
        );
    }

    #[test]
    fn numeric_and_bytea_are_unmapped() {
        assert!(
            cedar_type_for_pg_type("numeric").is_none(),
            "numeric has no Cedar mapping"
        );
        assert!(
            cedar_type_for_pg_type("real").is_none(),
            "real has no Cedar mapping"
        );
        assert!(
            cedar_type_for_pg_type("bytea").is_none(),
            "bytea has no Cedar mapping"
        );
    }

    #[test]
    fn entity_name_pascal_cases_and_depluralises_last_word() {
        assert_eq!(entity_name_for_table("students"), "Student", "students → Student");
        assert_eq!(
            entity_name_for_table("user_accounts"),
            "UserAccount",
            "user_accounts → UserAccount"
        );
        assert_eq!(entity_name_for_table("companies"), "Company", "companies → Company");
        assert_eq!(entity_name_for_table("addresses"), "Address", "addresses → Address");
        assert_eq!(entity_name_for_table("boxes"), "Box", "boxes → Box");
        assert_eq!(entity_name_for_table("classes"), "Class", "classes → Class");
        assert_eq!(
            entity_name_for_table("status"),
            "Status",
            "status is already singular and should not lose its trailing s"
        );
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
        assert_eq!(r.entity_name, "Student", "entity name should depluralize table");
        assert!(
            r.cedar_text.contains("entity Student = {"),
            "output should declare Student entity"
        );
        assert!(r.cedar_text.contains("\"id\": Long,"), "id should map to Long");
        assert!(r.cedar_text.contains("\"name\": String,"), "name should map to String");
        assert!(
            r.cedar_text.contains("\"tags\": Set<String>,"),
            "tags should map to Set<String>"
        );
        assert!(r.unmapped_columns.is_empty(), "all columns should map");
    }

    #[test]
    fn render_entity_escapes_quotes_in_attribute_names() {
        let cols = vec![Column {
            name: r#"col"quote"#.into(),
            pg_type: "text".into(),
        }];
        let r = render_entity("items", &cols);
        assert!(
            r.cedar_text.contains(r#""col\"quote": String,"#),
            "embedded double quotes must be escaped"
        );
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
        assert!(r.cedar_text.contains("\"id\": Long,"), "mapped id should appear");
        assert!(!r.cedar_text.contains("score"), "unmapped score should be omitted");
        assert!(!r.cedar_text.contains("image"), "unmapped image should be omitted");
        assert_eq!(
            r.unmapped_columns,
            vec!["score", "image"],
            "unmapped columns should be reported"
        );
    }

    #[test]
    fn empty_table_renders_bare_entity() {
        let r = render_entity("empty", &[]);
        assert_eq!(
            r.cedar_text,
            "  entity Empty;\n",
            "empty table should render bare entity"
        );
    }

    #[test]
    fn wrap_namespace_combines_entity_bodies() {
        let body_a = "  entity A;\n".to_string();
        let body_b = "  entity B = {\n    \"x\": Long,\n  };\n".to_string();
        let out = wrap_namespace("Jans", &[body_a, body_b]);
        assert!(out.starts_with("namespace Jans {\n"), "should open namespace");
        assert!(out.ends_with("}\n"), "should close namespace");
        assert!(out.contains("entity A;"), "should include first entity");
        assert!(out.contains("entity B = {"), "should include second entity");
    }

    #[test]
    fn sanitize_table_filename_blocks_path_traversal() {
        assert_eq!(
            sanitize_table_filename("../../../table"),
            "______table",
            "slashes and parent-dir segments must be neutralized into a single filename component"
        );
        assert_eq!(
            sanitize_table_filename("../../etc/passwd"),
            "____etc_passwd",
            "slashes and parent-dir segments must be neutralized into a single filename component"
        );
    }

    #[test]
    fn sanitize_table_filename_preserves_ordinary_names() {
        assert_eq!(
            sanitize_table_filename("students"),
            "students",
            "simple table name should pass through unchanged"
        );
        assert_eq!(
            sanitize_table_filename("user_accounts"),
            "user_accounts",
            "snake_case table name should pass through unchanged"
        );
        assert_eq!(
            sanitize_table_filename(r#""my_table""#),
            "my_table",
            "quoted identifier spelling should strip surrounding quotes"
        );
    }

    #[test]
    fn sanitize_table_filename_preserves_leading_underscores() {
        assert_eq!(
            sanitize_table_filename("_users"),
            "_users",
            "leading underscores must not be trimmed"
        );
        assert_eq!(
            sanitize_table_filename("users"),
            "users",
            "unprefixed name must stay distinct from _users"
        );
    }
}
