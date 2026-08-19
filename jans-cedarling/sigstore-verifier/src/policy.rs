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
    /// NOT ANCHORED: the pattern may match anywhere in the SAN, so `evil\.com`
    /// **does** match `not-evil.com.attacker.io`. Anchor it yourself —
    /// `^evil\.com$` — whenever the whole SAN is what you mean. This follows
    /// `sigstore-go`, whose `SubjectAlternativeNameMatcher` carries the same
    /// caveat ("regexp matching is not anchored by default; use `^...$` if you
    /// intend to match the entire SAN value") and cosign's
    /// `CheckCertificatePolicy`, which likewise matches unanchored. Matching
    /// their semantics keeps a pattern's meaning the same as it moves between
    /// this crate and the tools that produced the bundles.
    ///
    /// An invalid pattern never matches: it fails to compile and the SAN is
    /// rejected, rather than being treated as a wildcard.
    Regex(String),
    /// Same matching semantics as [`IdentityMatch::Regex`] — including being
    /// unanchored — but holding an already-compiled pattern instead of
    /// recompiling it on every [`VerificationPolicy::verify`] call.
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
            // An uncompilable pattern rejects rather than matching anything.
            IdentityMatch::Regex(pattern) => regex_lite::Regex::new(pattern)
                .is_ok_and(|re| re.is_match(san)),
            IdentityMatch::CompiledRegex(re) => re.is_match(san),
        }
    }
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
    fn compiled_regex_match_passes_and_rejects_a_mismatch() {
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
    fn regex_is_unanchored_and_the_caller_anchors() {
        // Matches sigstore-go and cosign: the pattern may match anywhere in
        // the SAN, and a caller who means the whole value writes the anchors.
        // Pinned in both directions so the semantics can't drift silently —
        // a bare `evil\.com` here is a substring match, exactly as it is in
        // the tools that produced the bundle.
        let policy = |pattern: &str| VerificationPolicy {
            cert_identity: IdentityMatch::Regex(pattern.into()),
            cert_issuer: "https://example.com".into(),
        };
        policy(r"evil\.com")
            .verify(
                &["not-evil.com.attacker.io".into()],
                Some("https://example.com"),
            )
            .expect("an unanchored pattern matches a substring, as upstream does");
        policy(r"^evil\.com$")
            .verify(
                &["not-evil.com.attacker.io".into()],
                Some("https://example.com"),
            )
            .expect_err("anchoring the pattern is what restricts it to the whole SAN");
        policy(r"^evil\.com$")
            .verify(&["evil.com".into()], Some("https://example.com"))
            .expect("an anchored pattern still matches the exact value");
    }

    #[test]
    fn compiled_regex_is_unanchored_too() {
        // The caller compiles this one, so its semantics must not differ from
        // the string variant.
        let re = regex_lite::Regex::new(r"evil\.com").expect("valid pattern");
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::CompiledRegex(re),
            cert_issuer: "https://example.com".into(),
        };
        policy
            .verify(
                &["not-evil.com.attacker.io".into()],
                Some("https://example.com"),
            )
            .expect("a caller-compiled pattern matches on the same terms as a string one");
    }

    #[test]
    fn regex_that_does_not_compile_matches_nothing() {
        // An unparseable pattern must reject, not degrade into a wildcard.
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
