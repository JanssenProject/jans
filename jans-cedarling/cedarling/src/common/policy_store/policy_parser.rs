// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cedar policy and template parsing and validation.
//!
//! This module handles parsing Cedar policy files (.cedar) and extracting
//! policy IDs from @id() annotations. It provides validation and error
//! reporting with file names and line numbers.

use cedar_policy::{Policy, PolicyId, PolicySet, Template};
#[cfg(test)]
use std::collections::HashMap;

use super::errors::{CedarParseErrorDetail, PolicyStoreError, ValidationError};

/// Represents a parsed Cedar policy with metadata.
#[derive(Debug, Clone)]
pub(super) struct ParsedPolicy {
    /// The policy ID (from Cedar engine or @id annotation)
    pub id: PolicyId,
    /// The original filename
    pub filename: String,
    /// The parsed Cedar policy
    pub policy: Policy,
}

/// Represents a parsed Cedar template with metadata.
#[derive(Debug, Clone)]
pub(super) struct ParsedTemplate {
    /// The original filename
    pub filename: String,
    /// The parsed Cedar template
    pub template: Template,
}

/// Cedar policy and template parser.
///
/// Provides methods for parsing Cedar policies and templates from text,
/// extracting @id() annotations, and validating syntax.
pub(super) struct PolicyParser;

impl PolicyParser {
    /// Parse a single policy from Cedar policy text.
    ///
    /// The policy ID is determined by:
    /// 1. Extracting from @id() annotation in the policy text, OR
    /// 2. Deriving from the filename (without .cedar extension)
    ///
    /// Pass the ID to `Policy::parse()` using the annotation or the filename (without
    /// the .cedar extension).
    pub(super) fn parse_policy(
        content: &str,
        filename: &str,
    ) -> Result<ParsedPolicy, PolicyStoreError> {
        // Extract policy ID from @id() annotation or derive from filename
        let policy_id_str = Self::extract_id_annotation(content)
            .or_else(|| Self::derive_id_from_filename(filename));

        let policy_id = match policy_id_str {
            Some(id_str) => {
                // Validate the ID format
                Self::validate_policy_id(&id_str, filename)
                    .map_err(PolicyStoreError::Validation)?;
                PolicyId::new(&id_str)
            },
            None => {
                return Err(PolicyStoreError::CedarParsing {
                    file: filename.to_string(),
                    detail: CedarParseErrorDetail::MissingIdAnnotation,
                });
            },
        };

        // Parse the policy using Cedar engine with the policy ID
        let policy = Policy::parse(Some(policy_id.clone()), content).map_err(|e| {
            PolicyStoreError::CedarParsing {
                file: filename.to_string(),
                detail: CedarParseErrorDetail::ParseError(e.to_string()),
            }
        })?;

        Ok(ParsedPolicy {
            id: policy_id,
            filename: filename.to_string(),
            policy,
        })
    }

    /// Parse a single template from Cedar policy text.
    ///
    /// Templates support slots (e.g., ?principal) and are parsed similarly to policies.
    /// The template ID is extracted from @id() annotation or derived from filename.
    ///
    /// the ID to `Template::parse()` based on annotation or filename.
    pub(super) fn parse_template(
        content: &str,
        filename: &str,
    ) -> Result<ParsedTemplate, PolicyStoreError> {
        // Extract template ID from @id() annotation or derive from filename
        let template_id_str = Self::extract_id_annotation(content)
            .or_else(|| Self::derive_id_from_filename(filename));

        let template_id = match template_id_str {
            Some(id_str) => {
                // Validate the ID format
                Self::validate_policy_id(&id_str, filename)
                    .map_err(PolicyStoreError::Validation)?;
                PolicyId::new(&id_str)
            },
            None => {
                return Err(PolicyStoreError::CedarParsing {
                    file: filename.to_string(),
                    detail: CedarParseErrorDetail::MissingIdAnnotation,
                });
            },
        };

        // Parse the template using Cedar engine with the template ID
        let template = Template::parse(Some(template_id.clone()), content).map_err(|e| {
            PolicyStoreError::CedarParsing {
                file: filename.to_string(),
                detail: CedarParseErrorDetail::ParseError(e.to_string()),
            }
        })?;

        Ok(ParsedTemplate {
            filename: filename.to_string(),
            template,
        })
    }

