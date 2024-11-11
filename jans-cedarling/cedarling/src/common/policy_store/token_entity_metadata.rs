use super::claim_mapping::ClaimMapping;
use serde::{de, Deserialize};
use serde_json::Value;
use std::collections::HashMap;

#[derive(Debug, PartialEq, Clone)]
#[allow(dead_code)]
pub struct TokenEntityMetadata {
    pub user_id: Option<String>,
    pub role_mapping: Option<String>,
    pub claim_mapping: Option<HashMap<String, ClaimMapping>>,
}

impl<'de> Deserialize<'de> for TokenEntityMetadata {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let mut value = Value::deserialize(deserializer)?;

        // Parse `user_id` and set it to None if the value is an
        // empty string or the field is missing.
        let user_id = match value.get_mut("user_id") {
            Some(value) => {
                let value =
                    serde_json::from_value::<String>(value.take()).map_err(de::Error::custom)?;
                match value.as_str() {
                    "" => None,
                    _ => Some(value),
                }
            },
            None => None,
        };

        // Parse `role_mapping` and set it to None if the value is an
        // empty string or the field is missing.
        let role_mapping = match value.get_mut("role_mapping") {
            Some(value) => {
                let value =
                    serde_json::from_value::<String>(value.take()).map_err(de::Error::custom)?;
                match value.as_str() {
                    "" => None,
                    _ => Some(value),
                }
            },
            None => None,
        };

        // Parse `claim_mapping` object.
        let mut claim_mapping = HashMap::new();
        if let Some(mappings) = value
            .get_mut("claim_mapping")
            .and_then(|v| v.as_object_mut())
        {
            for (key, mapping) in mappings.iter_mut() {
                let mapping = serde_json::from_value::<ClaimMapping>(mapping.take())
                    .map_err(de::Error::custom)?;
                claim_mapping.insert(key.to_string(), mapping);
            }
        }
        let claim_mapping = (!claim_mapping.is_empty()).then_some(claim_mapping);

        Ok(TokenEntityMetadata {
            user_id,
            role_mapping,
            claim_mapping,
        })
    }
}

#[cfg(test)]
mod test {
    use super::TokenEntityMetadata;
    use crate::common::policy_store::claim_mapping::ClaimMapping;
    use serde_json::json;
    use std::collections::HashMap;
    use test_utils::assert_eq;

    #[test]
    fn can_deserialize_token_entity_metadata() {
        let metadata_json = json!({
            "user_id": "sub",
            "role_mapping": "",
            "claim_mapping": {
                "dolphin": {
                    "parser": "json",
                    "type": "Acme::Dolphin"
                }
            },
        })
        .to_string();

        let parsed = serde_json::from_str::<TokenEntityMetadata>(&metadata_json)
            .expect("Should deserialize Token Entity Metadata JSON");

        let claim_mapping = Some(
            [(
                "dolphin".to_string(),
                ClaimMapping::Json {
                    r#type: "Acme::Dolphin".to_string(),
                },
            )]
            .into_iter()
            .collect::<HashMap<_, _>>(),
        );

        let expected = TokenEntityMetadata {
            user_id: Some("sub".to_string()),
            role_mapping: None,
            claim_mapping,
        };

        // Assert if the JSON got parsed correctly
        assert_eq!(
            parsed, expected,
            "Expected the Token Entity Metadata to be parsed correctly: {:?}",
            parsed
        );
    }
}
