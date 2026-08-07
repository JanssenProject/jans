// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Verification policy for Sigstore blob verification.
//!
//! The policy specifies **whom to trust** — the required certificate identity
//! (SAN) and OIDC issuer. Both are mandatory.

/// The verification policy specifying whom to trust.
///
/// Identity is MANDATORY. You must always specify whom you trust.
/// No `Option` — both `cert_identity` and `cert_issuer` are required.
#[derive(Debug, Clone)]
pub struct VerificationPolicy {
    /// How to match the certificate's Subject Alternative Name.
    pub cert_identity: IdentityMatch,

    /// The required OIDC issuer (exact match against OID 1.3.6.1.4.1.57264.1.8).
    pub cert_issuer: String,
}

/// How to match the certificate identity (SAN).
#[derive(Debug, Clone)]
pub enum IdentityMatch {
    /// Exact string match against the SAN value.
    Exact(String),
    /// Regex match against the SAN value, compiled fresh on every
    /// [`VerificationPolicy::verify`] call.
    ///
    /// Prefer [`IdentityMatch::CompiledRegex`] if the same policy verifies
    /// many bundles — this variant recompiles `pattern` from scratch each
    /// time, which is wasted work when the pattern doesn't change between
    /// calls.
    ///
    /// AUTO-ANCHORED: the pattern must match the *entire* SAN value, not a
    /// substring (e.g., `evil.com` won't match `not-evil.com.attacker.io`).
    /// This is enforced by checking that the match span covers the whole
    /// string — not by wrapping the pattern text in `\A(?:pattern)\z`, which
    /// would let a pattern with an unbalanced top-level `)` or `|` (e.g.
    /// `)|(?:.*`) escape the wrapping group and defeat the anchor.
    Regex(String),
    /// Same matching semantics as [`IdentityMatch::Regex`], but holding an
    /// already-compiled pattern instead of recompiling it on every
    /// [`VerificationPolicy::verify`] call.
    ///
    /// `regex_lite::Regex` clones cheaply (it's `Arc`-backed internally), so
    /// a caller that verifies many bundles against the same policy should
    /// compile the pattern once — e.g. in a `LazyLock`/`OnceLock`, or at
    /// startup — and reuse it, rather than constructing a fresh
    /// `VerificationPolicy` with `IdentityMatch::Regex(String)` per call.
    CompiledRegex(regex_lite::Regex),
}

impl VerificationPolicy {
    /// Check that the given SAN and issuer match this policy.
    ///
    /// Returns the SAN that matched.
    ///
    /// # Errors
    ///
    /// Returns [`PolicyViolation`](crate::SigstoreVerificationError) if the issuer is missing,
    /// doesn't match, or no SAN matches the identity pattern.
    pub fn verify(
        &self,
        sans: &[String],
        cert_issuer: Option<&str>,
    ) -> Result<String, crate::error::SigstoreVerificationError> {
        // Check issuer
        let issuer = cert_issuer.ok_or_else(|| {
            crate::error::SigstoreVerificationError::PolicyViolation {
                reason: "certificate does not contain an OIDC issuer extension".into(),
            }
        })?;

        if issuer != self.cert_issuer {
            return Err(crate::error::SigstoreVerificationError::PolicyViolation {
                reason: format!(
                    "issuer mismatch: expected '{}', got '{}'",
                    self.cert_issuer, issuer
                ),
            });
        }

        // Check identity — return the SAN that matched.
        let matched = sans.iter().find(|san| self.identity_match(san));
        match matched {
            Some(san) => Ok(san.clone()),
            None => Err(crate::error::SigstoreVerificationError::PolicyViolation {
                reason: format!(
                    "identity mismatch: none of {} SAN(s) matched the policy",
                    sans.len()
                ),
            }),
        }
    }

    /// Test whether a single SAN value matches the policy identity.
    fn identity_match(&self, san: &str) -> bool {
        match &self.cert_identity {
            IdentityMatch::Exact(pattern) => san == pattern,
            IdentityMatch::Regex(pattern) => {
                let Ok(re) = regex_lite::Regex::new(pattern) else {
                    return false;
                };
                full_match(&re, san)
            },
            IdentityMatch::CompiledRegex(re) => full_match(re, san),
        }
    }
}

