// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{collections::HashMap, sync::Arc};

use crate::{
    LogLevel,
    common::{issuer_utils::IssClaim, policy_store::TrustedIssuer},
    log::{BaseLogEntry, LogEntry, LogWriter, Logger},
};

/// Fast lookup index for trusted issuers by `iss` claim.
///
/// `iss` claim semantics vary across `IdPs`:
/// - origin only (Google: `https://accounts.google.com`),
/// - origin + path (Microsoft tenant: `https://login.microsoftonline.com/{tenant}/v2.0`),
/// - origin with trailing slash (Auth0).
///
/// To cover all cases we maintain two indexes — origin and full configured
/// URL — and try both on lookup. Both are keyed by the canonical
/// [`IssClaim`] form so trailing-slash / case differences cannot cause
/// false negatives.
///
/// # Origin Collision Handling
///
/// When multiple issuers share the same origin, the last issuer encountered
/// during construction wins for origin-based lookups; a warning is logged.
/// Full-URL lookups remain unaffected.
#[derive(Debug, Clone)]
pub(crate) struct TrustedIssuerIndex {
    url_index: HashMap<IssClaim, Arc<TrustedIssuer>>,
    origin_index: HashMap<IssClaim, Arc<TrustedIssuer>>,
}

impl TrustedIssuerIndex {
    pub(crate) fn new(issuers: &HashMap<String, TrustedIssuer>, logger: Option<&Logger>) -> Self {
        let mut origin_index: HashMap<IssClaim, Arc<TrustedIssuer>> = HashMap::new();
        let mut url_index: HashMap<IssClaim, Arc<TrustedIssuer>> = HashMap::new();

        for iss in issuers.values() {
            let origin = iss.iss_claim();
            let full_url = IssClaim::new(iss.get_oidc_endpoint().as_str());
            let issuer = Arc::new(iss.clone());

            if let Some(existing) = origin_index.get(&origin) {
                logger.log_any(
                    LogEntry::new(BaseLogEntry::new_system_opt_request_id(LogLevel::WARN, None))
                        .set_message(format!(
                            "Duplicate origin '{}': issuer '{}' will override existing issuer '{}' for origin-based lookups",
                            origin, iss.name, existing.name,
                        )),
                );
            }
            origin_index.insert(origin, issuer.clone());
            url_index.insert(full_url, issuer);
        }

        Self {
            url_index,
            origin_index,
        }
    }

    /// Finds a trusted issuer by canonical [`IssClaim`].
    pub(super) fn find(&self, iss: &IssClaim) -> Option<&Arc<TrustedIssuer>> {
        self.origin_index
            .get(iss)
            .or_else(|| self.url_index.get(iss))
    }

    /// Returns an iterator over references to all trusted issuers.
    pub(super) fn values(&self) -> impl Iterator<Item = &TrustedIssuer> + '_ {
        self.url_index.values().map(AsRef::as_ref)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::TrustedIssuer;
    use std::collections::HashMap;
    use url::Url;

    fn create_issuer_index() -> TrustedIssuerIndex {
        let mut trusted_issuers = HashMap::new();

        let issuer_one = TrustedIssuer::new(
            "Issuer One".to_string(),
            "Issuer One".to_string(),
            Url::parse("https://issuer1.example.com/auth").unwrap(),
            HashMap::new(),
        );
        trusted_issuers.insert("issuer_one".to_string(), issuer_one);

        let jans = TrustedIssuer::new(
            "Jans".to_string(),
            "Janssen".to_string(),
            Url::parse("https://account.gluu.org/.well-known/openid-configuration").unwrap(),
            HashMap::new(),
        );
        trusted_issuers.insert("jans".to_string(), jans);

        let microsoft_issuer = TrustedIssuer::new(
            "Microsoft".to_string(),
            "Microsoft Azure AD".to_string(),
            Url::parse("https://login.microsoftonline.com/tenant").unwrap(),
            HashMap::new(),
        );
        trusted_issuers.insert("microsoft".to_string(), microsoft_issuer);

        let company_issuer = TrustedIssuer::new(
            "Company".to_string(),
            "Company Internal Auth".to_string(),
            Url::parse("https://auth.company.internal:8443/oauth").unwrap(),
            HashMap::new(),
        );
        trusted_issuers.insert("company".to_string(), company_issuer);

        TrustedIssuerIndex::new(&trusted_issuers, None)
    }

    #[test]
    fn test_empty_issuer_index() {
        let issuer_index = TrustedIssuerIndex::new(&HashMap::new(), None);
        assert!(
            issuer_index
                .find(&IssClaim::new("https://login.microsoftonline.com/tenant"))
                .is_none()
        );
    }

    #[test]
    fn test_find_non_existing_issuer() {
        let issuer_index = create_issuer_index();
        assert!(
            issuer_index
                .find(&IssClaim::new("https://account.google.com"))
                .is_none()
        );
    }

    #[test]
    fn test_find_by_full_url() {
        let issuer_index = create_issuer_index();
        let issuer = issuer_index
            .find(&IssClaim::new("https://login.microsoftonline.com/tenant"))
            .expect("Should find issuer by full URL");

        assert_eq!(issuer.name, "Microsoft");
        assert_eq!(
            issuer.get_oidc_endpoint().as_str(),
            "https://login.microsoftonline.com/tenant"
        );
    }

    #[test]
    fn test_find_by_origin() {
        let issuer_index = create_issuer_index();
        let issuer = issuer_index
            .find(&IssClaim::new("https://account.gluu.org"))
            .expect("Should find issuer by origin");

        assert_eq!(issuer.name, "Jans");
        assert_eq!(
            issuer.get_oidc_endpoint().as_str(),
            "https://account.gluu.org/.well-known/openid-configuration"
        );
    }

    #[test]
    fn test_find_by_origin_with_trailing_slash() {
        // Auth0-style: `iss` claim emitted as `https://issuer/` (with slash).
        // Index keys come from `Url::origin().ascii_serialization()` (no slash).
        // `find` must canonicalize the input so lookup still succeeds.
        let issuer_index = create_issuer_index();
        let issuer = issuer_index
            .find(&IssClaim::new("https://account.gluu.org/"))
            .expect("Should find issuer by origin with trailing slash");

        assert_eq!(
            issuer.name,
            "Jans",
            "lookup-by-origin-with-trailing-slash should normalize the trailing slash and resolve to the 'Jans' issuer"
        );
    }

    #[test]
    fn test_find_by_origin_with_port() {
        let issuer_index = create_issuer_index();
        let issuer = issuer_index
            .find(&IssClaim::new("https://auth.company.internal:8443"))
            .expect("Should find issuer by origin with port");

        assert_eq!(issuer.name, "Company");
        assert_eq!(
            issuer.get_oidc_endpoint().as_str(),
            "https://auth.company.internal:8443/oauth"
        );
    }
}
