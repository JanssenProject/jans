// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::store::std_duration_to_chrono_duration;
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::net::IpAddr;
use std::str::FromStr;
use std::time::Duration as StdDuration;

/// Helper module for serializing DateTime
mod datetime {
    use chrono::{DateTime, Utc};
    use serde::{Deserialize, Deserializer, Serializer};

    pub(super) fn serialize<S>(date: &DateTime<Utc>, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_str(&date.to_rfc3339())
    }

    pub(super) fn deserialize<'de, D>(deserializer: D) -> Result<DateTime<Utc>, D::Error>
    where
        D: Deserializer<'de>,
    {
        let s: String = String::deserialize(deserializer)?;
        DateTime::parse_from_rfc3339(&s)
            .map(|dt| dt.with_timezone(&Utc))
            .map_err(serde::de::Error::custom)
    }
}

/// Helper module for serializing Optional DateTime
mod datetime_option {
    use chrono::{DateTime, Utc};
    use serde::{Deserialize, Deserializer, Serializer};

    pub(super) fn serialize<S>(
        date: &Option<DateTime<Utc>>,
        serializer: S,
    ) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        match date {
            Some(dt) => serializer.serialize_some(&dt.to_rfc3339()),
            None => serializer.serialize_none(),
        }
    }

    pub(super) fn deserialize<'de, D>(deserializer: D) -> Result<Option<DateTime<Utc>>, D::Error>
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
}

/// Represents the type of a Cedar value based on JSON structure.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum CedarType {
    /// String type
    String,
    /// Long (integer) type
    Long,
    /// Boolean type
    Bool,
    /// Set (array) type
    Set,
    /// Record (object) type
    Record,
    /// Entity reference type
    Entity,
    /// IP address extension type
    Ip,
    /// Decimal extension type
    Decimal,
}

impl CedarType {
    /// Infer the Cedar type from a JSON value.
    ///
    /// This method detects extension types by recognizing:
    /// - `__extn` markers with `fn` = "ip" or "decimal"
    /// - Strings that look like IP addresses or decimals (when auto-detection is appropriate)
    pub fn from_value(value: &Value) -> Self {
        match value {
            Value::String(s) => {
                // Check for IP address format
                if IpAddr::from_str(s).is_ok() {
                    return Self::Ip;
                }
                // Check for decimal format (contains decimal point and parseable as f64)
                if s.contains('.')
                    && s.parse::<f64>().is_ok()
                    && !s.ends_with('.')
                    && s.chars().filter(|&c| c == '.').count() == 1
                {
                    return Self::Decimal;
                }
                Self::String
            },
            Value::Number(n) => {
                if n.is_i64() || n.is_u64() {
                    Self::Long
                } else {
                    // Floating point numbers become decimals in Cedar
                    Self::Decimal
                }
            },
            Value::Bool(_) => Self::Bool,
            Value::Array(_) => Self::Set,
            Value::Object(obj) => {
                // Check for extension type marker (__extn)
                if let Some(extn) = obj.get("__extn") {
                    if let Some(extn_obj) = extn.as_object() {
                        if let Some(fn_name) = extn_obj.get("fn").and_then(|v| v.as_str()) {
                            return match fn_name {
                                "ip" | "ipaddr" => Self::Ip,
                                "decimal" => Self::Decimal,
                                _ => Self::Record,
                            };
                        }
                    }
                }

                // Check if it's an entity reference with explicit marker
                // Entity references must have exactly "type" and "id" fields,
                // and the "type" value must be a string (to avoid misclassifying
                // normal records that happen to have these fields)
                if obj.len() == 2
                    && obj.contains_key("type")
                    && obj.contains_key("id")
                    && obj.get("type").map_or(false, |v| v.is_string())
                    && obj.get("id").map_or(false, |v| v.is_string())
                {
                    Self::Entity
                } else {
                    Self::Record
                }
            },
            Value::Null => {
                // Null is not a valid Cedar type, but we'll default to String
                Self::String
            },
        }
    }
}

/// A data entry in the DataStore with value and metadata.
///
/// This structure wraps the actual value with metadata including creation time,
/// expiration time, access count, and type information.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct DataEntry {
    /// The key for this entry
    pub key: String,
    /// The actual value stored
    pub value: Value,
    /// The inferred Cedar type of the value
    pub data_type: CedarType,
    /// Timestamp when this entry was created
    #[serde(with = "datetime")]
    pub created_at: DateTime<Utc>,
    /// Timestamp when this entry expires (if TTL is set)
    #[serde(with = "datetime_option")]
    pub expires_at: Option<DateTime<Utc>>,
    /// Number of times this entry has been accessed
    pub access_count: u64,
}

