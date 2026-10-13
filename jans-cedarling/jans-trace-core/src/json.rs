// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Strict JSON for cryptographic input (core design §6.2).
//!
//! `serde_json` silently keeps the last duplicate member and stores large integers exactly,
//! while JCS consumers read numbers as IEEE-754 doubles. Either would let two parties sign
//! or verify different values from the same bytes, so both are rejected here.

use std::fmt;

use serde::de::{self, Deserialize, Deserializer, MapAccess, SeqAccess, Visitor};
use serde_json::{Map, Number, Value};
use thiserror::Error;

/// Why bytes are not strict JSON. Embedded in the error of every function that parses JSON.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum JsonError {
    #[error("invalid JSON at line {line}, column {column}")]
    Invalid { line: usize, column: usize },
    #[error("JSON object contains a duplicate member name")]
    DuplicateMember,
    /// Valid JSON that RFC 8785 / I-JSON cannot represent exactly, such as an integer
    /// beyond +/-(2^53-1).
    #[error("JSON value cannot be canonicalized without changing it")]
    NonCanonicalizable,
}

impl JsonError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::Invalid { .. } => "INVALID_JSON",
            Self::DuplicateMember => "DUPLICATE_MEMBER",
            Self::NonCanonicalizable => "NON_CANONICALIZABLE_JSON",
        }
    }
}

/// Largest integer an IEEE-754 double reproduces exactly.
const MAX_SAFE_INTEGER: u64 = (1 << 53) - 1;

// Markers let `map_error` recover our own rejections from `serde_json::Error`.
const DUPLICATE_MEMBER: &str = "jans-trace-core: duplicate member";
const UNSAFE_INTEGER: &str = "jans-trace-core: unsafe integer";

pub(crate) fn parse_strict(input: &[u8]) -> Result<Value, JsonError> {
    serde_json::from_slice::<StrictValue>(input)
        .map(|StrictValue(value)| value)
        .map_err(|err| map_error(&err))
}

fn map_error(err: &serde_json::Error) -> JsonError {
    let message = err.to_string();
    if message.starts_with(DUPLICATE_MEMBER) {
        JsonError::DuplicateMember
    } else if message.starts_with(UNSAFE_INTEGER) {
        JsonError::NonCanonicalizable
    } else {
        JsonError::Invalid {
            line: err.line(),
            column: err.column(),
        }
    }
}

struct StrictValue(Value);

impl<'de> Deserialize<'de> for StrictValue {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        deserializer.deserialize_any(StrictVisitor)
    }
}

struct StrictVisitor;

impl<'de> Visitor<'de> for StrictVisitor {
    type Value = StrictValue;

    fn expecting(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("a JSON value")
    }

    fn visit_unit<E>(self) -> Result<StrictValue, E> {
        Ok(StrictValue(Value::Null))
    }

    fn visit_bool<E>(self, v: bool) -> Result<StrictValue, E> {
        Ok(StrictValue(Value::Bool(v)))
    }

    fn visit_u64<E: de::Error>(self, v: u64) -> Result<StrictValue, E> {
        if v > MAX_SAFE_INTEGER {
            return Err(E::custom(UNSAFE_INTEGER));
        }
        Ok(StrictValue(Value::Number(v.into())))
    }

    fn visit_i64<E: de::Error>(self, v: i64) -> Result<StrictValue, E> {
        if v.unsigned_abs() > MAX_SAFE_INTEGER {
            return Err(E::custom(UNSAFE_INTEGER));
        }
        Ok(StrictValue(Value::Number(v.into())))
    }

    fn visit_f64<E: de::Error>(self, v: f64) -> Result<StrictValue, E> {
        // JSON text cannot spell NaN or infinity, so this only guards against misuse.
        Number::from_f64(v)
            .map(|n| StrictValue(Value::Number(n)))
            .ok_or_else(|| E::custom("non-finite number"))
    }

    fn visit_str<E>(self, v: &str) -> Result<StrictValue, E> {
        Ok(StrictValue(Value::String(v.to_owned())))
    }

    fn visit_string<E>(self, v: String) -> Result<StrictValue, E> {
        Ok(StrictValue(Value::String(v)))
    }

    fn visit_seq<A: SeqAccess<'de>>(self, mut seq: A) -> Result<StrictValue, A::Error> {
        let mut items = Vec::new();
        while let Some(StrictValue(item)) = seq.next_element()? {
            items.push(item);
        }
        Ok(StrictValue(Value::Array(items)))
    }

    fn visit_map<A: MapAccess<'de>>(self, mut map: A) -> Result<StrictValue, A::Error> {
        let mut members = Map::new();
        while let Some(key) = map.next_key::<String>()? {
            if members.contains_key(&key) {
                return Err(de::Error::custom(DUPLICATE_MEMBER));
            }
            let StrictValue(value) = map.next_value()?;
            members.insert(key, value);
        }
        Ok(StrictValue(Value::Object(members)))
    }
}
