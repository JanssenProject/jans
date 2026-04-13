// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cedar policy and template parsing and validation.
//!
//! This module handles parsing Cedar policy files (.cedar) and extracting
//! policy IDs from `@id()` annotations. It provides validation and error
//! reporting with file names and line numbers.
//!
//! A file may contain multiple policies. Single-policy files use the first
//! `@id()` annotation or the filename (without extension) as the policy id.
//! When a file contains more than one policy, each policy must use Cedar's
//! `@id("...")` policy annotation before `permit` / `forbid`; filename-based ids
//! are not applied in that case. Policies are renamed so their [`PolicyId`] matches
//! the `@id` value (Cedar 4 defaults internal ids to `policy0`, `policy1`, ...).

use cedar_policy::{Policy, PolicyId, PolicySet, Template};
#[cfg(test)]
use std::collections::HashMap;
use std::str::FromStr;

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
/// extracting `@id()` annotations, and validating syntax.
pub(super) struct PolicyParser;

impl PolicyParser {
    /// Parse Cedar policy text from one file.
    ///
    /// Returns one [`ParsedPolicy`] for single-policy files, or one entry per policy
    /// when the file contains multiple policies (see module docs).
    ///
    /// The policy ID for single-policy files is determined by:
    /// 1. Extracting from `@id()` annotation in the policy text, OR
    /// 2. Deriving from the filename (without .cedar extension)
    ///
    /// Multi-policy files are parsed with [`PolicySet`]; each policy must have a
    /// Cedar `@id("...")` annotation, then policies are given [`PolicyId`]s equal to
    /// those annotation values.
    pub(super) fn parse_policy(
        content: &str,
        filename: &str,
    ) -> Result<Vec<ParsedPolicy>, PolicyStoreError> {
        let policy_set =
            PolicySet::from_str(content).map_err(|e| PolicyStoreError::CedarParsing {
                file: filename.to_string(),
                detail: CedarParseErrorDetail::ParseError(e.to_string()),
            })?;

        let policies: Vec<Policy> = policy_set.policies().cloned().collect();

        match policies.len() {
            0 => Err(PolicyStoreError::CedarParsing {
                file: filename.to_string(),
                detail: CedarParseErrorDetail::ParseError("no policies found in file".to_string()),
            }),
            1 => {
                let policy = policies.into_iter().next().expect("len == 1 checked above");
                // Prefer Cedar's `@id("...")` annotation (same as multi-policy branch);
                // fall back to scanning `@id(...)` in comments, then to the filename.
                let id_str = policy
                    .annotation("id")
                    .filter(|s| !s.is_empty())
                    .map(str::to_owned)
                    .or_else(|| Self::extract_id_annotation(content))
                    .or_else(|| Self::derive_id_from_filename(filename))
                    .ok_or_else(|| PolicyStoreError::CedarParsing {
                        file: filename.to_string(),
                        detail: CedarParseErrorDetail::MissingIdAnnotation,
                    })?;
                Self::validate_policy_id(&id_str, filename)
                    .map_err(PolicyStoreError::Validation)?;
                let policy = policy.new_id(PolicyId::new(&id_str));
                Ok(vec![ParsedPolicy {
                    id: policy.id().clone(),
                    filename: filename.to_string(),
                    policy,
                }])
            },
            n => {
                let mut out = Vec::with_capacity(n);
                let mut seen_ids = std::collections::HashSet::with_capacity(n);
                for policy in policies {
                    // In Cedar 4+, `@id("...")` is a policy annotation named `id`; the AST
                    // `PolicyId` defaults to `policy0`, `policy1`, ... until renamed.
                    let id_str = policy
                        .annotation("id")
                        .filter(|s| !s.is_empty())
                        .map(str::to_owned)
                        .ok_or_else(|| {
                            // Cedar's `policies()` iteration order is not guaranteed,
                            // so `policy.id()` here (e.g. `policy0`) may not match file
                            // order. Scan the source text to point at the first policy
                            // start that has no preceding `@id(...)`.
                            let (line, snippet) = Self::find_first_policy_without_id(content)
                                .unwrap_or((0, String::new()));
                            PolicyStoreError::CedarParsing {
                                file: filename.to_string(),
                                detail: CedarParseErrorDetail::MultiPolicyMissingExplicitId {
                                    line,
                                    snippet,
                                },
                            }
                        })?;
                    Self::validate_policy_id(&id_str, filename)
                        .map_err(PolicyStoreError::Validation)?;
                    if !seen_ids.insert(id_str.clone()) {
                        return Err(PolicyStoreError::CedarParsing {
                            file: filename.to_string(),
                            detail: CedarParseErrorDetail::DuplicatePolicyIdInFile { id: id_str },
                        });
                    }
                    let policy = policy.new_id(PolicyId::new(&id_str));
                    out.push(ParsedPolicy {
                        id: policy.id().clone(),
                        filename: filename.to_string(),
                        policy,
                    });
                }
                Ok(out)
            },
        }
    }

