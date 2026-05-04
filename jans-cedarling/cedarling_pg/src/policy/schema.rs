// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Schema compatibility checks for a PostgreSQL table vs Cedar schema text.

use std::collections::BTreeSet;
use std::fs;

use pgrx::prelude::*;
use serde_json::json;

/// Validate table column names against identifiers found in a Cedar schema file.
///
/// This lightweight check reports obvious drift (`missing_in_table`, `missing_in_schema`) and
/// keeps type mismatches empty for now.
#[pg_extern]
pub fn cedarling_validate_schema(table_name: &str, cedar_schema_path: &str) -> pgrx::datum::JsonB {
    let table = table_name.trim();
    let schema_path = cedar_schema_path.trim();
    if table.is_empty() || schema_path.is_empty() {
        return pgrx::datum::JsonB(json!({
            "ok": false,
            "error": "table_name and cedar_schema_path are required",
            "missing_in_table": [],
            "missing_in_schema": [],
            "type_mismatches": []
        }));
    }

    let table_cols = match fetch_table_columns(table) {
        Ok(v) => v,
        Err(e) => {
            return pgrx::datum::JsonB(json!({
                "ok": false,
                "error": format!("table introspection failed: {e}"),
                "missing_in_table": [],
                "missing_in_schema": [],
                "type_mismatches": []
            }));
        },
    };

    let schema_txt = match fs::read_to_string(schema_path) {
        Ok(s) => s,
        Err(e) => {
            return pgrx::datum::JsonB(json!({
                "ok": false,
                "error": format!("cannot read schema file: {e}"),
                "missing_in_table": [],
                "missing_in_schema": [],
                "type_mismatches": []
            }));
        },
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

fn fetch_table_columns(table_name: &str) -> Result<BTreeSet<String>, pgrx::spi::Error> {
    let mut out = BTreeSet::new();
    Spi::connect(|client| {
        let rows = client.select(
            "SELECT a.attname
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

fn extract_schema_identifiers(schema_text: &str) -> BTreeSet<String> {
    let mut out = BTreeSet::new();
    for token in schema_text
        .split(|c: char| !(c.is_ascii_alphanumeric() || c == '_'))
        .filter(|t| !t.is_empty())
    {
        // Skip obvious Cedar keywords/types.
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
        let s = r#"entity User in Namespace { name: String, age: Long, is_ok: Bool }"#;
        let attrs = extract_schema_identifiers(s);
        assert!(attrs.contains("name"));
        assert!(attrs.contains("age"));
        assert!(attrs.contains("is_ok"));
        assert!(!attrs.contains("String"));
    }
}
