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

/// A schema file with its name and content.
///
/// Used when loading multiple schema files that need to be combined
/// into a single schema (see [`ParsedSchema::parse_multiple`]).
#[derive(Debug, Clone)]
pub(super) struct SchemaFile {
    /// File name (e.g. `"users.cedarschema"`)
    pub name: String,
    /// Raw Cedar schema content
    pub content: String,
}

/// A parsed and validated Cedar schema.
#[derive(Debug, Clone)]
pub(crate) struct ParsedSchema {
    /// The Cedar schema
    pub schema: Schema,
}

impl ParsedSchema {
    /// Parse a Cedar schema from a Cedar DSL string.
    pub(super) fn parse(content: &str, filename: &str) -> Result<Self, PolicyStoreError> {
        let fragment =
            SchemaFragment::from_str(content).map_err(|e| PolicyStoreError::CedarSchemaError {
                file: filename.to_string(),
                err: CedarSchemaErrorType::ParseError(e.to_string()),
            })?;
        let schema = Schema::from_schema_fragments([fragment]).map_err(|e| {
            PolicyStoreError::CedarSchemaError {
                file: filename.to_string(),
                err: CedarSchemaErrorType::ValidationError(e.to_string()),
            }
        })?;

        Ok(Self { schema })
    }

    /// Get a reference to the Cedar Schema.
    ///
    /// Returns the validated Cedar Schema that can be used for policy validation.
    pub(super) fn get_schema(&self) -> &Schema {
        &self.schema
    }

