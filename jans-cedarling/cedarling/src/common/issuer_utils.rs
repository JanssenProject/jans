// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt;

use serde::Serialize;
use serde::de::{self, Deserialize, Deserializer, Visitor};

/// Utility structure that holds a normalized issuer string
#[derive(Debug, Clone, PartialEq, Eq, Hash, derive_more::Display)]
pub(crate) struct IssClaim(String);

impl IssClaim {
    /// Create a new Issuer with normalized value
    pub(crate) fn new(issuer: &str) -> Self {
        Self(normalize_issuer(issuer))
    }

    /// Get the issuer as a &str
    pub(crate) fn as_str(&self) -> &str {
        &self.0
    }
}

impl<'de> Deserialize<'de> for IssClaim {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        struct IssClaimVisitor;

        impl Visitor<'_> for IssClaimVisitor {
            type Value = IssClaim;

            fn expecting(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
                formatter.write_str("a string representing an issuer claim")
            }

            fn visit_str<E>(self, value: &str) -> Result<Self::Value, E>
            where
                E: de::Error,
            {
                Ok(IssClaim::new(value))
            }

            fn visit_string<E>(self, value: String) -> Result<Self::Value, E>
            where
                E: de::Error,
            {
                Ok(IssClaim::new(&value))
            }
        }

        deserializer.deserialize_string(IssClaimVisitor)
    }
}

impl Serialize for IssClaim {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        serializer.serialize_str(&self.0)
    }
}

/// Normalizes an issuer URL by removing trailing slashes
/// This handles cases where IDPs like Auth0 return issuers with trailing slashes
/// but the policy store configuration might not have them
fn normalize_issuer(issuer: &str) -> String {
    // Parse the issuer as a URL be consistent after parsing trusted issuers URL by `url` crate.
    if let Ok(url) = url::Url::parse(issuer) {
        url.to_string().trim_end_matches('/').to_string()
    } else {
        issuer.trim_end_matches('/').to_string()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use test_utils::assert_eq;

    #[test]
    fn test_normalize_issuer() {
        // Test that uppercase letters are lowered
        assert_eq!(
            normalize_issuer("https://DEV-1e737fn2gji0j3fe.us.auth0.com/"),
            "https://dev-1e737fn2gji0j3fe.us.auth0.com"
        );

        // Test that trailing slashes are removed
        assert_eq!(
            normalize_issuer("https://dev-1e737fn2gji0j3fe.us.auth0.com/"),
            "https://dev-1e737fn2gji0j3fe.us.auth0.com"
        );

        // Test that issuers without trailing slashes are unchanged
        assert_eq!(
            normalize_issuer("https://dev-1e737fn2gji0j3fe.us.auth0.com"),
            "https://dev-1e737fn2gji0j3fe.us.auth0.com"
        );

        // Test that multiple trailing slashes are removed
        assert_eq!(
            normalize_issuer("https://example.com///"),
            "https://example.com"
        );

        // Test that non-URL strings are handled correctly
        assert_eq!(normalize_issuer("test///"), "test");
        assert_eq!(normalize_issuer("test"), "test");

        // Test edge cases
        assert_eq!(normalize_issuer(""), "");
        assert_eq!(normalize_issuer("/"), "");
        assert_eq!(normalize_issuer("///"), "");
        assert_eq!(
            normalize_issuer("https://example.com"),
            "https://example.com"
        );
        assert_eq!(
            normalize_issuer("https://example.com/"),
            "https://example.com"
        );
        assert_eq!(
            normalize_issuer("https://example.com/path"),
            "https://example.com/path"
        );
        assert_eq!(
            normalize_issuer("https://example.com/path/"),
            "https://example.com/path"
        );
    }
}
