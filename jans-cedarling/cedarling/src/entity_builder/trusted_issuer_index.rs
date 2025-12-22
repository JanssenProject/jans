// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{collections::HashMap, sync::Arc};

use crate::common::policy_store::TrustedIssuer;

#[derive(Debug, Clone)]
pub struct TrustedIssuerIndex {
    issuers: HashMap<String, Arc<TrustedIssuer>>,
    url_index: HashMap<String, String>,
    origin_index: HashMap<String, String>,
}

impl TrustedIssuerIndex {
    pub fn new(issuers: &HashMap<String, TrustedIssuer>) -> Self {
        let mut issuers_arc = HashMap::new();
        let mut origin_index = HashMap::new();
        let mut url_index = HashMap::new();

        for (key, issuer) in issuers {
            let origin = issuer.oidc_endpoint.origin().ascii_serialization();
            let full_url = issuer.oidc_endpoint.as_str();

            issuers_arc.insert(key.clone(), Arc::new(issuer.clone()));
            origin_index.insert(origin, key.clone());
            url_index.insert(full_url.to_string(), key.clone());
        }

        Self {
            issuers: issuers_arc,
            url_index,
            origin_index,
        }
    }

    /// Finds a trusted issuer by URL, checking full URL first, then origin.
    pub fn find_by_url(&self, url: &str) -> Option<&Arc<TrustedIssuer>> {
        self.url_index
            .get(url)
            .or_else(|| self.origin_index.get(url))
            .and_then(|key| self.issuers.get(key))
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

        TrustedIssuerIndex::new(&trusted_issuers)
    }

    #[test]
    fn test_empty_issuer_index() {
        let issuer_index = TrustedIssuerIndex::new(&HashMap::new());
        assert!(
            issuer_index
                .find_by_url("https://login.microsoftonline.com/tenant")
                .is_none()
        );
    }

    #[test]
    fn test_find_non_existing_issuer() {
        let issuer_index = create_issuer_index();
        assert!(
            issuer_index
                .find_by_url("https://account.google.com")
                .is_none()
        );
    }

    #[test]
    fn test_find_by_full_url() {
        let issuer_index = create_issuer_index();
        let issuer = issuer_index
            .find_by_url("https://login.microsoftonline.com/tenant")
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
            .find_by_url("https://account.gluu.org")
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
            .find_by_url("https://auth.company.internal:8443")
            .expect("Should find issuer by origin with port");

        assert_eq!(issuer.name, "Company");
        assert_eq!(
            issuer.oidc_endpoint.as_str(),
            "https://auth.company.internal:8443/oauth"
        );
    }
}