    /// Parse a single template from Cedar policy text.
    ///
    /// Templates support slots (e.g., ?principal) and are parsed similarly to policies.
    /// The template ID is extracted from `@id()` annotation or derived from filename.
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
            for parsed in Self::parse_policy(content, filename)? {
                policy_map.insert(parsed.id, parsed.filename);
            }
        }

        Ok(policy_map)
    }

    /// Create a [`PolicySet`] from parsed policies and templates.
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

    /// Extract the first `@id()` annotation from Cedar policy text.
    ///
    /// Looks for `@id("...")` or `@id('...')` on each line.
    fn extract_id_annotation(content: &str) -> Option<String> {
        Self::collect_id_annotations(content).into_iter().next()
    }

    /// Collect every `@id("...")` / `@id('...')` value in file order.
    fn collect_id_annotations(content: &str) -> Vec<String> {
        let mut ids = Vec::new();
        for line in content.lines() {
            if let Some(id) = Self::parse_id_from_line_trimmed(line.trim()) {
                ids.push(id);
            }
        }
        ids
    }

    fn parse_id_from_line_trimmed(trimmed: &str) -> Option<String> {
        let start_idx = trimmed.find("@id(")?;
        let after_id = &trimmed[start_idx + 4..];
        // Pair the byte offset with the quote we matched on. `str::find` returns a
        // byte offset, not a char index, so recovering the quote via
        // `chars().nth(open_quote)` would pick the wrong character once any
        // multi-byte UTF-8 appears before the quote (e.g. `@id(🙂"x")`).
        let (open_quote, quote_char) = after_id
            .find('"')
            .map(|i| (i, '"'))
            .or_else(|| after_id.find('\'').map(|i| (i, '\'')))?;
        let after_open = &after_id[open_quote + 1..];
        let close_quote = after_open.find(quote_char)?;
        Some(after_open[..close_quote].to_string())
    }

    /// Scan source text for the first `permit` / `forbid` policy whose preceding
    /// lines (back to the start of the file or the previous policy) contain no
    /// `@id(...)` annotation. Returns `(1-based line, trimmed-and-truncated snippet)`.
    fn find_first_policy_without_id(content: &str) -> Option<(usize, String)> {
        let lines: Vec<&str> = content.lines().collect();
        let mut window_start = 0usize;
        for (idx, line) in lines.iter().enumerate() {
            let trimmed = line.trim_start();
            let is_start = trimmed.starts_with("permit(")
                || trimmed.starts_with("forbid(")
                || trimmed.starts_with("permit ")
                || trimmed.starts_with("forbid ");
            if !is_start {
                continue;
            }
            let has_id = lines[window_start..idx]
                .iter()
                .any(|l| Self::parse_id_from_line_trimmed(l.trim()).is_some());
            if !has_id {
                let mut snippet = trimmed.trim_end().to_string();
                const MAX: usize = 80;
                if snippet.chars().count() > MAX {
                    snippet = snippet.chars().take(MAX).collect::<String>() + "...";
                }
                return Some((idx + 1, snippet));
            }
            window_start = idx + 1;
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
        let parsed = result.expect("parse_policy should succeed for simple policy");
        assert_eq!(parsed.len(), 1, "expected exactly one policy");
        assert_eq!(parsed[0].filename, "test.cedar");
        // ID should be derived from filename
        assert_eq!(parsed[0].id.to_string(), "test");
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
            "Expected CedarParsing error with ParseError detail, got: {err:?}"
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
        let parsed = result.expect("parse_template should succeed for template.cedar");
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
        let policy_map =
            result.expect("parse_policies should succeed for two separate policy files");

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
        let policy_text = r"
            // @id('another-policy-id')
            permit(principal, action, resource);
        ";

        let id = PolicyParser::extract_id_annotation(policy_text);
        assert_eq!(id, Some("another-policy-id".to_string()));
    }

    #[test]
    fn test_extract_id_annotation_not_found() {
        let policy_text = r"
            permit(principal, action, resource);
        ";

        let id = PolicyParser::extract_id_annotation(policy_text);
        assert_eq!(id, None);
    }

    #[test]
    fn test_validate_policy_id_valid() {
        PolicyParser::validate_policy_id("valid_policy-id:123", "test.cedar")
            .expect("valid_policy-id:123 should pass policy id validation for test.cedar");
    }

    #[test]
    fn test_validate_policy_id_empty() {
        let result = PolicyParser::validate_policy_id("", "test.cedar");
        let err = result.expect_err("Expected EmptyPolicyId error for empty policy ID");
        assert!(
            matches!(err, ValidationError::EmptyPolicyId { .. }),
            "Expected EmptyPolicyId error, got: {err:?}"
        );
    }

    #[test]
    fn test_validate_policy_id_invalid_chars() {
        let result = PolicyParser::validate_policy_id("invalid@policy#id", "test.cedar");
        let err = result.expect_err("Expected InvalidPolicyIdCharacters error for invalid chars");
        assert!(
            matches!(err, ValidationError::InvalidPolicyIdCharacters { .. }),
            "Expected InvalidPolicyIdCharacters error, got: {err:?}"
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
        let policy_set = PolicySet::from_str(combined_text)
            .expect("combined permit/forbid text should parse as PolicySet");
        let policies: Vec<ParsedPolicy> = policy_set
            .policies()
            .map(|p| ParsedPolicy {
                id: p.id().clone(),
                filename: "test.cedar".to_string(),
                policy: p.clone(),
            })
            .collect();

        let policy_set = PolicyParser::create_policy_set(policies, vec![])
            .expect("create_policy_set should succeed for permit+forbid policies");
        assert!(!policy_set.is_empty());
    }

    #[test]
    fn test_create_policy_set_with_template() {
        let policy_text = r"permit(principal, action, resource);";
        let template_text = r"permit(principal == ?principal, action, resource);";

        let parsed_policy = PolicyParser::parse_policy(policy_text, "policy.cedar")
            .expect("parse_policy should succeed for policy.cedar");
        assert_eq!(parsed_policy.len(), 1, "expected exactly one policy");
        let parsed_template = PolicyParser::parse_template(template_text, "template.cedar")
            .expect("parse_template should succeed for template.cedar");

        // Verify IDs are derived from filenames
        assert_eq!(parsed_policy[0].id.to_string(), "policy");
        assert_eq!(parsed_template.template.id().to_string(), "template");

        let policy_set = PolicyParser::create_policy_set(parsed_policy, vec![parsed_template])
            .expect("create_policy_set should succeed with one policy and one template");
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
        let parsed = result.expect("parse_policy should succeed with @id annotation");
        assert_eq!(parsed.len(), 1, "expected exactly one policy");
        // ID should come from @id annotation, not filename
        assert_eq!(parsed[0].id.to_string(), "custom-policy-id");
    }

    #[test]
    fn test_parse_multi_policy_file_success() {
        let policy_text = r#"
@id("researcher-search")
permit(
  principal == User::"researcher",
  action == Action::"search",
  resource == File::"doc1.txt"
);

@id("researcher-fetch")
permit(
  principal == User::"researcher",
  action == Action::"edit",
  resource == File::"doc2.txt"
);
        "#;

        let result = PolicyParser::parse_policy(policy_text, "researcher.cedar");
        let parsed = result.expect("multi-policy file with @id each should parse");

        assert_eq!(parsed.len(), 2, "expected two policies from one file");
        let ids: Vec<String> = parsed.iter().map(|p| p.id.to_string()).collect();
        assert!(
            ids.contains(&"researcher-search".to_string())
                && ids.contains(&"researcher-fetch".to_string()),
            "expected both policy ids, got {ids:?}"
        );
        for p in &parsed {
            assert_eq!(p.filename, "researcher.cedar");
        }
    }

    #[test]
    fn test_parse_multi_policy_file_duplicate_id() {
        let policy_text = r#"
@id("dup")
permit(
  principal == User::"alice",
  action == Action::"view",
  resource == File::"doc1.txt"
);

@id("dup")
permit(
  principal == User::"bob",
  action == Action::"edit",
  resource == File::"doc2.txt"
);
        "#;

        let err = PolicyParser::parse_policy(policy_text, "dup.cedar")
            .expect_err("duplicate @id within a file should fail");

        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing {
                    file,
                    detail: CedarParseErrorDetail::DuplicatePolicyIdInFile { id }
                } if file == "dup.cedar" && id == "dup"
            ),
            "expected DuplicatePolicyIdInFile, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_multi_policy_file_missing_explicit_id() {
        let policy_text = r#"
@id("only-one")
permit(
  principal == User::"alice",
  action == Action::"view",
  resource == File::"doc1.txt"
);
permit(
  principal == User::"bob",
  action == Action::"edit",
  resource == File::"doc2.txt"
);
        "#;

        let result = PolicyParser::parse_policy(policy_text, "two-permits.cedar");
        let err = result.expect_err("second policy without @id should fail");

        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing {
                    file,
                    detail: CedarParseErrorDetail::MultiPolicyMissingExplicitId { line, snippet }
                } if file == "two-permits.cedar"
                    && *line > 0
                    && (snippet.starts_with("permit(") || snippet.starts_with("permit "))
            ),
            "expected MultiPolicyMissingExplicitId with line + permit snippet, got: {err:?}"
        );
    }
}
