use super::claim_mapping::ClaimMapping;
use super::{parse_option_hashmap, parse_option_string};
use serde::Deserialize;
use std::collections::HashMap;

#[derive(Debug, PartialEq, Clone, Default, Deserialize)]
#[allow(dead_code)]
pub struct TokenEntityMetadata {
    #[serde(deserialize_with = "parse_option_string", default)]
    pub user_id: Option<String>,
    #[serde(deserialize_with = "parse_option_string", default)]
    pub role_mapping: Option<String>,
    #[serde(deserialize_with = "parse_option_hashmap", default)]
    pub claim_mapping: Option<HashMap<String, ClaimMapping>>,
}

#[cfg(test)]
mod test {
    use super::TokenEntityMetadata;
    use serde_json::json;

    #[test]
    fn can_parse_from_json() {
        let json = json!({});
        let parsed = serde_json::from_value::<TokenEntityMetadata>(json)
            .expect("Failed to parse an empty JSON object into TokenEntityMetadata");
        assert_eq!(
            parsed,
            TokenEntityMetadata::default(),
            "Expected empty JSON to be parsed into default TokenEntityMetadata"
        );

        let json = json!({
            "user_id": "sub",
            "role_mapping": "",
        });
        let parsed = serde_json::from_value::<TokenEntityMetadata>(json).expect(
            "Failed to parse JSON object with user_id and role_mapping into TokenEntityMetadata",
        );
        assert_eq!(
            parsed, 
            TokenEntityMetadata { 
                user_id: Some("sub".into()), 
                role_mapping: None, 
                claim_mapping: None 
            }, 
            "Expected JSON with user_id and empty role_mapping to be parsed into TokenEntityMetadata"
        );
    }

    #[test]
    fn can_parse_from_yaml() {
        let yaml = "";
        let parsed = serde_yml::from_str::<TokenEntityMetadata>(yaml)
            .expect("Failed to parse an empty YAML object into TokenEntityMetadata");
        assert_eq!(
            parsed,
            TokenEntityMetadata::default(),
            "Expected empty YAML to be parsed into default TokenEntityMetadata"
        );
        
        let yaml = "
            user_id: 'sub'
            role_mapping: ''
        ";
        let parsed = serde_yml::from_str::<TokenEntityMetadata>(yaml).expect(
            "Failed to parse YAML object with user_id and role_mapping into TokenEntityMetadata",
        );
        assert_eq!(
            parsed, 
            TokenEntityMetadata { 
                user_id: Some("sub".into()), 
                role_mapping: None, 
                claim_mapping: None 
            }, 
            "Expected YAML with user_id and empty role_mapping to be parsed into TokenEntityMetadata"
        );
    }
}
