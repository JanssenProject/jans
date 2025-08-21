// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

/// Normalizes an issuer URL by removing trailing slashes
/// This handles cases where IDPs like Auth0 return issuers with trailing slashes
/// but the policy store configuration might not have them
pub fn normalize_issuer(issuer: &str) -> String {
    issuer.trim_end_matches('/').to_string()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_normalize_issuer() {
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
        assert_eq!(normalize_issuer("https://example.com"), "https://example.com");
        assert_eq!(normalize_issuer("https://example.com/"), "https://example.com");
        assert_eq!(normalize_issuer("https://example.com/path"), "https://example.com/path");
        assert_eq!(normalize_issuer("https://example.com/path/"), "https://example.com/path");
    }
}
