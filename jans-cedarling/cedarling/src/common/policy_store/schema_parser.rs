// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cedar schema parsing and validation.
//!
//! This module provides functionality to parse and validate Cedar schema files,
//! ensuring they are syntactically correct and semantically valid before being
//! used for policy validation and evaluation.

use cedar_policy::{Schema, SchemaFragment};
use std::str::FromStr;

use super::errors::{CedarSchemaErrorType, PolicyStoreError};

/// Merge multiple `SchemaFragment`s into one by serializing each to JSON,
/// deep-merging the JSON objects, and parsing the result back.
///
/// # Why JSON?
///
/// Each `SchemaFragment` stores two private fields (cedar-policy 4.11):
/// - `value: ValidatorSchemaFragment` — the validated form used for
///   `Schema::from_schema_fragments()`. No public getter, no merge method.
/// - `lossless: json_schema::Fragment<RawName>` — a `BTreeMap<Option<Name>,
///   NamespaceDefinition>` which is the JSON representation.
///
/// There is no public API to merge `SchemaFragment`s directly. The only way
/// to obtain a merged `SchemaFragment` is to round-trip through JSON:
///   `to_json_value()` → deep-merge → `from_json_value()`
///
/// Internally, cedar-policy-core's `ValidatorSchema::from_schema_fragments`
/// (see schema.rs:553) merges by collecting all namespace definitions from
/// all fragments, running RFC 70 checks, adding builtin aliases, etc. But it
/// returns a `ValidatorSchema`, not a `ValidatorSchemaFragment`, so the
/// `lossless` JSON data is lost. We use `Schema::from_schema_fragments`
/// separately for validation; this function is only about building a single
/// `SchemaFragment` for downstream JSON serialization.
///
/// # Why not construct a `SchemaFragment` from `Schema`?
///
/// `Schema` has no `to_cedarschema()` or `to_json_string()`. Only
/// `SchemaFragment` does (via its `lossless` field). Downstream code in
/// `manager.rs` calls `fragment.to_json_string()` to build `CedarSchemaJson`
/// and `ValidatorSchema`, so we must produce a `SchemaFragment`.
pub(super) fn combine_schema_fragments(
    fragments: &[SchemaFragment],
) -> Result<SchemaFragment, PolicyStoreError> {
    let mut combined = serde_json::Map::new();
    for fragment in fragments {
        let json =
            fragment
                .clone()
                .to_json_value()
                .map_err(|e| PolicyStoreError::CedarSchemaError {
                    file: "(combine)".to_string(),
                    err: CedarSchemaErrorType::ParseError(e.to_string()),
                })?;
        if let serde_json::Value::Object(map) = json {
            deep_merge(&mut combined, map);
        }
    }
    SchemaFragment::from_json_value(serde_json::Value::Object(combined)).map_err(|e| {
        PolicyStoreError::CedarSchemaError {
            file: "(combine)".to_string(),
            err: CedarSchemaErrorType::ValidationError(e.to_string()),
        }
    })
}

/// Recursively merge `source` into `target` (both are JSON objects).
/// Keys in `source` that already exist in `target` are deep-merged
/// (if both values are objects) or overwritten.
///
/// This implementation is sufficient for our use case (merging Cedar schema
/// namespace maps). Ideally this functionality would live upstream in
/// cedar-policy as `SchemaFragment::merge()`. Consider contributing a merge
/// API to cedar-policy so we can remove the JSON round-trip entirely.
fn deep_merge(
    target: &mut serde_json::Map<String, serde_json::Value>,
    source: serde_json::Map<String, serde_json::Value>,
) {
    for (key, value) in source {
        match (target.get_mut(&key), value) {
            (Some(serde_json::Value::Object(existing)), serde_json::Value::Object(new_map)) => {
                deep_merge(existing, new_map);
            },
            (_existing, new_value) => {
                target.insert(key, new_value);
            },
        }
    }
}

/// A parsed and validated Cedar schema.
///
/// Contains the schema, fragment, and metadata about the source file.
/// The fragment is preserved to avoid re-parsing when converting to JSON.
#[derive(Debug, Clone)]
pub(super) struct ParsedSchema {
    /// The Cedar schema
    pub schema: Schema,
    /// The schema fragment (preserved for JSON conversion without re-parsing)
    pub fragment: SchemaFragment,
    /// Source filename
    pub filename: String,
    /// Raw schema content
    pub content: String,
}

