/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use base64::prelude::*;

#[derive(Debug, thiserror::Error)]
pub enum ParceCedarSchemaSetMessage {
    #[error("unable to decode cedar policy schema base64")]
    Base64,
    #[error("unable to decode cedar policy schema json")]
    Json,
}

/// A custom deserializer for Cedar's Schema.
//
// is used to deserialize field `schema` in `PolicyStore`
pub(crate) fn parse_cedar_schema<'de, D>(deserializer: D) -> Result<cedar_policy::Schema, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let source = <String as serde::Deserialize>::deserialize(deserializer)?;
    let decoded: Vec<u8> = BASE64_STANDARD.decode(source.as_str()).map_err(|err| {
        serde::de::Error::custom(format!("{}: {}", ParceCedarSchemaSetMessage::Base64, err,))
    })?;

    let schema = cedar_policy::Schema::from_json_file(decoded.as_slice()).map_err(|err| {
        serde::de::Error::custom(format!("{}: {}", ParceCedarSchemaSetMessage::Json, err))
    })?;

    Ok(schema)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::policy_store::PolicyStoreMap;

    #[test]
    fn test_read_ok() {
        static POLICY_STORE_RAW: &str = include_str!("test_files/policy-store_ok.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        assert!(policy_result.is_ok());
    }

    #[test]
    fn test_read_base64_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_schema_err_base64.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParceCedarSchemaSetMessage::Base64.to_string()));
    }

    #[test]
    fn test_read_json_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_schema_err_json.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParceCedarSchemaSetMessage::Json.to_string()));
    }

    #[test]
    fn test_read_cedar_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_schema_err_cedar_mistake.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        let err_msg = policy_result.unwrap_err().to_string();
        assert_eq!(
            err_msg,
            "unable to decode cedar policy schema json: failed to resolve type: User_TypeNotExist at line 35 column 1"
        );
    }
}
