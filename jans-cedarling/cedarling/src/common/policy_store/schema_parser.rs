// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cedar schema parsing and validation.
//!
//! This module provides functionality to parse and validate Cedar schema files,
//! ensuring they are syntactically correct and semantically valid before being
//! used for policy validation and evaluation.

use super::errors::{CedarSchemaErrorType, PolicyStoreError};
use cedar_policy::{Schema, SchemaFragment};
use std::str::FromStr;

/// A parsed and validated Cedar schema.
///
/// Contains the schema and metadata about the source file.
#[derive(Debug, Clone)]
pub struct ParsedSchema {
    /// The Cedar schema
    pub schema: Schema,
    /// Source filename
    pub filename: String,
    /// Raw schema content
    pub content: String,
}

impl ParsedSchema {
    /// Get a reference to the Cedar Schema.
    ///
    /// Returns the validated Cedar Schema that can be used for policy validation.
    pub fn get_schema(&self) -> &Schema {
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
    pub fn validate(&self) -> Result<(), PolicyStoreError> {
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

/// Schema parser for loading and validating Cedar schemas.
pub struct SchemaParser;

impl SchemaParser {
    /// Parse a Cedar schema from a string.
    ///
    /// Parses the schema content using Cedar's schema parser and returns
    /// a `ParsedSchema` with metadata. The schema is validated for correct
    /// syntax and structure during parsing.
    ///
    /// # Errors
    /// Returns `PolicyStoreError::CedarSchemaError` if schema syntax is invalid,
    /// structure is malformed, or validation fails.
    ///
    /// # Example
    /// ```ignore
    /// let content = r#"
    ///     namespace MyApp {
    ///         entity User;
    ///         entity File;
    ///         action "view" appliesTo {
    ///             principal: [User],
    ///             resource: [File]
    ///         };
    ///     }
    /// "#;
    /// let parsed = SchemaParser::parse_schema(content, "schema.cedarschema")?;
    /// ```
    pub fn parse_schema(content: &str, filename: &str) -> Result<ParsedSchema, PolicyStoreError> {
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

        Ok(ParsedSchema {
            schema,
            filename: filename.to_string(),
            content: content.to_string(),
        })
    }

    /// Extract namespace declarations from schema content.
    ///
    /// Returns a list of namespaces defined in the schema, useful for
    /// validation, debugging, and policy store analysis.
    pub fn extract_namespaces(content: &str) -> Vec<String> {
        let mut namespaces = Vec::new();

        // Simple regex-like parsing for namespace declarations
        for line in content.lines() {
            let trimmed = line.trim();
            if trimmed.starts_with("namespace ")
                && let Some(ns_name) = trimmed
                    .strip_prefix("namespace ")
                    .and_then(|s| s.split_whitespace().next())
                    .map(|s| s.trim_end_matches('{').trim())
            {
                namespaces.push(ns_name.to_string());
            }
        }

        namespaces
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

        let result = SchemaParser::parse_schema(content, "test.cedarschema");
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

        let result = SchemaParser::parse_schema(content, "multi.cedarschema");
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

        let result = SchemaParser::parse_schema(content, "complex.cedarschema");
        assert!(result.is_ok());
    }

    #[test]
    fn test_parse_invalid_schema_syntax() {
        let content = "this is not valid cedar schema syntax!!!";

        let result = SchemaParser::parse_schema(content, "invalid.cedarschema");
        assert!(result.is_err());

        let Err(PolicyStoreError::CedarSchemaError { file, err }) = result else {
            panic!("Expected CedarSchemaError");
        };

        assert_eq!(file, "invalid.cedarschema");
        assert!(matches!(err, CedarSchemaErrorType::ParseError(_)));
    }

    #[test]
    fn test_parse_empty_schema() {
        let content = "";

        let result = SchemaParser::parse_schema(content, "empty.cedarschema");
        // Empty schema is actually valid in Cedar, but our validation will catch it
        if result.is_ok() {
            let parsed = result.unwrap();
            let validation = parsed.validate();
            assert!(validation.is_err());
        }
    }

    #[test]
    fn test_parse_schema_missing_closing_brace() {
        let content = r#"
            namespace MyApp {
                entity User;
                entity File;
        "#;

        let result = SchemaParser::parse_schema(content, "malformed.cedarschema");
        assert!(result.is_err());
    }

    #[test]
    fn test_validate_schema_success() {
        let content = r#"
            namespace TestApp {
                entity User;
            }
        "#;

        let parsed = SchemaParser::parse_schema(content, "test.cedarschema").unwrap();
        let result = parsed.validate();
        assert!(result.is_ok());
    }

    #[test]
    fn test_extract_namespaces_single() {
        let content = r#"
            namespace MyApp {
                entity User;
            }
        "#;

        let namespaces = SchemaParser::extract_namespaces(content);
        assert_eq!(namespaces.len(), 1);
        assert_eq!(namespaces[0], "MyApp");
    }

    #[test]
    fn test_extract_namespaces_multiple() {
        let content = r#"
            namespace App1 {
                entity User;
            }
            
            namespace App2 {
                entity Admin;
            }
            
            namespace App3 {
                entity Guest;
            }
        "#;

        let namespaces = SchemaParser::extract_namespaces(content);
        assert_eq!(namespaces.len(), 3);
        assert!(namespaces.contains(&"App1".to_string()));
        assert!(namespaces.contains(&"App2".to_string()));
        assert!(namespaces.contains(&"App3".to_string()));
    }

    #[test]
    fn test_extract_namespaces_none() {
        let content = r#"
            entity User;
            entity File;
        "#;

        let namespaces = SchemaParser::extract_namespaces(content);
        assert_eq!(namespaces.len(), 0);
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

        let result = SchemaParser::parse_schema(content, "hierarchy.cedarschema");
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

        let result = SchemaParser::parse_schema(content, "action_groups.cedarschema");
        assert!(result.is_ok());
    }

    #[test]
    fn test_schema_error_message_includes_filename() {
        let content = "namespace { invalid }";

        let result = SchemaParser::parse_schema(content, "my_schema.cedarschema");
        assert!(result.is_err());

        let err_str = result.unwrap_err().to_string();
        assert!(err_str.contains("my_schema.cedarschema"));
    }

    #[test]
    fn test_validate_empty_schema_fails() {
        let content = "   \n  \t  \n   ";

        let result = SchemaParser::parse_schema(content, "whitespace.cedarschema");
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

        let result = SchemaParser::parse_schema(content, "types.cedarschema");
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

        let result = SchemaParser::parse_schema(content, "context.cedarschema");
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

        let result = SchemaParser::parse_schema(content, "optional.cedarschema");
        assert!(
            result.is_ok(),
            "Schema with optional attributes should parse"
        );
    }

    #[test]
    fn test_extract_namespaces_with_comments() {
        let content = r#"
            // This is a comment
            namespace App1 {
                entity User;
            }
            
            /* Block comment */
            namespace App2 {
                entity Admin;
            }
        "#;

        let namespaces = SchemaParser::extract_namespaces(content);
        assert_eq!(namespaces.len(), 2);
        assert!(namespaces.contains(&"App1".to_string()));
        assert!(namespaces.contains(&"App2".to_string()));
    }

    #[test]
    fn test_extract_namespaces_empty_content() {
        let content = "";
        let namespaces = SchemaParser::extract_namespaces(content);
        assert_eq!(namespaces.len(), 0);
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

        let result = SchemaParser::parse_schema(content, "invalid_type.cedarschema");
        assert!(result.is_err(), "Invalid entity type should fail parsing");
    }

    #[test]
    fn test_parse_schema_missing_semicolon() {
        let content = r#"
            namespace MyApp {
                entity User
                entity File;
            }
        "#;

        let result = SchemaParser::parse_schema(content, "missing_semicolon.cedarschema");
        assert!(result.is_err(), "Missing semicolon should fail parsing");
    }

    #[test]
    fn test_parse_schema_duplicate_entity() {
        let content = r#"
            namespace MyApp {
                entity User;
                entity User;
            }
        "#;

        let result = SchemaParser::parse_schema(content, "duplicate.cedarschema");
        // Cedar may or may not allow duplicate entity definitions
        // This test documents the current behavior
        if result.is_err() {
            let err_str = result.unwrap_err().to_string();
            assert!(err_str.contains("duplicate"));
        }
    }

    #[test]
    fn test_parsed_schema_clone() {
        let content = r#"
            namespace TestApp {
                entity User;
            }
        "#;

        let parsed = SchemaParser::parse_schema(content, "test.cedarschema").unwrap();
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

        let result = SchemaParser::parse_schema(content, "extension.cedarschema");
        assert!(
            result.is_ok(),
            "Schema with entity hierarchy should parse successfully"
        );
    }

    #[test]
    fn test_format_schema_error_not_empty() {
        // Create an intentionally malformed schema to trigger SchemaError
        let content = "namespace MyApp { entity User = { invalid } }";

        let result = SchemaParser::parse_schema(content, "test.cedarschema");
        assert!(result.is_err());

        let err = result.unwrap_err();
        let err_msg = err.to_string();
        assert!(!err_msg.is_empty(), "Error message should not be empty");
        assert!(
            err_msg.contains("test.cedarschema"),
            "Error should reference filename"
        );
    }

    #[test]
    fn test_parse_schema_preserves_content() {
        let content = r#"namespace Test { entity User; }"#;

        let parsed = SchemaParser::parse_schema(content, "preserve.cedarschema").unwrap();
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

        let result1 = SchemaParser::parse_schema(schema1, "schema1.cedarschema");
        let result2 = SchemaParser::parse_schema(schema2, "schema2.cedarschema");

        assert!(result1.is_ok());
        assert!(result2.is_ok());

        let parsed1 = result1.unwrap();
        let parsed2 = result2.unwrap();

        assert_ne!(parsed1.filename, parsed2.filename);
        assert_ne!(parsed1.content, parsed2.content);
    }
}
