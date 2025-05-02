// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Common
//! This package provides the core data models for the *Cedarling* application,
//! defining the structures and types essential for its functionality and is used in more than one module.

pub(crate) mod app_types;
pub(crate) mod cedar_schema;
pub(crate) mod json_rules;

pub mod policy_store;

/// Used for decoding the policy and schema metadata
#[derive(Debug, Copy, Clone, PartialEq, serde::Deserialize)]
enum Encoding {
    /// indicates that the related value is base64 encoded
    #[serde(rename = "base64")]
    Base64,

    /// indicates that the related value is not encoded, ie it's just a plain string
    #[serde(rename = "none")]
    None,
}

/// Used for decoding the policy and schema metadata
#[derive(Debug, Clone, serde::Deserialize)]
enum ContentType {
    /// indicates that the related value is in the cedar policy / schema language
    #[serde(rename = "cedar")]
    Cedar,

    /// indicates that the related value is in the json representation of the cedar policy / schema language
    #[serde(rename = "cedar-json")]
    CedarJson,
}

// Implement the trait for all iterators over Result<T, E>
impl<T, E, I> PartitionResult<T, E> for I where I: Iterator<Item = Result<T, E>> {}

pub(crate) trait PartitionResult<T, E>: Iterator<Item = Result<T, E>> + Sized {
    /// Splits an iterator of `Result<T, E>` into two separate `Vec`s:
    /// one containing all `Ok(T)` values and the other containing all `Err(E)` values.
    fn partition_result(self) -> (Vec<T>, Vec<E>) {
        let mut ok = Vec::new();
        let mut errs = Vec::new();

        for r in self {
            match r {
                Ok(v) => ok.push(v),
                Err(e) => errs.push(e),
            }
        }
        (ok, errs)
    }
}