impl DataEntry {
    /// Create a new DataEntry with the given key and value.
    ///
    /// The `created_at` timestamp is set to the current time,
    /// and `expires_at` is calculated from the optional TTL.
    /// Uses saturating conversion for TTL to prevent overflow.
    pub fn new(key: String, value: Value, ttl: Option<StdDuration>) -> Self {
        let created_at = Utc::now();
        let expires_at = ttl.map(|duration| {
            // Use saturating conversion to prevent overflow
            let chrono_duration = std_duration_to_chrono_duration(duration);
            created_at + chrono_duration
        });

        Self {
            key,
            data_type: CedarType::from_value(&value),
            value,
            created_at,
            expires_at,
            access_count: 0,
        }
    }

    /// Increment the access count for this entry.
    pub fn increment_access(&mut self) {
        self.access_count = self.access_count.saturating_add(1);
    }

    /// Check if this entry has expired.
    pub fn is_expired(&self) -> bool {
        if let Some(expires_at) = self.expires_at {
            Utc::now() > expires_at
        } else {
            false
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_cedar_type_from_value() {
        assert_eq!(CedarType::from_value(&json!("test")), CedarType::String);
        assert_eq!(CedarType::from_value(&json!(42)), CedarType::Long);
        assert_eq!(CedarType::from_value(&json!(true)), CedarType::Bool);
        assert_eq!(CedarType::from_value(&json!([1, 2, 3])), CedarType::Set);
        assert_eq!(CedarType::from_value(&json!({"a": 1})), CedarType::Record);
        assert_eq!(
            CedarType::from_value(&json!({"type": "User", "id": "123"})),
            CedarType::Entity
        );
    }

    #[test]
    fn test_data_entry_new() {
        let entry = DataEntry::new("key1".to_string(), json!("value1"), None);
        assert_eq!(entry.key, "key1");
        assert_eq!(entry.value, json!("value1"));
        assert_eq!(entry.data_type, CedarType::String);
        assert_eq!(entry.access_count, 0);
        assert!(entry.expires_at.is_none());
    }

    #[test]
    fn test_data_entry_with_ttl() {
        let entry = DataEntry::new(
            "key1".to_string(),
            json!("value1"),
            Some(StdDuration::from_secs(60)),
        );
        assert!(entry.expires_at.is_some());
        assert!(entry.expires_at.unwrap() > entry.created_at);
    }

    #[test]
    fn test_increment_access() {
        let mut entry = DataEntry::new("key1".to_string(), json!("value1"), None);
        assert_eq!(entry.access_count, 0);
        entry.increment_access();
        assert_eq!(entry.access_count, 1);
        entry.increment_access();
        assert_eq!(entry.access_count, 2);
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_is_expired() {
        let entry = DataEntry::new(
            "key1".to_string(),
            json!("value1"),
            Some(StdDuration::from_millis(100)),
        );
        assert!(!entry.is_expired());

        // Wait for expiration
        std::thread::sleep(StdDuration::from_millis(150));
        assert!(entry.is_expired());
    }

    #[test]
    fn test_serialization() {
        let entry = DataEntry::new(
            "key1".to_string(),
            json!("value1"),
            Some(StdDuration::from_secs(3600)),
        );
        let serialized = serde_json::to_string(&entry).expect("should serialize");
        let deserialized: DataEntry =
            serde_json::from_str(&serialized).expect("should deserialize");
        assert_eq!(entry.key, deserialized.key);
        assert_eq!(entry.value, deserialized.value);
        assert_eq!(entry.data_type, deserialized.data_type);
        assert_eq!(entry.access_count, deserialized.access_count);

        // Verify datetime fields survive round-trip using RFC3339 representation
        assert_eq!(
            entry.created_at.to_rfc3339(),
            deserialized.created_at.to_rfc3339(),
            "created_at should survive serde_json round-trip"
        );
        assert_eq!(
            entry.expires_at.map(|dt| dt.to_rfc3339()),
            deserialized.expires_at.map(|dt| dt.to_rfc3339()),
            "expires_at should survive serde_json round-trip"
        );
    }
}