    /// Parse multiple policies and return a map of policy ID to filename.
    ///
    /// Useful for batch processing of policy files in tests and tooling.
    #[cfg(test)]
    pub(super) fn parse_policies<'a, I>(
        policy_files: I,
    ) -> Result<HashMap<PolicyId, String>, PolicyStoreError>
    where
        I: IntoIterator<Item = (&'a str, &'a str)>,
    {
        let policy_files_vec: Vec<_> = policy_files.into_iter().collect();
        let mut policy_map = HashMap::with_capacity(policy_files_vec.len());

        for (filename, content) in policy_files_vec {
            let parsed = Self::parse_policy(content, filename)?;
            policy_map.insert(parsed.id, parsed.filename);
        }

        Ok(policy_map)
    }

    /// Create a PolicySet from parsed policies and templates.
    ///
    /// Validates that all policies and templates can be successfully added
    /// to the policy set, ensuring no ID conflicts or other issues.
    pub(super) fn create_policy_set(
        policies: Vec<ParsedPolicy>,
        templates: Vec<ParsedTemplate>,
    ) -> Result<PolicySet, PolicyStoreError> {
        let mut policy_set = PolicySet::new();

        // Add all policies
        for parsed in policies {
            policy_set
                .add(parsed.policy)
                .map_err(|e| PolicyStoreError::CedarParsing {
                    file: parsed.filename,
                    detail: CedarParseErrorDetail::AddPolicyFailed(e.to_string()),
                })?;
        }

        // Add all templates
        for parsed in templates {
            policy_set.add_template(parsed.template).map_err(|e| {
                PolicyStoreError::CedarParsing {
                    file: parsed.filename,
                    detail: CedarParseErrorDetail::AddTemplateFailed(e.to_string()),
                }
            })?;
        }

        Ok(policy_set)
    }

    /// Derive a policy ID from a filename.
    ///
    /// Removes the .cedar extension, sanitizes characters, and returns the ID.
    /// Returns None if the filename is empty or invalid.
    fn derive_id_from_filename(filename: &str) -> Option<String> {
        // Extract just the filename without path
        let base_name = filename.rsplit('/').next().unwrap_or(filename);

        // Remove .cedar extension
        let without_ext = base_name.strip_suffix(".cedar").unwrap_or(base_name);

        // If empty after stripping, return None
        if without_ext.is_empty() {
            return None;
        }

        // Replace invalid characters with underscores
        let sanitized: String = without_ext
            .chars()
            .map(|c| {
                if c.is_alphanumeric() || c == '_' || c == '-' || c == ':' {
                    c
                } else {
                    '_'
                }
            })
            .collect();

        Some(sanitized)
    }

    /// Extract @id() annotation from Cedar policy text.
    ///
    /// Looks for @id("...") or @id('...') pattern in comments.
    fn extract_id_annotation(content: &str) -> Option<String> {
        // Look for @id("...") or @id('...') pattern
        for line in content.lines() {
            let trimmed = line.trim();
            if let Some(start_idx) = trimmed.find("@id(") {
                let after_id = &trimmed[start_idx + 4..];
                // Find the string content between quotes
                if let Some(open_quote) = after_id.find('"').or_else(|| after_id.find('\'')) {
                    let quote_char = after_id.chars().nth(open_quote).unwrap();
                    let after_open = &after_id[open_quote + 1..];
                    if let Some(close_quote) = after_open.find(quote_char) {
                        return Some(after_open[..close_quote].to_string());
                    }
                }
            }
        }
        None
    }

    /// Validate policy ID format (alphanumeric, underscore, hyphen, colon only).
    fn validate_policy_id(id: &str, filename: &str) -> Result<(), ValidationError> {
        if id.is_empty() {
            return Err(ValidationError::EmptyPolicyId {
                file: filename.to_string(),
            });
        }

        // Check for valid characters (alphanumeric, underscore, hyphen, colon)
        if !id
            .chars()
            .all(|c| c.is_alphanumeric() || c == '_' || c == '-' || c == ':')
        {
            return Err(ValidationError::InvalidPolicyIdCharacters {
                file: filename.to_string(),
                id: id.to_string(),
            });
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::str::FromStr;

    #[test]
    fn test_parse_simple_policy() {
        let policy_text = r#"
            permit(
                principal == User::"alice",
                action == Action::"view",
                resource == File::"report.txt"
            );
        "#;

        let result = PolicyParser::parse_policy(policy_text, "test.cedar");
        assert!(result.is_ok());

        let parsed = result.unwrap();
        assert_eq!(parsed.filename, "test.cedar");
        // ID should be derived from filename
        assert_eq!(parsed.id.to_string(), "test");
    }

    #[test]
    fn test_parse_invalid_policy() {
        let policy_text = "this is not valid cedar syntax";

        let result = PolicyParser::parse_policy(policy_text, "invalid.cedar");
        let err = result.expect_err("Expected CedarParsing error for invalid syntax");

        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing { file, detail: CedarParseErrorDetail::ParseError(_) }
                if file == "invalid.cedar"
            ),
            "Expected CedarParsing error with ParseError detail, got: {:?}",
            err
        );
    }

    #[test]
    fn test_parse_template() {
        let template_text = r#"
            permit(
                principal == ?principal,
                action == Action::"view",
                resource == File::"report.txt"
            );
        "#;

        let result = PolicyParser::parse_template(template_text, "template.cedar");
        assert!(result.is_ok());

        let parsed = result.unwrap();
        assert_eq!(parsed.filename, "template.cedar");
        // ID should be derived from filename - get from template directly
        assert_eq!(parsed.template.id().to_string(), "template");
    }

    #[test]
    fn test_parse_multiple_policies() {
        let policy1 = r#"
            permit(
                principal == User::"alice",
                action == Action::"view",
                resource == File::"doc1.txt"
            );
        "#;

        let policy2 = r#"
            permit(
                principal == User::"bob",
                action == Action::"edit",
                resource == File::"doc2.txt"
            );
        "#;

        let files = vec![("policy1.cedar", policy1), ("policy2.cedar", policy2)];

        let result = PolicyParser::parse_policies(files);
        assert!(result.is_ok());

        let policy_map = result.unwrap();

        assert!(!policy_map.is_empty());
    }

    #[test]
    fn test_extract_id_annotation_double_quotes() {
        let policy_text = r#"
            // @id("my-policy-id")
            permit(
                principal == User::"alice",
                action == Action::"view",
                resource == File::"report.txt"
            );
        "#;

        let id = PolicyParser::extract_id_annotation(policy_text);
        assert_eq!(id, Some("my-policy-id".to_string()));
    }

    #[test]
    fn test_extract_id_annotation_single_quotes() {
        let policy_text = r#"
            // @id('another-policy-id')
            permit(principal, action, resource);
        "#;

        let id = PolicyParser::extract_id_annotation(policy_text);
        assert_eq!(id, Some("another-policy-id".to_string()));
    }

    #[test]
    fn test_extract_id_annotation_not_found() {
        let policy_text = r#"
            permit(principal, action, resource);
        "#;

        let id = PolicyParser::extract_id_annotation(policy_text);
        assert_eq!(id, None);
    }

    #[test]
    fn test_validate_policy_id_valid() {
        let result = PolicyParser::validate_policy_id("valid_policy-id:123", "test.cedar");
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_policy_id_empty() {
        let result = PolicyParser::validate_policy_id("", "test.cedar");
        let err = result.expect_err("Expected EmptyPolicyId error for empty policy ID");
        assert!(
            matches!(err, ValidationError::EmptyPolicyId { .. }),
            "Expected EmptyPolicyId error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_validate_policy_id_invalid_chars() {
        let result = PolicyParser::validate_policy_id("invalid@policy#id", "test.cedar");
        let err = result.expect_err("Expected InvalidPolicyIdCharacters error for invalid chars");
        assert!(
            matches!(err, ValidationError::InvalidPolicyIdCharacters { .. }),
            "Expected InvalidPolicyIdCharacters error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_create_policy_set() {
        // When parsing a single permit and forbid, they get different content so different IDs
        let combined_text = r#"
            permit(principal == User::"alice", action, resource);
            forbid(principal == User::"bob", action, resource);
        "#;

        // Parse as a set to get unique IDs
        let policy_set = PolicySet::from_str(combined_text).unwrap();
        let policies: Vec<ParsedPolicy> = policy_set
            .policies()
            .map(|p| ParsedPolicy {
                id: p.id().clone(),
                filename: "test.cedar".to_string(),
                policy: p.clone(),
            })
            .collect();

        let result = PolicyParser::create_policy_set(policies, vec![]);
        assert!(result.is_ok());

        let policy_set = result.unwrap();
        assert!(!policy_set.is_empty());
    }

    #[test]
    fn test_create_policy_set_with_template() {
        let policy_text = r#"permit(principal, action, resource);"#;
        let template_text = r#"permit(principal == ?principal, action, resource);"#;

        let parsed_policy = PolicyParser::parse_policy(policy_text, "policy.cedar").unwrap();
        let parsed_template =
            PolicyParser::parse_template(template_text, "template.cedar").unwrap();

        // Verify IDs are derived from filenames
        assert_eq!(parsed_policy.id.to_string(), "policy");
        assert_eq!(parsed_template.template.id().to_string(), "template");

        let result = PolicyParser::create_policy_set(vec![parsed_policy], vec![parsed_template]);
        assert!(result.is_ok());

        let policy_set = result.unwrap();
        assert!(!policy_set.is_empty());
    }

    #[test]
    fn test_derive_id_from_filename() {
        assert_eq!(
            PolicyParser::derive_id_from_filename("my-policy.cedar"),
            Some("my-policy".to_string())
        );
        assert_eq!(
            PolicyParser::derive_id_from_filename("/path/to/policy.cedar"),
            Some("policy".to_string())
        );
        assert_eq!(
            PolicyParser::derive_id_from_filename("policy with spaces.cedar"),
            Some("policy_with_spaces".to_string())
        );
        assert_eq!(PolicyParser::derive_id_from_filename(".cedar"), None);
    }

    #[test]
    fn test_parse_policy_with_id_annotation() {
        let policy_text = r#"
            // @id("custom-policy-id")
            permit(
                principal == User::"alice",
                action == Action::"view",
                resource == File::"report.txt"
            );
        "#;

        let result = PolicyParser::parse_policy(policy_text, "ignored.cedar");
        assert!(result.is_ok());

        let parsed = result.unwrap();
        // ID should come from @id annotation, not filename
        assert_eq!(parsed.id.to_string(), "custom-policy-id");
    }
}