impl ParsedSchema {
    /// Parse a Cedar schema from a string.
    ///
    /// Parses the schema content using Cedar's schema parser and returns
    /// a `ParsedSchema` with metadata. The schema is validated for correct
    /// syntax and structure during parsing.
    pub(super) fn parse(content: &str, filename: &str) -> Result<Self, PolicyStoreError> {
        // Parse the schema using Cedar's schema parser
        // Cedar uses SchemaFragment to parse human-readable schema syntax
        let fragment =
            SchemaFragment::from_str(content).map_err(|e| PolicyStoreError::CedarSchemaError {
                file: filename.to_string(),
                err: CedarSchemaErrorType::ParseError(e.to_string()),
            })?;

        // Create schema from the fragment (clone the fragment to preserve it)
        let schema = Schema::from_schema_fragments([fragment.clone()]).map_err(|e| {
            PolicyStoreError::CedarSchemaError {
                file: filename.to_string(),
                err: CedarSchemaErrorType::ValidationError(e.to_string()),
            }
        })?;

        Ok(Self {
            schema,
            fragment,
            filename: filename.to_string(),
            content: content.to_string(),
        })
    }

    /// Get a reference to the Cedar Schema.
    ///
    /// Returns the validated Cedar Schema that can be used for policy validation.
    pub(super) fn get_schema(&self) -> &Schema {
        &self.schema
    }

    /// Get a reference to the schema fragment.
    ///
    /// Returns the parsed fragment, which can be used for JSON serialization
    /// without re-parsing the schema content.
    pub(super) fn get_fragment(&self) -> &SchemaFragment {
        &self.fragment
    }

    /// Parse multiple Cedar schema fragments and combine them into a single schema.
    ///
    /// Each file is parsed into its own `SchemaFragment`, then all fragments are
    /// merged via `Schema::from_schema_fragments`. This allows splitting a schema
    /// across multiple files (e.g., one file per namespace or entity type).
    ///
    /// The returned `ParsedSchema` contains the concatenated content of all files.
    ///
    /// ## Why JSON merge for the fragment?
    ///
    /// After validation we need a single `SchemaFragment` for downstream code
    /// (`manager.rs` calls `fragment.to_json_string()`). There is no public
    /// API to merge `SchemaFragment`s or to convert `Schema` → `SchemaFragment`
    /// (cedar-policy 4.11). The only workaround is to round-trip through JSON:
    /// `to_json_value()` → deep-merge namespace maps → `from_json_value()`.
    #[allow(dead_code)]
    pub(super) fn parse_multiple(files: &[(String, String)]) -> Result<Self, PolicyStoreError> {
        if files.is_empty() {
            return Err(PolicyStoreError::CedarSchemaError {
                file: "(no files)".to_string(),
                err: CedarSchemaErrorType::EmptySchema,
            });
        }

        let mut fragments = Vec::new();
        let mut combined_content = String::new();
        let mut filenames = Vec::new();

        for (filename, content) in files {
            let fragment = SchemaFragment::from_str(content).map_err(|e| {
                PolicyStoreError::CedarSchemaError {
                    file: filename.clone(),
                    err: CedarSchemaErrorType::ParseError(e.to_string()),
                }
            })?;

            if !combined_content.is_empty() {
                combined_content.push('\n');
            }
            combined_content.push_str(content);
            filenames.push(filename.clone());
            fragments.push(fragment);
        }

        // Combine via Schema::from_schema_fragments — this is additive and
        // correctly handles the same namespace appearing in multiple files.
        let schema = Schema::from_schema_fragments(fragments.clone()).map_err(|e| {
            PolicyStoreError::CedarSchemaError {
                file: filenames.join(", "),
                err: CedarSchemaErrorType::ValidationError(e.to_string()),
            }
        })?;

        // Merge fragments into a single SchemaFragment via JSON so that
        // get_fragment().to_json_string() works downstream.
        let combined_fragment = combine_schema_fragments(&fragments)?;

        Ok(Self {
            schema,
            fragment: combined_fragment,
            filename: filenames.join(", "),
            content: combined_content,
        })
    }

