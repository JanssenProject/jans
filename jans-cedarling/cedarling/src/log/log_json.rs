// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Serialization of log entries shared by every log sink.

use serde::Serialize;
use serde_json::Value;
use serde_json::value::{RawValue, to_raw_value};

use super::err_log_entry::ErrorLogEntry;
use super::interface::Loggable;

/// Written when neither the entry nor its [`ErrorLogEntry`] can be serialized,
/// so that a log call always produces a line.
const UNSERIALIZABLE_ENTRY_JSON: &str =
    r#"{"log_kind":"System","level":"ERROR","msg":"failed to serialize log entry to JSON"}"#;

/// The JSON text of one log entry.
///
/// Building it never fails or panics, so the sinks can write it as is, and its
/// length is the memory it takes.
#[derive(Debug)]
pub(super) struct LogJson(Box<RawValue>);

impl LogJson {
    /// Serializes `entry`; on failure, an [`ErrorLogEntry`] that keeps the
    /// entry's id and request id, so the lost entry stays correlatable.
    pub(super) fn from_loggable<T: Loggable>(entry: &T) -> Self {
        Self::serialize_or_else(entry, |err| {
            ErrorLogEntry::from_loggable(
                entry,
                format!("failed to serialize log entry to JSON: {err}"),
            )
        })
    }

    fn serialize_or_else<T, E>(entry: &T, on_error: impl FnOnce(serde_json::Error) -> E) -> Self
    where
        T: Serialize,
        E: Serialize,
    {
        let raw = to_raw_value(entry)
            .or_else(|err| to_raw_value(&on_error(err)))
            .or_else(|_| RawValue::from_string(UNSERIALIZABLE_ENTRY_JSON.to_owned()))
            // The constant is valid JSON (checked by a test), so this arm is never taken.
            .unwrap_or_else(|_| RawValue::NULL.to_owned());
        Self(raw)
    }

    pub(super) fn as_str(&self) -> &str {
        self.0.get()
    }

    /// Parses the text back into a [`Value`].
    ///
    /// The text was serialized from a value, so parsing fails only when it is nested
    /// deeper than the parser's recursion limit; such an entry is returned as a
    /// [`Value::String`] holding its JSON text, so it is not lost.
    pub(super) fn to_value(&self) -> Value {
        serde_json::from_str(self.as_str()).unwrap_or_else(|_| Value::String(self.as_str().into()))
    }
}

#[cfg(test)]
mod tests {
    use serde::ser::Error as _;
    use serde_json::json;
    use uuid7::Uuid;

    use super::*;
    use crate::log::interface::Indexed;
    use crate::log::{BaseLogEntry, LogEntry, LogLevel, LogType, gen_uuid7};

    /// A loggable entry whose serialization always fails.
    #[derive(Clone)]
    struct Unserializable {
        base: BaseLogEntry,
    }

    impl Serialize for Unserializable {
        fn serialize<S: serde::Serializer>(&self, _: S) -> Result<S::Ok, S::Error> {
            Err(S::Error::custom("unserializable entry"))
        }
    }

    impl Indexed for Unserializable {
        fn get_id(&self) -> Uuid {
            self.base.get_id()
        }

        fn get_additional_ids(&self) -> Vec<Uuid> {
            self.base.get_additional_ids()
        }

        fn get_tags(&self) -> Vec<&str> {
            self.base.get_tags()
        }
    }

    impl Loggable for Unserializable {
        fn get_log_level(&self) -> Option<LogLevel> {
            self.base.get_log_level()
        }
    }

    #[test]
    fn serializes_entry_as_its_json() {
        let entry = LogEntry::new(BaseLogEntry::new_decision(gen_uuid7()))
            .set_message("some message".to_string());

        let log_json = LogJson::from_loggable(&entry);

        assert_eq!(
            log_json.to_value(),
            json!(entry),
            "the JSON text should parse back to the entry's value"
        );
        assert_eq!(
            log_json.as_str(),
            json!(entry).to_string(),
            "the JSON text should be the entry serialized directly"
        );
    }

    #[test]
    fn unserializable_entry_is_logged_as_error_entry() {
        let request_id = gen_uuid7();
        let entry = Unserializable {
            base: BaseLogEntry::new_decision(request_id),
        };

        let value = LogJson::from_loggable(&entry).to_value();
        let error_entry: ErrorLogEntry = serde_json::from_value(value)
            .expect("an unserializable entry should be logged as an ErrorLogEntry");

        assert_eq!(error_entry.base.id, entry.base.id, "id should be kept");
        assert_eq!(
            error_entry.base.request_id,
            Some(request_id),
            "request id should be kept"
        );
        assert_eq!(
            error_entry.base.log_kind,
            LogType::System,
            "should be a system entry"
        );
        assert_eq!(
            error_entry.base.level,
            Some(LogLevel::ERROR),
            "should be an ERROR entry"
        );
        assert_eq!(
            error_entry.msg, "failed to serialize log entry to JSON: unserializable entry",
            "message should name the serialization error"
        );
    }

    #[test]
    fn unserializable_error_entry_is_logged_as_fixed_json() {
        let entry = Unserializable {
            base: BaseLogEntry::new_decision(gen_uuid7()),
        };

        let log_json = LogJson::serialize_or_else(&entry, |_| entry.clone());

        assert_eq!(
            log_json.as_str(),
            UNSERIALIZABLE_ENTRY_JSON,
            "when the error entry fails too, the fixed JSON should be written"
        );
    }

    #[test]
    fn fixed_json_is_a_valid_error_entry() {
        let value: Value = serde_json::from_str(UNSERIALIZABLE_ENTRY_JSON)
            .expect("the fixed fallback should be valid JSON");

        assert_eq!(
            value,
            json!({
                "log_kind": LogType::System,
                "level": LogLevel::ERROR,
                "msg": "failed to serialize log entry to JSON",
            }),
            "the fixed fallback should use the same field names and values as other entries"
        );
    }

    #[test]
    fn too_deeply_nested_json_is_returned_as_string() {
        let depth = 200;
        let nested = format!("{}{}", "[".repeat(depth), "]".repeat(depth));
        let log_json = LogJson(
            RawValue::from_string(nested.clone()).expect("balanced brackets are valid JSON"),
        );

        assert_eq!(
            log_json.to_value(),
            Value::String(nested),
            "JSON nested past the recursion limit should be returned as its text"
        );
    }
}
