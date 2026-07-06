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
    /// Regex match against the SAN value.
    ///
    /// AUTO-ANCHORED: internally wrapped to `\A(?:pattern)\z`
    /// to prevent partial-match attacks (e.g., `evil.com` won't match
    /// `not-evil.com.attacker.io`).
    Regex(String),
}

impl VerificationPolicy {
    /// Check that the given SAN and issuer match this policy.
    pub fn verify(
        &self,
        sans: &[String],
        cert_issuer: Option<&str>,
    ) -> Result<(), crate::error::SigstoreVerificationError> {
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

        // Check identity
        let matched = sans.iter().any(|san| self.identity_match(san));
        if !matched {
            return Err(crate::error::SigstoreVerificationError::PolicyViolation {
                reason: format!(
                    "identity mismatch: no SAN matched the policy. SANs: {sans:?}"
                ),
            });
        }

        Ok(())
    }

    /// Test whether a single SAN value matches the policy identity.
    fn identity_match(&self, san: &str) -> bool {
        match &self.cert_identity {
            IdentityMatch::Exact(pattern) => san == pattern,
            IdentityMatch::Regex(pattern) => {
                // Auto-anchor the regex to prevent partial-match attacks.
                // `evil.com` should NOT match `not-evil.com.attacker.io`.
                let anchored = format!("\\A(?:{pattern})\\z");
                regex_lite::Regex::new(&anchored).is_ok_and(|re| re.is_match(san))
            }
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
        assert!(policy
            .verify(
                &["https://github.com/example".into()],
                Some("https://token.actions.githubusercontent.com")
            )
            .is_ok());
    }

    #[test]
    fn exact_match_wrong_san_rejected() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        assert!(policy
            .verify(
                &["https://github.com/other".into()],
                Some("https://token.actions.githubusercontent.com")
            )
            .is_err());
    }

    #[test]
    fn exact_match_wrong_issuer_rejected() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        assert!(policy
            .verify(
                &["https://github.com/example".into()],
                Some("https://accounts.google.com")
            )
            .is_err());
    }

    #[test]
    fn regex_match_passes() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Regex(
                r"https://github\.com/slsa-framework/.*".into(),
            ),
            cert_issuer: "https://token.actions.githubusercontent.com".into(),
        };
        assert!(policy
            .verify(
                &["https://github.com/slsa-framework/slsa-github-generator".into()],
                Some("https://token.actions.githubusercontent.com")
            )
            .is_ok());
    }

    #[test]
    fn regex_anchored_prevents_partial_match() {
        // "evil.com" should NOT match "not-evil.com.attacker.io"
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Regex("evil\\.com".into()),
            cert_issuer: "https://example.com".into(),
        };
        assert!(policy
            .verify(
                &["not-evil.com.attacker.io".into()],
                Some("https://example.com")
            )
            .is_err());
    }

    #[test]
    fn missing_issuer_extension_rejected() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://example.com".into()),
            cert_issuer: "https://example.com".into(),
        };
        assert!(policy
            .verify(&["https://example.com".into()], None)
            .is_err());
    }

    #[test]
    fn empty_sans_rejected() {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact("https://example.com".into()),
            cert_issuer: "https://example.com".into(),
        };
        assert!(policy
            .verify(&[], Some("https://example.com"))
            .is_err());
    }
}