    /// Validate that the schema is non-empty and well-formed.
    ///
    /// Performs additional validation checks beyond basic parsing to ensure
    /// the schema is not empty. If parsing succeeded, the schema is already
    /// validated by Cedar for internal consistency.
    ///
    /// # Errors
    /// Returns `PolicyStoreError::CedarSchemaError` if the schema file is empty.
    pub(super) fn validate(&self) -> Result<(), PolicyStoreError> {
        // Check that content is not empty
        if self.content.trim().is_empty() {
            return Err(PolicyStoreError::CedarSchemaError {
                file: self.filename.clone(),
                err: CedarSchemaErrorType::EmptySchema,
            });
        }

        // If parsing succeeded, the schema is already validated by Cedar
        // The Schema type guarantees internal consistency
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_valid_schema() {
        let content = r#"
            namespace TestApp {
                entity User;
                entity File;
                action "view" appliesTo {
                    principal: [User],
                    resource: [File]
                };
            }
        "#;

        let result = ParsedSchema::parse(content, "test.cedarschema");
        assert!(result.is_ok());

        let parsed = result.unwrap();
        assert_eq!(parsed.filename, "test.cedarschema");
        assert_eq!(parsed.content, content);
    }

    #[test]
    fn test_parse_schema_with_multiple_namespaces() {
        let content = r"
            namespace App1 {
                entity User;
            }
            
            namespace App2 {
                entity Admin;
            }
        ";

        let result = ParsedSchema::parse(content, "multi.cedarschema");
        assert!(result.is_ok());
    }

    #[test]
    fn test_parse_schema_with_complex_types() {
        let content = r#"
            namespace MyApp {
                entity User = {
                    "name": String,
                    "age": Long,
                    "email": String
                };
                
                entity Document = {
                    "title": String,
                    "owner": User,
                    "tags": Set<String>
                };
                
                action "view" appliesTo {
                    principal: [User],
                    resource: [Document]
                };
                
                action "edit" appliesTo {
                    principal: [User],
                    resource: [Document]
                };
            }
        "#;

        let result = ParsedSchema::parse(content, "complex.cedarschema");
        assert!(result.is_ok());
    }

    #[test]
    fn test_parse_invalid_schema_syntax() {
        let content = "this is not valid cedar schema syntax!!!";

        let result = ParsedSchema::parse(content, "invalid.cedarschema");
        let err = result.expect_err("Expected CedarSchemaError for invalid syntax");

        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarSchemaError { file, err: CedarSchemaErrorType::ParseError(_) }
                if file == "invalid.cedarschema"
            ),
            "Expected CedarSchemaError with ParseError, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_empty_schema() {
        let content = "";

        let result = ParsedSchema::parse(content, "empty.cedarschema");
        // Empty schema is actually valid in Cedar, but our validation will catch it
        if let Ok(parsed) = result {
            let validation = parsed.validate();
            let err = validation.expect_err("Expected EmptySchema validation error");
            assert!(
                matches!(
                    &err,
                    PolicyStoreError::CedarSchemaError {
                        err: CedarSchemaErrorType::EmptySchema,
                        ..
                    }
                ),
                "Expected EmptySchema error, got: {err:?}"
            );
        }
    }

