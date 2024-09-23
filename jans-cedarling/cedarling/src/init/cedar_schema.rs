use base64::prelude::*;

const MSG_UNABLE_DECODE_SCHEMA_BASE64: &str = "unable to decode cedar policy schema base64";
const MSG_UNABLE_DECODE_SCHEMA_JSON: &str = "unable to decode cedar policy schema json";

/// A custom deserializer for Cedar's Schema.
//
// is used to deserialize field `schema` in `PolicyStore`
pub(crate) fn parse_cedar_schema<'de, D>(deserializer: D) -> Result<cedar_policy::Schema, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let source = <String as serde::Deserialize>::deserialize(deserializer)?;
    let decoded: Vec<u8> = BASE64_STANDARD.decode(source.as_str()).map_err(|err| {
        serde::de::Error::custom(format!("{MSG_UNABLE_DECODE_SCHEMA_BASE64}: {}", err,))
    })?;

    let schema = cedar_policy::Schema::from_json_file(decoded.as_slice()).map_err(|err| {
        serde::de::Error::custom(format!("{MSG_UNABLE_DECODE_SCHEMA_JSON}: {}", err))
    })?;

    Ok(schema)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::policy_store::PolicyStoreSet;

    #[test]
    fn test_read_ok() {
        static POLICY_STORE_RAW: &str = include_str!("test_files/policy-store_ok.json");

        let policy_result = serde_json::from_str::<PolicyStoreSet>(POLICY_STORE_RAW);
        assert!(policy_result.is_ok());
    }

    #[test]
    fn test_read_base64_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_schema_err_base64.json");

        let policy_result = serde_json::from_str::<PolicyStoreSet>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(MSG_UNABLE_DECODE_SCHEMA_BASE64));
    }

    #[test]
    fn test_read_json_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_schema_err_json.json");

        let policy_result = serde_json::from_str::<PolicyStoreSet>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(MSG_UNABLE_DECODE_SCHEMA_JSON));
    }

    #[test]
    fn test_read_cedar_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_schema_err_cedar_mistake.json");

        let policy_result = serde_json::from_str::<PolicyStoreSet>(POLICY_STORE_RAW);
        // in this scenario error message looks like:
        // `unable to decode cedar policy schema json: failed to resolve type: User_TypeNotExist", line: 35, column: 1`
        let err_msg = policy_result.unwrap_err().to_string();
        assert!(err_msg.contains(MSG_UNABLE_DECODE_SCHEMA_JSON));
        assert!(err_msg.contains("failed to resolve type"));
    }
}
