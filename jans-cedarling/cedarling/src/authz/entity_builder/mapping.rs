// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::policy_store::ClaimMappings;
use serde_json::Value;
use std::collections::HashMap;

impl ClaimMappings {
    /// Creates new claims and adds it to the HashMap of the given claims
    /// if a mapping exists
    ///
    /// * Note that this will overwrite existing names
    pub fn apply_mapping(&self, claims: &HashMap<String, Value>) -> HashMap<String, Value> {
        let mut mapped_claims = HashMap::new();
        for (name, claim) in claims.iter() {
            if let Some(mapping) = self.get(name) {
                let applied_mapping = mapping.apply_mapping(claim);
                mapped_claims.extend(applied_mapping);
            }
        }
        mapped_claims
    }
}

#[cfg(test)]
mod test {
    use crate::common::policy_store::ClaimMappings;
    use serde_json::json;
    use std::collections::HashMap;
    use test_utils::assert_eq;

    #[test]
    fn can_apply_mapping() {
        let claims = HashMap::from([
            ("email".to_string(), json!("test@test.com")),
            ("url".to_string(), json!("https://example.com/test")),
        ]);
        let claim_mapping = serde_json::from_value::<ClaimMappings>(json!({
            "url": {
                "parser": "regex",
                "type": "Jans::Url",
                "regex_expression": r#"^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/(?P<DOMAIN>[^\/]+)(?P<PATH>\/.*)?$"#,
                "SCHEME": {"attr": "scheme", "type": "String"},
                "DOMAIN": {"attr": "domain", "type": "String"},
                "PATH": {"attr": "path", "type": "String"}
            }
        }))
        .unwrap();
        let mapped_claims = claim_mapping.apply_mapping(&claims);
        assert_eq!(
            mapped_claims,
            HashMap::from([
                ("scheme".to_string(), json!("https")),
                ("domain".to_string(), json!("example.com")),
                ("path".to_string(), json!("/test")),
            ])
        );
    }
}
