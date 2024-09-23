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