    #[test]
    fn test_parse_schema_missing_closing_brace() {
        let content = r"
            namespace MyApp {
                entity User;
                entity File;
        ";

        let result = ParsedSchema::parse(content, "malformed.cedarschema");
        let err = result.expect_err("Expected error for missing closing brace");
        assert!(
            matches!(&err, PolicyStoreError::CedarSchemaError { .. }),
            "Expected CedarSchemaError for malformed schema, got: {err:?}"
        );
    }

    #[test]
    fn test_validate_schema_success() {
        let content = r"
            namespace TestApp {
                entity User;
            }
        ";

        let parsed = ParsedSchema::parse(content, "test.cedarschema").unwrap();
        let result = parsed.validate();
        assert!(result.is_ok());
    }

    #[test]
    fn test_parse_schema_with_entity_hierarchy() {
        let content = r#"
            namespace OrgApp {
                entity User in [Group];
                entity Group in [Organization];
                entity Organization;
                
                action "view" appliesTo {
                    principal: [User, Group],
                    resource: [User, Group, Organization]
                };
            }
        "#;

        let result = ParsedSchema::parse(content, "hierarchy.cedarschema");
        assert!(result.is_ok());
    }

    #[test]
    fn test_parse_schema_with_action_groups() {
        let content = r#"
            namespace FileSystem {
                entity User;
                entity File;
                
                action "read" appliesTo {
                    principal: [User],
                    resource: [File]
                };
                
                action "write" appliesTo {
                    principal: [User],
                    resource: [File]
                };
                
                action "readWrite" in ["read", "write"];
            }
        "#;

        let result = ParsedSchema::parse(content, "action_groups.cedarschema");
        assert!(result.is_ok());
    }

    #[test]
    fn test_schema_error_message_includes_filename() {
        let content = "namespace { invalid }";

        let result = ParsedSchema::parse(content, "my_schema.cedarschema");
        let err = result.expect_err("Expected error for invalid namespace syntax");
        assert!(
            matches!(&err, PolicyStoreError::CedarSchemaError { file, .. } if file == "my_schema.cedarschema"),
            "Expected CedarSchemaError with filename my_schema.cedarschema, got: {err:?}"
        );
    }

    #[test]
    fn test_validate_empty_schema_fails() {
        let content = "   \n  \t  \n   ";

        let result = ParsedSchema::parse(content, "whitespace.cedarschema");
        // Empty content might parse successfully, but validation should fail
        if let Ok(parsed) = result {
            let validation = parsed.validate();
            assert!(
                validation.is_err(),
                "Validation should fail for whitespace-only schema"
            );

            let Err(PolicyStoreError::CedarSchemaError { file, err }) = validation else {
                panic!("Expected CedarSchemaError");
            };

            assert_eq!(file, "whitespace.cedarschema");
            assert!(matches!(err, CedarSchemaErrorType::EmptySchema));
        }
    }

    #[test]
    fn test_parse_schema_with_common_types() {
        let content = r#"
            namespace AppSchema {
                entity User = {
                    "id": String,
                    "email": String,
                    "roles": Set<String>,
                    "age": Long,
                    "active": Bool
                };
                
                entity Resource = {
                    "name": String,
                    "owner": User,
                    "tags": Set<String>
                };
            }
        "#;

        let result = ParsedSchema::parse(content, "types.cedarschema");
        assert!(result.is_ok(), "Schema with common types should parse");
    }

    #[test]
    fn test_parse_schema_with_context() {
        let content = r#"
            namespace ContextApp {
                entity User;
                entity File;
                
                action "view" appliesTo {
                    principal: [User],
                    resource: [File],
                    context: {
                        "ip_address": String,
                        "time": Long
                    }
                };
            }
        "#;

        let result = ParsedSchema::parse(content, "context.cedarschema");
        assert!(result.is_ok(), "Schema with action context should parse");
    }

    #[test]
    fn test_parse_schema_with_optional_attributes() {
        let content = r#"
            namespace OptionalApp {
                entity User = {
                    "name": String,
                    "email"?: String,
                    "phone"?: String
                };
            }
        "#;

        let result = ParsedSchema::parse(content, "optional.cedarschema");
        assert!(
            result.is_ok(),
            "Schema with optional attributes should parse"
        );
    }

    #[test]
    fn test_parse_schema_invalid_entity_definition() {
        let content = r#"
            namespace MyApp {
                entity User = {
                    "name": InvalidType
                };
            }
        "#;

        let result = ParsedSchema::parse(content, "invalid_type.cedarschema");
        let err = result.expect_err("Invalid entity type should fail parsing");
        assert!(
            matches!(&err, PolicyStoreError::CedarSchemaError { .. }),
            "Expected CedarSchemaError for invalid entity type, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_schema_missing_semicolon() {
        let content = r"
            namespace MyApp {
                entity User
                entity File;
            }
        ";

        let result = ParsedSchema::parse(content, "missing_semicolon.cedarschema");
        let err = result.expect_err("Missing semicolon should fail parsing");
        assert!(
            matches!(&err, PolicyStoreError::CedarSchemaError { .. }),
            "Expected CedarSchemaError for missing semicolon, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_schema_duplicate_entity() {
        let content = r"
            namespace MyApp {
                entity User;
                entity User;
            }
        ";

        let result = ParsedSchema::parse(content, "duplicate.cedarschema");
        // Cedar may or may not allow duplicate entity definitions
        // This test documents the current behavior - if an error occurs, it should be a schema error
        if let Err(err) = result {
            assert!(
                matches!(&err, PolicyStoreError::CedarSchemaError { .. }),
                "Expected CedarSchemaError for duplicate entity, got: {err:?}"
            );
        }
    }

    #[test]
    fn test_parsed_schema_clone() {
        let content = r"
            namespace TestApp {
                entity User;
            }
        ";

        let parsed = ParsedSchema::parse(content, "test.cedarschema").unwrap();
        let cloned = parsed.clone();

        assert_eq!(parsed.filename, cloned.filename);
        assert_eq!(parsed.content, cloned.content);
    }

    #[test]
    fn test_parse_schema_with_extension() {
        let content = r"
            namespace ExtApp {
                entity User;
                entity AdminUser in [User];
                entity SuperAdmin in [AdminUser];
            }
        ";

        let result = ParsedSchema::parse(content, "extension.cedarschema");
        assert!(
            result.is_ok(),
            "Schema with entity hierarchy should parse successfully"
        );
    }

    #[test]
    fn test_format_schema_error_not_empty() {
        // Create an intentionally malformed schema to trigger SchemaError
        let content = "namespace MyApp { entity User = { invalid } }";

        let result = ParsedSchema::parse(content, "test.cedarschema");
        let err = result.expect_err("Expected error for malformed schema");
        assert!(
            matches!(&err, PolicyStoreError::CedarSchemaError { file, .. } if file == "test.cedarschema"),
            "Expected CedarSchemaError with filename test.cedarschema, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_schema_preserves_content() {
        let content = r"namespace Test { entity User; }";

        let parsed = ParsedSchema::parse(content, "preserve.cedarschema").unwrap();
        assert_eq!(
            parsed.content, content,
            "Original content should be preserved"
        );
    }

    #[test]
    fn test_parse_multiple_schemas_independently() {
        let schema1 = r"
            namespace App1 {
                entity User;
            }
        ";
        let schema2 = r"
            namespace App2 {
                entity Admin;
            }
        ";

        let result1 = ParsedSchema::parse(schema1, "schema1.cedarschema");
        let result2 = ParsedSchema::parse(schema2, "schema2.cedarschema");

        assert!(result1.is_ok());
        assert!(result2.is_ok());

        let parsed1 = result1.unwrap();
        let parsed2 = result2.unwrap();

        assert_ne!(parsed1.filename, parsed2.filename);
        assert_ne!(parsed1.content, parsed2.content);
    }

    #[test]
    fn test_parse_multiple_valid() {
        let files = vec![
            (
                "users.cedarschema".to_string(),
                r"namespace App { entity User; }".to_string(),
            ),
            (
                "resources.cedarschema".to_string(),
                r"namespace App { entity Resource; }".to_string(),
            ),
        ];

        let result = ParsedSchema::parse_multiple(&files);
        let parsed = result.expect("parse_multiple should succeed");

        assert!(
            parsed.content.contains("entity User"),
            "expected merged schema content to contain 'entity User'; got: {}",
            parsed.content
        );
        assert!(
            parsed.content.contains("entity Resource"),
            "expected merged schema content to contain 'entity Resource'; got: {}",
            parsed.content
        );
        assert_eq!(
            parsed.filename,
            "users.cedarschema, resources.cedarschema",
            "expected parsed filename to be 'users.cedarschema, resources.cedarschema'; got: {}",
            parsed.filename
        );

        parsed.validate().expect("Combined schema should be valid");
    }

    #[test]
    fn test_parse_multiple_empty_list() {
        let result = ParsedSchema::parse_multiple(&[]);
        let err = result.expect_err("Expected error for empty files list");
        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarSchemaError {
                    err: CedarSchemaErrorType::EmptySchema,
                    ..
                }
            ),
            "Expected EmptySchema error, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_multiple_with_invalid_file() {
        let files = vec![
            (
                "valid.cedarschema".to_string(),
                r"namespace App { entity User; }".to_string(),
            ),
            (
                "invalid.cedarschema".to_string(),
                "this is not valid cedar schema".to_string(),
            ),
        ];

        let result = ParsedSchema::parse_multiple(&files);
        let err = result.expect_err("Expected error for invalid file");
        assert!(
            matches!(&err, PolicyStoreError::CedarSchemaError { file, err: CedarSchemaErrorType::ParseError(_) }
                if file == "invalid.cedarschema"),
            "Expected ParseError for invalid.cedarschema, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_multiple_different_namespaces() {
        let files = vec![
            (
                "users.cedarschema".to_string(),
                r"namespace Users { entity User; }".to_string(),
            ),
            (
                "docs.cedarschema".to_string(),
                r"namespace Docs { entity Document; entity Folder; }".to_string(),
            ),
        ];

        let result = ParsedSchema::parse_multiple(&files);
        let parsed = result.expect("parse_multiple with different namespaces should succeed");

        // Verify schema contains all entities
        let schema = parsed.get_schema();
        let type_names: Vec<_> = schema
            .entity_types()
            .map(std::string::ToString::to_string)
            .collect();
        assert!(
            type_names.contains(&"Users::User".to_string()),
            "Should contain Users::User"
        );
        assert!(
            type_names.contains(&"Docs::Document".to_string()),
            "Should contain Docs::Document"
        );
        assert!(
            type_names.contains(&"Docs::Folder".to_string()),
            "Should contain Docs::Folder"
        );

        // Verify JSON conversion works (via fragment)
        let fragment = parsed.get_fragment();
        let json = fragment.to_json_string().expect("Should serialize to JSON");
        assert!(
            json.contains("Users::User") || json.contains("Users"),
            "expected JSON to contain Users namespace; got: {}",
            json
        );
        assert!(
            json.contains("Docs::Document") || json.contains("Docs"),
            "expected JSON to contain Docs namespace; got: {}",
            json
        );
    }

    #[test]
    fn test_combine_fragments_json_single() {
        let content = r"namespace Test { entity User; }";
        let fragment = SchemaFragment::from_str(content).unwrap();
        let result = combine_schema_fragments(&[fragment]);
        let combined = result.expect("combine_fragments_json with single fragment should succeed");
        let json = combined.to_json_string().expect("Should serialize to JSON");
        assert!(
            json.contains("User"),
            "Combined fragment should contain User"
        );
    }

    #[test]
    fn test_combine_fragments_json_deep_merge_same_namespace() {
        let f1 = SchemaFragment::from_str(r"namespace App { entity User; }").unwrap();
        let f2 = SchemaFragment::from_str(r"namespace App { entity Admin; }").unwrap();
        let result = combine_schema_fragments(&[f1, f2]);
        let combined = result.expect("combine_fragments_json with same namespace should succeed");
        let json = combined.to_json_string().expect("Should serialize to JSON");
        assert!(json.contains("User"), "Should contain User");
        assert!(json.contains("Admin"), "Should contain Admin");
    }

    #[test]
    fn test_deep_merge_overlapping_keys() {
        use serde_json::{Map, Value, json};

        let mut target: Map<String, Value> = serde_json::from_value(json!({
            "App": {
                "entityTypes": {
                    "User": { "shape": { "type": "Record", "attributes": {} } }
                }
            }
        }))
        .unwrap();

        let source: Map<String, Value> = serde_json::from_value(json!({
            "App": {
                "entityTypes": {
                    "Admin": { "shape": { "type": "Record", "attributes": {} } }
                }
            }
        }))
        .unwrap();

        deep_merge(&mut target, source);

        let merged = Value::Object(target);
        let app = merged.get("App").unwrap();
        let etypes = app.get("entityTypes").unwrap();
        assert!(etypes.get("User").is_some(), "User should survive merge");
        assert!(
            etypes.get("Admin").is_some(),
            "Admin should be added by merge"
        );
    }

    #[test]
    fn test_parse_multiple_with_actions() {
        let files = vec![
            (
                "types.cedarschema".to_string(),
                r"
                namespace App {
                    entity User;
                    entity File;
                }
                "
                .to_string(),
            ),
            (
                "actions.cedarschema".to_string(),
                r#"
                namespace App {
                    action "read" appliesTo {
                        principal: [User],
                        resource: [File]
                    };
                }
                "#
                .to_string(),
            ),
        ];

        let result = ParsedSchema::parse_multiple(&files);
        let parsed = result.expect("parse_multiple with actions should succeed");

        parsed
            .validate()
            .expect("Schema with actions should be valid");

        let schema = parsed.get_schema();
        let actions: Vec<_> = schema.actions().collect();
        let action_names: Vec<_> = actions
            .iter()
            .map(std::string::ToString::to_string)
            .collect();
        assert!(
            action_names.contains(&"App::Action::\"read\"".to_string()),
            "Should contain read action, got: {action_names:?}"
        );
    }
}