    /// Parse multiple Cedar schema fragments and combine them into a single schema.
    ///
    /// Each file is parsed into its own `SchemaFragment`, then Cedar merges them
    /// with `Schema::from_schema_fragments`, which accepts a namespace split across
    /// files and references from one file to types declared in another.
    pub(super) fn parse_multiple(files: &[SchemaFile]) -> Result<Self, PolicyStoreError> {
        if files.is_empty() {
            return Err(PolicyStoreError::CedarSchemaError {
                file: "(no files)".to_string(),
                err: CedarSchemaErrorType::EmptySchema,
            });
        }

        let mut fragments = Vec::new();
        let mut filenames = Vec::new();

        for SchemaFile { name, content } in files {
            let fragment = SchemaFragment::from_str(content).map_err(|e| {
                PolicyStoreError::CedarSchemaError {
                    file: name.clone(),
                    err: CedarSchemaErrorType::ParseError(e.to_string()),
                }
            })?;

            filenames.push(name.clone());
            fragments.push(fragment);
        }

        let schema = Schema::from_schema_fragments(fragments).map_err(|e| {
            PolicyStoreError::CedarSchemaError {
                file: filenames.join(", "),
                err: CedarSchemaErrorType::ValidationError(e.to_string()),
            }
        })?;

        Ok(Self { schema })
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

        let parsed = ParsedSchema::parse(content, "test.cedarschema")
            .expect("Valid Cedar schema should parse successfully");
        let schema = parsed.get_schema();
        let type_names: Vec<_> = schema
            .entity_types()
            .map(std::string::ToString::to_string)
            .collect();
        assert!(
            type_names.contains(&"TestApp::User".to_string()),
            "Should contain TestApp::User; got: {type_names:?}"
        );
        let action_names: Vec<_> = schema
            .actions()
            .map(std::string::ToString::to_string)
            .collect();
        assert!(
            action_names.contains(&"TestApp::Action::\"view\"".to_string()),
            "Should contain view action; got: {action_names:?}"
        );
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
        let result = ParsedSchema::parse("", "empty.cedarschema");
        // Empty string is a valid (empty) Cedar schema — parsing succeeds
        assert!(
            result.is_ok(),
            "Empty Cedar schema is valid and should parse"
        );
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
    fn test_parse_schema_success_implies_valid() {
        let content = r"
            namespace TestApp {
                entity User;
            }
        ";

        // If parsing succeeds, the schema is inherently valid
        let parsed = ParsedSchema::parse(content, "test.cedarschema");
        assert!(parsed.is_ok(), "Valid Cedar schema should parse");
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
    fn test_whitespace_only_schema_is_valid() {
        let result = ParsedSchema::parse("   \n  \t  \n   ", "whitespace.cedarschema");
        // Whitespace-only content is valid Cedar (empty fragment)
        assert!(
            result.is_ok(),
            "Whitespace-only schema is valid and should parse"
        );
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

        let parsed = ParsedSchema::parse(content, "test.cedarschema")
            .expect("Valid schema should parse for clone test");
        let cloned = parsed.clone();

        // Clone preserves the schema
        assert_eq!(
            parsed.get_schema().entity_types().count(),
            cloned.get_schema().entity_types().count()
        );
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
    fn test_parse_schema_produces_valid_entity_types() {
        let content = r"namespace Test { entity User; }";

        let parsed = ParsedSchema::parse(content, "test.cedarschema")
            .expect("Valid schema should parse for entity type test");
        let type_names: Vec<_> = parsed
            .get_schema()
            .entity_types()
            .map(std::string::ToString::to_string)
            .collect();
        assert!(
            type_names.contains(&"Test::User".to_string()),
            "Should contain Test::User; got: {type_names:?}"
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

        ParsedSchema::parse(schema1, "schema1.cedarschema")
            .expect("Schema1 should parse successfully");
        ParsedSchema::parse(schema2, "schema2.cedarschema")
            .expect("Schema2 should parse successfully");
    }

    #[test]
    fn test_parse_multiple_valid() {
        let files = vec![
            SchemaFile {
                name: "users.cedarschema".to_string(),
                content: r"namespace App { entity User; }".to_string(),
            },
            SchemaFile {
                name: "resources.cedarschema".to_string(),
                content: r"namespace App { entity Resource; }".to_string(),
            },
        ];

        let result = ParsedSchema::parse_multiple(&files);
        let parsed = result.expect("parse_multiple should succeed");

        let type_names: Vec<_> = parsed
            .get_schema()
            .entity_types()
            .map(std::string::ToString::to_string)
            .collect();
        assert!(
            type_names.contains(&"App::User".to_string()),
            "Should contain App::User; got: {type_names:?}"
        );
        assert!(
            type_names.contains(&"App::Resource".to_string()),
            "Should contain App::Resource; got: {type_names:?}"
        );
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
            SchemaFile {
                name: "valid.cedarschema".to_string(),
                content: r"namespace App { entity User; }".to_string(),
            },
            SchemaFile {
                name: "invalid.cedarschema".to_string(),
                content: "this is not valid cedar schema".to_string(),
            },
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
            SchemaFile {
                name: "users.cedarschema".to_string(),
                content: r"namespace Users { entity User; }".to_string(),
            },
            SchemaFile {
                name: "docs.cedarschema".to_string(),
                content: r"namespace Docs { entity Document; entity Folder; }".to_string(),
            },
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
    }

    #[test]
    fn test_parse_multiple_resolves_context_entity_from_another_file() {
        use cedar_policy_core::validator::types::{EntityKind, Type};

        let files = vec![
            SchemaFile {
                name: "users.cedarschema".to_string(),
                content: "namespace App { entity User; entity File; }".to_string(),
            },
            SchemaFile {
                name: "actions.cedarschema".to_string(),
                content: r#"namespace App {
                    action "read" appliesTo {
                        principal: [User],
                        resource: [File],
                        context: { owner: User }
                    };
                }"#
                .to_string(),
            },
        ];

        let parsed = ParsedSchema::parse_multiple(&files)
            .expect("a namespace split across files should merge");
        let action =
            cedar_policy::EntityUid::from_str(r#"App::Action::"read""#).expect("valid action uid");
        let context = parsed
            .get_schema()
            .as_ref()
            .context_type(action.as_ref())
            .expect("the merged schema should know the action");

        let Type::Record { attrs, .. } = context else {
            panic!("an action context is always a record, got: {context:?}");
        };
        let owner = attrs
            .iter()
            .find_map(|(name, attr)| (name == "owner").then_some(attr))
            .expect("context should declare owner");
        assert!(
            matches!(
                owner.attr_type.as_ref(),
                Type::Entity(EntityKind::Entity(lub))
                    if lub.get_single_entity().map(ToString::to_string).as_deref()
                        == Some("App::User")
            ),
            "owner should resolve to the User type declared in another file, got: {owner:?}"
        );
    }

    #[test]
    fn test_parse_multiple_with_actions() {
        let files = vec![
            SchemaFile {
                name: "types.cedarschema".to_string(),
                content: r"
                namespace App {
                    entity User;
                    entity File;
                }
                "
                .to_string(),
            },
            SchemaFile {
                name: "actions.cedarschema".to_string(),
                content: r#"
                namespace App {
                    action "read" appliesTo {
                        principal: [User],
                        resource: [File]
                    };
                }
                "#
                .to_string(),
            },
        ];

        let result = ParsedSchema::parse_multiple(&files);
        let parsed = result.expect("parse_multiple with actions should succeed");

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
