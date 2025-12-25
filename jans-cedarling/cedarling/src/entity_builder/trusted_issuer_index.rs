// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{collections::HashMap, sync::Arc};

use crate::{
    LogLevel,
    common::policy_store::TrustedIssuer,
    log::{LogEntry, LogType, LogWriter, Logger},
};

/// Fast lookup index for trusted issuers by URL or origin.
///
/// This structure maintains indexes for O(1) lookups of trusted issuers by either:
/// - Full OIDC endpoint URL
/// - Origin only
///
/// # Origin Collision Handling
///
/// When multiple issuers share the same origin, the last issuer encountered during construction
/// will be used for origin-based lookups. A warning is logged when this occurs. Full URL lookups
/// remain unaffected and will correctly resolve to each specific issuer.
#[derive(Debug, Clone)]
pub struct TrustedIssuerIndex {
    /// Index mapping full URL to trusted issuer
    url_index: HashMap<String, Arc<TrustedIssuer>>,
    /// Index mapping origin to trusted issuer
    origin_index: HashMap<String, Arc<TrustedIssuer>>,
}

impl TrustedIssuerIndex {
    pub fn new(issuers: &HashMap<String, TrustedIssuer>, logger: Option<Logger>) -> Self {
        let mut origin_index: HashMap<String, Arc<TrustedIssuer>> = HashMap::new();
        let mut url_index = HashMap::new();

        for iss in issuers.values() {
            let origin = iss.oidc_endpoint.origin().ascii_serialization();
            let full_url = iss.oidc_endpoint.as_str();
            let issuer = Arc::new(iss.clone());

            if let Some(existing) = origin_index.get(&origin) {
                logger.log_any(
                    LogEntry::new_with_data(LogType::System, None)
                        .set_level(LogLevel::WARN)
                        .set_message(format!(
                            "Duplicate origin '{}': issuer '{}' will override existing issuer '{}' for origin-based lookups",
                            origin,
                            iss.name,
                            existing.name,
                        )),
                );
            }
            origin_index.insert(origin, issuer.clone());
            url_index.insert(full_url.to_string(), issuer);
        }

        Self {
            url_index,
            origin_index,
        }
    }

    /// Finds a trusted issuer by URL, checking full URL first, then origin.
    pub fn find(&self, url: &str) -> Option<&Arc<TrustedIssuer>> {
        self.url_index
            .get(url)
            .or_else(|| self.origin_index.get(url))
    }

    /// Returns an iterator over references to all trusted issuers.
    pub fn values(&self) -> impl Iterator<Item = &TrustedIssuer> + '_ {
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

        let issuer_one = TrustedIssuer {
            name: "Issuer One".to_string(),
            description: "Issuer One".to_string(),
            oidc_endpoint: Url::parse("https://issuer1.example.com/auth").unwrap(),
            token_metadata: HashMap::new(),
        };
        trusted_issuers.insert("issuer_one".to_string(), issuer_one);

        let jans = TrustedIssuer {
            name: "Jans".to_string(),
            description: "Janssen".to_string(),
            oidc_endpoint: Url::parse("https://account.gluu.org/.well-known/openid-configuration")
                .unwrap(),
            token_metadata: HashMap::new(),
        };
        trusted_issuers.insert("jans".to_string(), jans);

        let microsoft_issuer = TrustedIssuer {
            name: "Microsoft".to_string(),
            description: "Microsoft Azure AD".to_string(),
            oidc_endpoint: Url::parse("https://login.microsoftonline.com/tenant").unwrap(),
            token_metadata: HashMap::new(),
        };
        trusted_issuers.insert("microsoft".to_string(), microsoft_issuer);

        let company_issuer = TrustedIssuer {
            name: "Company".to_string(),
            description: "Company Internal Auth".to_string(),
            oidc_endpoint: Url::parse("https://auth.company.internal:8443/oauth").unwrap(),
            token_metadata: HashMap::new(),
        };
        trusted_issuers.insert("company".to_string(), company_issuer);

        TrustedIssuerIndex::new(&trusted_issuers, None)
    }

    #[test]
    fn test_empty_issuer_index() {
        let issuer_index = TrustedIssuerIndex::new(&HashMap::new(), None);
        assert!(
            issuer_index
                .find("https://login.microsoftonline.com/tenant")
                .is_none()
        );
    }

    #[test]
    fn test_find_non_existing_issuer() {
        let issuer_index = create_issuer_index();
        assert!(issuer_index.find("https://account.google.com").is_none());
    }

    #[test]
    fn test_find_by_full_url() {
        let issuer_index = create_issuer_index();
        let issuer = issuer_index
            .find("https://login.microsoftonline.com/tenant")
            .expect("Should find issuer by full URL");

        assert_eq!(issuer.name, "Microsoft");
        assert_eq!(
            issuer.oidc_endpoint.as_str(),
            "https://login.microsoftonline.com/tenant"
        );
    }

    #[test]
    fn test_find_by_origin() {
        let issuer_index = create_issuer_index();
        let issuer = issuer_index
            .find("https://account.gluu.org")
            .expect("Should find issuer by origin");

        assert_eq!(issuer.name, "Jans");
        assert_eq!(
            issuer.oidc_endpoint.as_str(),
            "https://account.gluu.org/.well-known/openid-configuration"
        );
    }

    #[test]
    fn test_find_by_origin_with_port() {
        let issuer_index = create_issuer_index();
        let issuer = issuer_index
            .find("https://auth.company.internal:8443")
            .expect("Should find issuer by origin with port");

        assert_eq!(issuer.name, "Company");
        assert_eq!(
            issuer.oidc_endpoint.as_str(),
            "https://auth.company.internal:8443/oauth"
        );
    }
}
