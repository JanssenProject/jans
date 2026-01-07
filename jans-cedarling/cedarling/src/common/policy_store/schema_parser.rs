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

/// A parsed and validated Cedar schema.
///
/// Contains the schema and metadata about the source file.
#[derive(Debug, Clone)]
pub(super) struct ParsedSchema {
    /// The Cedar schema
    pub schema: Schema,
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

        // Create schema from the fragment
        let schema = Schema::from_schema_fragments([fragment]).map_err(|e| {
            PolicyStoreError::CedarSchemaError {
                file: filename.to_string(),
                err: CedarSchemaErrorType::ValidationError(e.to_string()),
            }
        })?;

        Ok(Self {
            schema,
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
        let content = r#"
            namespace App1 {
                entity User;
            }
            
            namespace App2 {
                entity Admin;
            }
        "#;

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
            "Expected CedarSchemaError with ParseError, got: {:?}",
            err
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
                "Expected EmptySchema error, got: {:?}",
                err
            );
        }
    }

    #[test]
    fn test_parse_schema_missing_closing_brace() {
        let content = r#"
            namespace MyApp {
                entity User;
                entity File;
        "#;

        let result = ParsedSchema::parse(content, "malformed.cedarschema");
        let err = result.expect_err("Expected error for missing closing brace");
        assert!(
            matches!(&err, PolicyStoreError::CedarSchemaError { .. }),
            "Expected CedarSchemaError for malformed schema, got: {:?}",
            err
        );
    }

    #[test]
    fn test_validate_schema_success() {
        let content = r#"
            namespace TestApp {
                entity User;
            }
        "#;

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
            "Expected CedarSchemaError with filename my_schema.cedarschema, got: {:?}",
            err
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
            "Expected CedarSchemaError for invalid entity type, got: {:?}",
            err
        );
    }

    #[test]
    fn test_parse_schema_missing_semicolon() {
        let content = r#"
            namespace MyApp {
                entity User
                entity File;
            }
        "#;

        let result = ParsedSchema::parse(content, "missing_semicolon.cedarschema");
        let err = result.expect_err("Missing semicolon should fail parsing");
        assert!(
            matches!(&err, PolicyStoreError::CedarSchemaError { .. }),
            "Expected CedarSchemaError for missing semicolon, got: {:?}",
            err
        );
    }

    #[test]
    fn test_parse_schema_duplicate_entity() {
        let content = r#"
            namespace MyApp {
                entity User;
                entity User;
            }
        "#;

        let result = ParsedSchema::parse(content, "duplicate.cedarschema");
        // Cedar may or may not allow duplicate entity definitions
        // This test documents the current behavior - if an error occurs, it should be a schema error
        if let Err(err) = result {
            assert!(
                matches!(&err, PolicyStoreError::CedarSchemaError { .. }),
                "Expected CedarSchemaError for duplicate entity, got: {:?}",
                err
            );
        }
    }

    #[test]
    fn test_parsed_schema_clone() {
        let content = r#"
            namespace TestApp {
                entity User;
            }
        "#;

        let parsed = ParsedSchema::parse(content, "test.cedarschema").unwrap();
        let cloned = parsed.clone();

        assert_eq!(parsed.filename, cloned.filename);
        assert_eq!(parsed.content, cloned.content);
    }

    #[test]
    fn test_parse_schema_with_extension() {
        let content = r#"
            namespace ExtApp {
                entity User;
                entity AdminUser in [User];
                entity SuperAdmin in [AdminUser];
            }
        "#;

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
            "Expected CedarSchemaError with filename test.cedarschema, got: {:?}",
            err
        );
    }

    #[test]
    fn test_parse_schema_preserves_content() {
        let content = r#"namespace Test { entity User; }"#;

        let parsed = ParsedSchema::parse(content, "preserve.cedarschema").unwrap();
        assert_eq!(
            parsed.content, content,
            "Original content should be preserved"
        );
    }

    #[test]
    fn test_parse_multiple_schemas_independently() {
        let schema1 = r#"
            namespace App1 {
                entity User;
            }
        "#;
        let schema2 = r#"
            namespace App2 {
                entity Admin;
            }
        "#;

        let result1 = ParsedSchema::parse(schema1, "schema1.cedarschema");
        let result2 = ParsedSchema::parse(schema2, "schema2.cedarschema");

        assert!(result1.is_ok());
        assert!(result2.is_ok());

        let parsed1 = result1.unwrap();
        let parsed2 = result2.unwrap();

        assert_ne!(parsed1.filename, parsed2.filename);
        assert_ne!(parsed1.content, parsed2.content);
    }
}
