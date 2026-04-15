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

        // Reject policy files that also contain templates (slots like `?principal`).
        // Cedar parses them into a separate `templates()` collection, so iterating only
        // `policies()` would silently drop them.
        let template_count = policy_set.templates().count();
        if template_count > 0 {
            return Err(PolicyStoreError::CedarParsing {
                file: filename.to_string(),
                detail: CedarParseErrorDetail::TemplatesInPolicyFile {
                    count: template_count,
                },
            });
        }

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
                    let id_str = policy.annotation("id").map(str::to_owned).ok_or_else(|| {
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

    /// Parse Cedar template text from one file.
    ///
    /// Symmetric to [`parse_policy`](Self::parse_policy):
    /// returns one [`ParsedTemplate`] for single-template files, or one entry per
    /// template when the file contains multiple. Templates use slots (e.g. `?principal`).
    ///
    /// Single-template files use the `@id("...")` annotation, then the comment-style
    /// `@id(...)`, then the filename (without `.cedar`) as the template id.
    /// Multi-template files require a Cedar `@id("...")` annotation on each template.
    /// Files that mix templates with non-template policies are rejected.
    pub(super) fn parse_template(
        content: &str,
        filename: &str,
    ) -> Result<Vec<ParsedTemplate>, PolicyStoreError> {
        let policy_set =
            PolicySet::from_str(content).map_err(|e| PolicyStoreError::CedarParsing {
                file: filename.to_string(),
                detail: CedarParseErrorDetail::ParseError(e.to_string()),
            })?;

        // Reject template files that also contain non-template policies.
        let policy_count = policy_set.policies().count();
        if policy_count > 0 {
            return Err(PolicyStoreError::CedarParsing {
                file: filename.to_string(),
                detail: CedarParseErrorDetail::PoliciesInTemplateFile {
                    count: policy_count,
                },
            });
        }

        let templates: Vec<Template> = policy_set.templates().cloned().collect();

        match templates.len() {
            0 => Err(PolicyStoreError::CedarParsing {
                file: filename.to_string(),
                detail: CedarParseErrorDetail::ParseError("no templates found in file".to_string()),
            }),
            1 => {
                let template = templates
                    .into_iter()
                    .next()
                    .expect("len == 1 checked above");
                let id_str = template
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
                let template = template.new_id(PolicyId::new(&id_str));
                Ok(vec![ParsedTemplate {
                    filename: filename.to_string(),
                    template,
                }])
            },
            n => {
                let mut out = Vec::with_capacity(n);
                let mut seen_ids = std::collections::HashSet::with_capacity(n);
                for template in templates {
                    let id_str = template
                        .annotation("id")
                        .filter(|s| !s.is_empty())
                        .map(str::to_owned)
                        .ok_or_else(|| {
                            let (line, snippet) = Self::find_first_policy_without_id(content)
                                .unwrap_or((0, String::new()));
                            PolicyStoreError::CedarParsing {
                                file: filename.to_string(),
                                detail: CedarParseErrorDetail::MultiTemplateMissingExplicitId {
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
                            detail: CedarParseErrorDetail::DuplicateTemplateIdInFile { id: id_str },
                        });
                    }
                    let template = template.new_id(PolicyId::new(&id_str));
                    out.push(ParsedTemplate {
                        filename: filename.to_string(),
                        template,
                    });
                }
                Ok(out)
            },
        }
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
        // Cedar's PolicySet namespaces policies and templates together. Check
        // cross-file duplicates up front so the error names both offending files
        // instead of surfacing a generic Cedar "duplicate id" on add().
        let mut seen: HashMap<PolicyId, String> =
            HashMap::with_capacity(policies.len() + templates.len());
        let check = |id: &PolicyId,
                     filename: &str,
                     seen: &mut HashMap<PolicyId, String>|
         -> Result<(), PolicyStoreError> {
            if let Some(first) = seen.get(id) {
                if first != filename {
                    return Err(PolicyStoreError::CedarParsing {
                        file: filename.to_string(),
                        detail: CedarParseErrorDetail::DuplicatePolicyIdAcrossFiles {
                            id: id.to_string(),
                            first_file: first.clone(),
                            second_file: filename.to_string(),
                        },
                    });
                }
            } else {
                seen.insert(id.clone(), filename.to_string());
            }
            Ok(())
        };
        for parsed in &policies {
            check(&parsed.id, &parsed.filename, &mut seen)?;
        }
        for parsed in &templates {
            check(parsed.template.id(), &parsed.filename, &mut seen)?;
        }

        let mut policy_set = PolicySet::new();

        for parsed in policies {
            policy_set
                .add(parsed.policy)
                .map_err(|e| PolicyStoreError::CedarParsing {
                    file: parsed.filename,
                    detail: CedarParseErrorDetail::AddPolicyFailed(e.to_string()),
                })?;
        }

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
        // Prefer whichever opening quote appears first so a later `"` (e.g. in
        // `User::"alice"`) does not steal parsing from an earlier `'...'`.
        let dq = after_id.find('"');
        let sq = after_id.find('\'');
        let (open_quote, quote_char) = match (dq, sq) {
            (Some(i), Some(j)) => {
                if i < j {
                    (i, '"')
                } else {
                    (j, '\'')
                }
            },
            (Some(i), None) => (i, '"'),
            (None, Some(j)) => (j, '\''),
            (None, None) => return None,
        };
        let after_open = &after_id[open_quote + 1..];
        let close_quote = after_open.find(quote_char)?;
        Some(after_open[..close_quote].to_string())
    }

    /// Byte offset in `trimmed` of the first `permit` / `forbid` policy keyword,
    /// or `None` if this line does not contain a policy statement start.
    fn policy_keyword_byte_offset(trimmed: &str) -> Option<usize> {
        const KEYWORDS: [&str; 4] = ["permit(", "forbid(", "permit ", "forbid "];
        KEYWORDS.iter().filter_map(|kw| trimmed.find(kw)).min()
    }

    /// Scan source text for the first `permit` / `forbid` policy whose preceding
    /// lines (back to the start of the file or the previous policy) contain no
    /// `@id(...)` annotation. Returns `(1-based line, trimmed-and-truncated snippet)`.
    ///
    /// Treats a line as containing a policy start if any of `permit(` / `forbid(` /
    /// `permit ` / `forbid ` appears in the line after leading whitespace (e.g.
    /// `@id("p1") permit(...)` on one line). The snippet starts at that keyword.
    fn find_first_policy_without_id(content: &str) -> Option<(usize, String)> {
        const MAX_SNIPPET: usize = 80;
        let lines: Vec<&str> = content.lines().collect();
        let mut window_start = 0usize;
        for (idx, line) in lines.iter().enumerate() {
            let trimmed = line.trim_start();
            let Some(token_off) = Self::policy_keyword_byte_offset(trimmed) else {
                continue;
            };
            let has_id_on_prior_lines = lines[window_start..idx]
                .iter()
                .any(|l| Self::parse_id_from_line_trimmed(l.trim()).is_some());
            let has_id_on_same_line_before_kw = if token_off == 0 {
                false
            } else {
                Self::parse_id_from_line_trimmed(trimmed.get(..token_off).unwrap_or("").trim_end())
                    .is_some()
            };
            let has_id = has_id_on_prior_lines || has_id_on_same_line_before_kw;
            if !has_id {
                let mut snippet = trimmed[token_off..].trim_end().to_string();
                if snippet.chars().count() > MAX_SNIPPET {
                    snippet = snippet.chars().take(MAX_SNIPPET).collect::<String>() + "...";
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
        assert_eq!(parsed.len(), 1, "expected exactly one template");
        assert_eq!(parsed[0].filename, "template.cedar");
        // ID should be derived from filename - get from template directly
        assert_eq!(parsed[0].template.id().to_string(), "template");
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

        assert_eq!(
            policy_map.len(),
            2,
            "expected exactly two policy ids in policy_map (one per file)"
        );
        assert_eq!(
            policy_map
                .get(&PolicyId::new("policy1"))
                .map(String::as_str),
            Some("policy1.cedar"),
            "first file should contribute id policy1 mapped to policy1.cedar"
        );
        assert_eq!(
            policy_map
                .get(&PolicyId::new("policy2"))
                .map(String::as_str),
            Some("policy2.cedar"),
            "second file should contribute id policy2 mapped to policy2.cedar"
        );
        assert!(
            !policy_map.contains_key(&PolicyId::new("missing")),
            "policy_map must not contain a synthetic id that was never parsed"
        );
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
        assert_eq!(
            id,
            Some("another-policy-id".to_string()),
            "single-quoted @id in a comment line should be extracted as the policy id"
        );
    }

    #[test]
    fn test_extract_id_annotation_prefers_earlier_opening_quote() {
        // A later ASCII `"` (e.g. in `User::"alice"`) must not be chosen as the
        // opening delimiter when `@id('...')` appears first on the same logical line.
        let policy_text =
            r#"@id('quoted-id') permit(principal == User::"alice", action, resource);"#;

        let id = PolicyParser::extract_id_annotation(policy_text);
        assert_eq!(
            id,
            Some("quoted-id".to_string()),
            "the opening quote for @id must be the earliest of ' or \", matching Cedar-style strings on the line"
        );
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
        assert_eq!(
            policy_set.policies().count(),
            2,
            "permit and forbid from the combined text should both be present in the PolicySet"
        );
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
        assert_eq!(parsed_template.len(), 1, "expected exactly one template");
        assert_eq!(parsed_template[0].template.id().to_string(), "template");

        let policy_set = PolicyParser::create_policy_set(parsed_policy, parsed_template)
            .expect("create_policy_set should succeed with one policy and one template");
        assert_eq!(
            policy_set.policies().count(),
            1,
            "create_policy_set should retain exactly one non-template policy"
        );
        assert_eq!(
            policy_set.templates().count(),
            1,
            "create_policy_set should retain exactly one template"
        );
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
    fn test_create_policy_set_rejects_duplicate_id_across_files() {
        // Two files, each single-policy, both with @id("same") — the cross-file
        // duplicate must be caught with names of both files before Cedar's add().
        let p1 = PolicyParser::parse_policy(
            r#"@id("same") permit(principal, action, resource);"#,
            "file-a.cedar",
        )
        .expect("file-a should parse");
        let p2 = PolicyParser::parse_policy(
            r#"@id("same") forbid(principal, action, resource);"#,
            "file-b.cedar",
        )
        .expect("file-b should parse");

        let mut all = p1;
        all.extend(p2);

        let err = PolicyParser::create_policy_set(all, vec![])
            .expect_err("duplicate id across files should fail");
        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing {
                    detail: CedarParseErrorDetail::DuplicatePolicyIdAcrossFiles {
                        id, first_file, second_file
                    }, ..
                } if id == "same" && first_file == "file-a.cedar" && second_file == "file-b.cedar"
            ),
            "expected DuplicatePolicyIdAcrossFiles with both filenames, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_multi_template_file_success() {
        let template_text = r#"
@id("principal-view")
permit(
    principal == ?principal,
    action == Action::"view",
    resource
);

@id("resource-edit")
permit(
    principal,
    action == Action::"edit",
    resource == ?resource
);
        "#;

        let parsed = PolicyParser::parse_template(template_text, "templates.cedar")
            .expect("multi-template file with @id each should parse");
        assert_eq!(parsed.len(), 2, "expected two templates");
        let ids: Vec<String> = parsed.iter().map(|t| t.template.id().to_string()).collect();
        assert!(
            ids.contains(&"principal-view".to_string())
                && ids.contains(&"resource-edit".to_string()),
            "expected both template ids, got {ids:?}"
        );
    }

    #[test]
    fn test_parse_multi_template_file_duplicate_id() {
        let template_text = r#"
@id("dup")
permit(principal == ?principal, action, resource);

@id("dup")
permit(principal == ?principal, action, resource);
        "#;

        let err = PolicyParser::parse_template(template_text, "dup-templates.cedar")
            .expect_err("duplicate @id in one template file should fail");

        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing {
                    file,
                    detail: CedarParseErrorDetail::DuplicateTemplateIdInFile { id }
                } if file == "dup-templates.cedar" && id == "dup"
            ),
            "expected DuplicateTemplateIdInFile for id dup, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_multi_template_file_missing_explicit_id() {
        let template_text = r#"
@id("with-id")
permit(principal == ?principal, action, resource);

permit(principal == ?principal, action, resource);
        "#;

        let err = PolicyParser::parse_template(template_text, "missing-id-templates.cedar")
            .expect_err("second template without @id should fail");

        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing {
                    file,
                    detail: CedarParseErrorDetail::MultiTemplateMissingExplicitId { line, snippet }
                } if file == "missing-id-templates.cedar"
                    && *line > 0
                    && (snippet.starts_with("permit(") || snippet.starts_with("permit "))
            ),
            "expected MultiTemplateMissingExplicitId with line and permit snippet, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_template_file_rejects_embedded_policy() {
        // A template file that also contains a non-template policy.
        let mixed_text = r#"
@id("tpl")
permit(
    principal == ?principal,
    action == Action::"view",
    resource
);

@id("plain")
permit(
    principal,
    action == Action::"view",
    resource
);
        "#;

        let err = PolicyParser::parse_template(mixed_text, "mixed-t.cedar")
            .expect_err("template file containing a non-template policy should be rejected");
        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing {
                    file,
                    detail: CedarParseErrorDetail::PoliciesInTemplateFile { count }
                } if file == "mixed-t.cedar" && *count == 1
            ),
            "expected PoliciesInTemplateFile, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_policy_file_rejects_embedded_template() {
        // A policy file that also contains a template (uses `?principal` slot).
        // Cedar would parse the template into `policy_set.templates()`, so without an
        // explicit check it would be silently dropped.
        let policy_text = r#"
@id("allow-read")
permit(
    principal,
    action == Action::"read",
    resource
);

@id("tpl-access")
permit(
    principal == ?principal,
    action == Action::"access",
    resource
);
        "#;

        let err = PolicyParser::parse_policy(policy_text, "mixed.cedar")
            .expect_err("policy file containing a template should be rejected");

        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing {
                    file,
                    detail: CedarParseErrorDetail::TemplatesInPolicyFile { count }
                } if file == "mixed.cedar" && *count == 1
            ),
            "expected TemplatesInPolicyFile, got: {err:?}"
        );
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

    #[test]
    fn test_parse_multi_policy_file_missing_explicit_id_after_inline_first_policy() {
        let policy_text = r#"
@id("first") permit(principal, action, resource);

permit(principal, action, resource);
        "#;

        let err = PolicyParser::parse_policy(policy_text, "inline-then-bare.cedar")
            .expect_err("second policy on its own line without @id should fail");

        assert!(
            matches!(
                &err,
                PolicyStoreError::CedarParsing {
                    file,
                    detail: CedarParseErrorDetail::MultiPolicyMissingExplicitId { line, snippet }
                } if file == "inline-then-bare.cedar"
                    && *line > 0
                    && (snippet.starts_with("permit(") || snippet.starts_with("permit "))
            ),
            "expected MultiPolicyMissingExplicitId after inline @id+permit line, got: {err:?}"
        );
    }
}