/// Whether `re` matches the *entire* `san`, not just a substring of it.
///
/// Enforced by checking the match span rather than by wrapping the pattern
/// text in `\A(?:pattern)\z` — see the doc comment on `IdentityMatch::Regex`
/// for why that string-concatenation approach is unsafe.
fn full_match(re: &regex_lite::Regex, san: &str) -> bool {
    re.find(san)
        .is_some_and(|m| m.start() == 0 && m.end() == san.len())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn exact_match_passes() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        policy
            .verify(
                &["https://github.com/example".into()],
                Some("https://token.actions.githubusercontent.com"),
            )
            .expect("exact match on SAN and issuer must pass");
    }

    #[test]
    fn exact_match_wrong_san_rejected() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        policy
            .verify(
                &["https://github.com/other".into()],
                Some("https://token.actions.githubusercontent.com"),
            )
            .expect_err("wrong SAN must be rejected");
    }

    #[test]
    fn exact_match_wrong_issuer_rejected() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        policy
            .verify(
                &["https://github.com/example".into()],
                Some("https://accounts.google.com"),
            )
            .expect_err("wrong issuer must be rejected");
    }

    #[test]
    fn regex_match_passes() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Regex(r"https://github\.com/slsa-framework/.*".into()),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        policy
            .verify(
                &["https://github.com/slsa-framework/slsa-github-generator".into()],
                Some("https://token.actions.githubusercontent.com"),
            )
            .expect("regex match on SAN must pass");
    }

    #[test]
    fn compiled_regex_match_passes_and_still_anchors() {
        // Same semantics as IdentityMatch::Regex, but the caller compiles
        // once and reuses the Regex across many verify() calls instead of
        // paying the compile cost on every one.
        let re = regex_lite::Regex::new(r"https://github\.com/slsa-framework/.*")
            .expect("valid pattern");
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::CompiledRegex(re.clone()),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        policy
            .verify(
                &["https://github.com/slsa-framework/slsa-github-generator".into()],
                Some("https://token.actions.githubusercontent.com"),
            )
            .expect("compiled regex match on SAN must pass");

        // Same compiled Regex, reused for a second policy — cloning it is
        // cheap (Arc-backed), unlike recompiling from a pattern string.
        let policy_wrong_san = VerificationPolicy {
            cert_identity: IdentityMatch::CompiledRegex(re),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        policy_wrong_san
            .verify(
                &["https://github.com/other-org/other-repo".into()],
                Some("https://token.actions.githubusercontent.com"),
            )
            .expect_err("compiled regex must still reject a non-matching SAN");
    }

    #[test]
    fn regex_anchored_prevents_partial_match() {
        // "evil.com" should NOT match "not-evil.com.attacker.io"
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Regex("evil\\.com".into()),
            cert_issuer: "https://example.com".into(),
        };
        policy
            .verify(
                &["not-evil.com.attacker.io".into()],
                Some("https://example.com"),
            )
            .expect_err("partial regex match must be prevented by anchoring");
    }

    #[test]
    fn regex_with_unbalanced_paren_does_not_defeat_anchoring() {
        // Regression: the old implementation anchored by string-wrapping
        // the pattern as `\A(?:pattern)\z`. A pattern like `)|(?:.*` would
        // close the wrapping group early and open a new top-level
        // alternative, producing `\A(?:)|(?:.*)\z` — whose first branch
        // matches (empty, at the start) against ANY string, defeating the
        // anchor entirely. The span-based full-match check can't be
        // escaped this way: `pattern` is compiled as-is, never concatenated
        // into a larger regex string.
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Regex(")|(?:.*".into()),
            cert_issuer: "https://example.com".into(),
        };
        policy
            .verify(&["anything at all".into()], Some("https://example.com"))
            .expect_err("an unbalanced-paren pattern must not match everything");
    }

    #[test]
    fn missing_issuer_extension_rejected() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://example.com".into()),
            cert_issuer: "https://example.com".into(),
        };
        policy
            .verify(&["https://example.com".into()], None)
            .expect_err("missing issuer extension must be rejected");
    }

    #[test]
    fn empty_sans_rejected() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://example.com".into()),
            cert_issuer: "https://example.com".into(),
        };
        policy
            .verify(&[], Some("https://example.com"))
            .expect_err("policy with empty SANs must be rejected");
    }

    #[test]
    fn verify_returns_the_matched_san() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        let matched = policy
            .verify(
                &[
                    "mail@example.com".into(),
                    "https://github.com/example".into(),
                ],
                Some("https://token.actions.githubusercontent.com"),
            )
            .expect("policy must match the second SAN");
        assert_eq!(
            matched, "https://github.com/example",
            "must return the SAN that matched, not the first SAN"
        );
    }
}
