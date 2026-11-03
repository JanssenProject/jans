// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! RFC 3339 timestamp deserialization shared by every format version.
//!
//! A stateless codec rather than part of any version's shape, so versions share
//! it instead of each freezing a copy.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Deserializer};

/// Deserializes an optional RFC 3339 timestamp.
pub(super) fn deserialize_option<'de, D>(deserializer: D) -> Result<Option<DateTime<Utc>>, D::Error>
where
    D: Deserializer<'de>,
{
    let opt: Option<String> = Option::deserialize(deserializer)?;
    match opt {
        Some(s) => DateTime::parse_from_rfc3339(&s)
            .map(|dt| Some(dt.with_timezone(&Utc)))
            .map_err(serde::de::Error::custom),
        None => Ok(None),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[derive(Debug, Deserialize)]
    struct Holder {
        #[serde(default, deserialize_with = "deserialize_option")]
        at: Option<DateTime<Utc>>,
    }

    fn at(json: &str) -> Option<DateTime<Utc>> {
        serde_json::from_str::<Holder>(json)
            .expect("timestamp should parse")
            .at
    }

    #[test]
    fn reads_rfc3339_and_treats_absent_or_null_as_none() {
        assert_eq!(
            at(r#"{ "at": "2024-01-02T03:04:05Z" }"#),
            Some(
                DateTime::parse_from_rfc3339("2024-01-02T03:04:05Z")
                    .expect("valid timestamp")
                    .with_timezone(&Utc)
            ),
            "an RFC 3339 timestamp should be parsed into UTC"
        );
        assert_eq!(at("{}"), None, "an absent timestamp should read as None");
        assert_eq!(
            at(r#"{ "at": null }"#),
            None,
            "an explicit null should read as None"
        );
    }

    #[test]
    fn rejects_a_non_rfc3339_timestamp() {
        serde_json::from_str::<Holder>(r#"{ "at": "01/02/2024" }"#)
            .expect_err("a non-RFC-3339 timestamp must be rejected");
    }
}
