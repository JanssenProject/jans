// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Strict JSON for cryptographic input (core design §6.2, §11).
//!
//! `serde_json` silently keeps the last duplicate member and stores large integers exactly,
//! while JCS consumers read numbers as IEEE-754 doubles. Either would let two parties sign
//! or verify different values from the same bytes, so both are rejected here. Structural
//! limits are checked while parsing, so a hostile document is never fully built.

use std::fmt;

use serde::de::{self, DeserializeSeed, Deserializer, MapAccess, SeqAccess, Visitor};
use serde_json::{Map, Number, Value};
use thiserror::Error;

use crate::{Limits, SizeLimitError};

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
    /// Nesting, member count, element count, string length or canonical size is above the
    /// caller's `Limits`.
    #[error(transparent)]
    LimitExceeded(SizeLimitError),
}

impl JsonError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::Invalid { .. } => "INVALID_JSON",
            Self::DuplicateMember => "DUPLICATE_MEMBER",
            Self::NonCanonicalizable => "NON_CANONICALIZABLE_JSON",
            Self::LimitExceeded(e) => e.code(),
        }
    }
}

/// Largest integer an IEEE-754 double reproduces exactly.
const MAX_SAFE_INTEGER: u64 = (1 << 53) - 1;

// Markers let `map_error` recover our own rejections from `serde_json::Error`.
const DUPLICATE_MEMBER: &str = "jans-trace-core: duplicate member";
const UNSAFE_INTEGER: &str = "jans-trace-core: unsafe integer";
const LIMIT_EXCEEDED: &str = "jans-trace-core: limit ";

pub(crate) fn parse_strict(input: &[u8], limits: &Limits) -> Result<Value, JsonError> {
    let mut deserializer = serde_json::Deserializer::from_slice(input);
    let value = StrictSeed { limits, depth: 0 }
        .deserialize(&mut deserializer)
        .map_err(|err| map_error(&err))?;
    // The trailing-data check `serde_json::from_slice` performs.
    deserializer.end().map_err(|err| map_error(&err))?;
    Ok(value)
}

fn map_error(err: &serde_json::Error) -> JsonError {
    let message = err.to_string();
    if message.starts_with(DUPLICATE_MEMBER) {
        JsonError::DuplicateMember
    } else if message.starts_with(UNSAFE_INTEGER) {
        JsonError::NonCanonicalizable
    } else if let Some(limit) = message
        .strip_prefix(LIMIT_EXCEEDED)
        .and_then(leading_number)
    {
        JsonError::LimitExceeded(SizeLimitError::new(limit))
    } else {
        JsonError::Invalid {
            line: err.line(),
            column: err.column(),
        }
    }
}

/// `serde_json` appends ` at line L column C`, so only the leading digits are the limit.
fn leading_number(text: &str) -> Option<usize> {
    let end = text
        .find(|c: char| !c.is_ascii_digit())
        .unwrap_or(text.len());
    text[..end].parse().ok()
}

fn limit_exceeded<E: de::Error>(limit: usize) -> E {
    E::custom(format_args!("{LIMIT_EXCEEDED}{limit}"))
}

/// Parses one value; `depth` counts the containers around it.
#[derive(Clone, Copy)]
struct StrictSeed<'l> {
    limits: &'l Limits,
    depth: usize,
}

impl StrictSeed<'_> {
    /// Seed for the members of a container that opens at this position.
    fn enter<E: de::Error>(self) -> Result<Self, E> {
        let depth = self.depth + 1;
        if depth > self.limits.max_depth() {
            return Err(limit_exceeded(self.limits.max_depth()));
        }
        Ok(Self { depth, ..self })
    }

    fn check_string<E: de::Error>(self, text: &str) -> Result<(), E> {
        if text.len() > self.limits.max_string_bytes() {
            return Err(limit_exceeded(self.limits.max_string_bytes()));
        }
        Ok(())
    }
}

impl<'de> DeserializeSeed<'de> for StrictSeed<'_> {
    type Value = Value;

    fn deserialize<D: Deserializer<'de>>(self, deserializer: D) -> Result<Value, D::Error> {
        deserializer.deserialize_any(self)
    }
}

impl<'de> Visitor<'de> for StrictSeed<'_> {
    type Value = Value;

    fn expecting(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("a JSON value")
    }

    fn visit_unit<E>(self) -> Result<Value, E> {
        Ok(Value::Null)
    }

    fn visit_bool<E>(self, v: bool) -> Result<Value, E> {
        Ok(Value::Bool(v))
    }

    fn visit_u64<E: de::Error>(self, v: u64) -> Result<Value, E> {
        if v > MAX_SAFE_INTEGER {
            return Err(E::custom(UNSAFE_INTEGER));
        }
        Ok(Value::Number(v.into()))
    }

    fn visit_i64<E: de::Error>(self, v: i64) -> Result<Value, E> {
        if v.unsigned_abs() > MAX_SAFE_INTEGER {
            return Err(E::custom(UNSAFE_INTEGER));
        }
        Ok(Value::Number(v.into()))
    }

    fn visit_f64<E: de::Error>(self, v: f64) -> Result<Value, E> {
        // JSON text cannot spell NaN or infinity, so this only guards against misuse.
        Number::from_f64(v)
            .map(Value::Number)
            .ok_or_else(|| E::custom("non-finite number"))
    }

    fn visit_str<E: de::Error>(self, v: &str) -> Result<Value, E> {
        self.check_string::<E>(v)?;
        Ok(Value::String(v.to_owned()))
    }

    fn visit_string<E: de::Error>(self, v: String) -> Result<Value, E> {
        self.check_string::<E>(&v)?;
        Ok(Value::String(v))
    }

    fn visit_seq<A: SeqAccess<'de>>(self, mut seq: A) -> Result<Value, A::Error> {
        let child = self.enter::<A::Error>()?;
        let mut items = Vec::new();
        while let Some(item) = seq.next_element_seed(child)? {
            if items.len() == self.limits.max_array_elements() {
                return Err(limit_exceeded(self.limits.max_array_elements()));
            }
            items.push(item);
        }
        Ok(Value::Array(items))
    }

    fn visit_map<A: MapAccess<'de>>(self, mut map: A) -> Result<Value, A::Error> {
        let child = self.enter::<A::Error>()?;
        let mut members = Map::new();
        while let Some(key) = map.next_key::<String>()? {
            self.check_string::<A::Error>(&key)?;
            if members.contains_key(&key) {
                return Err(de::Error::custom(DUPLICATE_MEMBER));
            }
            if members.len() == self.limits.max_object_members() {
                return Err(limit_exceeded(self.limits.max_object_members()));
            }
            let value = map.next_value_seed(child)?;
            members.insert(key, value);
        }
        Ok(Value::Object(members))
    }
}
