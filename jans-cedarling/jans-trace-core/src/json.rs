// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! RFC 8785 canonicalization and the strict JSON rules every function in this crate applies
//! to its input (core design §6.1, §6.2, §11).
//!
//! `serde_json` silently keeps the last duplicate member and stores large integers exactly,
//! while JCS consumers read numbers as IEEE-754 doubles. Either would let two parties sign
//! or verify different values from the same bytes, so both are rejected here. Structural
//! limits are checked while parsing, so a hostile document is never fully built.

use std::cell::Cell;
use std::fmt;

use serde::de::{self, DeserializeSeed, Deserializer, MapAccess, SeqAccess, Visitor};
use serde_json::{Map, Number, Value};
use thiserror::Error;

use crate::ErrorCode;
use crate::error_code::HasErrorCode;
use crate::limits::LimitKind;
use crate::{Limits, SizeLimitError};

mod canonical;

pub use canonical::{CanonicalizeError, canonicalize_json};
pub(crate) use canonical::{canonicalize_value, write_jcs_string};

/// Why text is not strict JSON. Embedded in the error of every function that parses JSON.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum JsonError {
    #[error("invalid JSON at line {line}, column {column}")]
    Invalid { line: usize, column: usize },
    #[error("JSON object contains a duplicate member name")]
    DuplicateMember,
    /// Valid JSON that RFC 8785 / I-JSON cannot represent exactly, such as an integer
    /// beyond +/-(2^53-1).
    #[error("JSON value cannot be canonicalized without changing it: {0}")]
    NonCanonicalizable(NonCanonicalReason),
    /// Nesting, member count, element count, string length or canonical size is above the
    /// caller's `Limits`.
    #[error(transparent)]
    LimitExceeded(SizeLimitError),
}

impl JsonError {
    #[must_use]
    pub fn code(&self) -> ErrorCode {
        HasErrorCode::error_code(self)
    }
}

/// Why a value has no exact canonical form. Shown through `Display` only; it names the rule,
/// never the record's value.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{0}")]
pub struct NonCanonicalReason(NonCanonicalCause);

/// Private, so the canonicalizer's error type never becomes part of the public API.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
enum NonCanonicalCause {
    #[error("a number is outside +/-(2^53-1), the range every JSON reader keeps exact")]
    OutOfSafeRange,
    /// The canonicalizer's own message; its messages are fixed texts without input values.
    #[error("{0}")]
    Canonicalizer(String),
}

/// Largest integer an IEEE-754 double reproduces exactly.
const MAX_SAFE_INTEGER: u64 = (1 << 53) - 1;

/// `MAX_SAFE_INTEGER` as a double; exact, because it is below 2^53.
const MAX_SAFE_DOUBLE: f64 = 9_007_199_254_740_991.0;

pub(crate) fn parse_strict(input: &str, limits: &Limits) -> Result<Value, JsonError> {
    let rejection = Cell::new(None);
    // `serde_json` errors carry only text, so our own rejections travel beside them.
    let to_json_error = |err: serde_json::Error| {
        rejection.take().unwrap_or(JsonError::Invalid {
            line: err.line(),
            column: err.column(),
        })
    };
    let mut deserializer = serde_json::Deserializer::from_str(input);
    let seed = StrictSeed {
        limits,
        depth: 0,
        rejection: &rejection,
    };
    let value = seed.deserialize(&mut deserializer).map_err(to_json_error)?;
    // The trailing-data check `serde_json::from_str` performs.
    deserializer.end().map_err(to_json_error)?;
    Ok(value)
}

/// Parses one value; `depth` counts the containers around it.
#[derive(Clone, Copy)]
struct StrictSeed<'l> {
    limits: &'l Limits,
    depth: usize,
    /// Where a rejection stores its typed reason; `parse_strict` reads it back.
    rejection: &'l Cell<Option<JsonError>>,
}

impl StrictSeed<'_> {
    /// Records the typed reason and stops `serde_json`.
    fn reject<E: de::Error>(self, error: JsonError) -> E {
        self.rejection.set(Some(error));
        E::custom("rejected by jans-trace-core strict JSON rules")
    }

    fn out_of_safe_range<E: de::Error>(self) -> E {
        self.reject(JsonError::NonCanonicalizable(NonCanonicalReason(
            NonCanonicalCause::OutOfSafeRange,
        )))
    }

    fn limit_exceeded<E: de::Error>(self, kind: LimitKind, limit: usize) -> E {
        self.reject(JsonError::LimitExceeded(SizeLimitError::new(kind, limit)))
    }

    /// Seed for the members of a container that opens at this position.
    fn enter<E: de::Error>(self) -> Result<Self, E> {
        let depth = self.depth.saturating_add(1);
        if depth > self.limits.max_depth() {
            return Err(self.limit_exceeded(LimitKind::Depth, self.limits.max_depth()));
        }
        Ok(Self { depth, ..self })
    }

    fn check_string<E: de::Error>(self, text: &str) -> Result<(), E> {
        if text.len() > self.limits.max_string_bytes() {
            return Err(self.limit_exceeded(LimitKind::StringBytes, self.limits.max_string_bytes()));
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
            return Err(self.out_of_safe_range());
        }
        Ok(Value::Number(v.into()))
    }

    fn visit_i64<E: de::Error>(self, v: i64) -> Result<Value, E> {
        if v.unsigned_abs() > MAX_SAFE_INTEGER {
            return Err(self.out_of_safe_range());
        }
        Ok(Value::Number(v.into()))
    }

    fn visit_f64<E: de::Error>(self, v: f64) -> Result<Value, E> {
        // The same bound as for integers, whatever the spelling: `9007199254740993.0` arrives
        // here already rounded to 2^53, so only a bound on the value catches it, and one rule
        // for every number is simpler for producers than integers and fractions apart.
        if v.abs() > MAX_SAFE_DOUBLE {
            return Err(self.out_of_safe_range());
        }
        // JSON text cannot spell NaN or infinity, so the error only guards against misuse.
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
                return Err(
                    self.limit_exceeded(LimitKind::ArrayElements, self.limits.max_array_elements())
                );
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
                return Err(self.reject(JsonError::DuplicateMember));
            }
            if members.len() == self.limits.max_object_members() {
                return Err(
                    self.limit_exceeded(LimitKind::ObjectMembers, self.limits.max_object_members())
                );
            }
            let value = map.next_value_seed(child)?;
            members.insert(key, value);
        }
        Ok(Value::Object(members))
    }
}
