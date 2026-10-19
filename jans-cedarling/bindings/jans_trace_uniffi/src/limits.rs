// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;

use thiserror::Error;

/// Bounds for untrusted input. Immutable: every `with_max_*` returns a new value.
///
/// Values cross the boundary as `i64`, because Java has no unsigned types and Kotlin's
/// `ULong` cannot be called from Java.
#[derive(Debug, uniffi::Object)]
pub struct Limits(pub(crate) jans_trace_core::Limits);

/// A limit the caller passed has no meaning.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum InvalidLimitError {
    #[error("a limit must not be negative, got {value}")]
    Negative { value: i64 },
}

/// A bound above what the platform can address limits nothing, so saturating keeps its meaning.
fn to_usize(value: i64) -> Result<usize, InvalidLimitError> {
    if value < 0 {
        return Err(InvalidLimitError::Negative { value });
    }
    Ok(usize::try_from(value).unwrap_or(usize::MAX))
}

/// Every value was set from an `i64` or is a small default, so saturation never triggers.
fn to_i64(value: usize) -> i64 {
    i64::try_from(value).unwrap_or(i64::MAX)
}

macro_rules! limit_accessors {
    ($($setter:ident, $getter:ident;)*) => {
        #[uniffi::export]
        impl Limits {
            /// Starts from the core defaults (decision R16).
            #[uniffi::constructor]
            pub fn new() -> Arc<Self> {
                Arc::new(Self(jans_trace_core::Limits::default()))
            }

            $(
                pub fn $setter(&self, value: i64) -> Result<Arc<Self>, InvalidLimitError> {
                    Ok(Arc::new(Self(self.0.$setter(to_usize(value)?))))
                }

                pub fn $getter(&self) -> i64 {
                    to_i64(self.0.$getter())
                }
            )*
        }
    };
}

limit_accessors! {
    with_max_input_bytes, max_input_bytes;
    with_max_depth, max_depth;
    with_max_object_members, max_object_members;
    with_max_array_elements, max_array_elements;
    with_max_string_bytes, max_string_bytes;
    with_max_canonical_bytes, max_canonical_bytes;
    with_max_batch_bytes, max_batch_bytes;
    with_max_batch_records, max_batch_records;
}
