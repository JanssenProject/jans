/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// TokenInput
/// ==========
///
/// A Python wrapper for the Rust `cedarling::TokenInput` struct.
/// Represents a JWT token with explicit type mapping for multi-issuer authorization.
///
/// Attributes
/// ----------
/// :param mapping: Token mapping type (e.g., "Jans::Access_Token", "Acme::DolphinToken")
/// :param payload: JWT token string
///
/// Example
/// -------
/// ```python
/// # Create a token input for multi-issuer authorization
/// token = TokenInput(mapping="Jans::Access_Token", payload="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
/// ```
#[pyclass(get_all, set_all)]
#[derive(Clone)]
pub struct TokenInput {
    /// Token mapping type
    pub mapping: String,
    /// JWT token string
    pub payload: String,
}

#[pymethods]
impl TokenInput {
    #[new]
    fn new(mapping: String, payload: String) -> Self {
        Self { mapping, payload }
    }
}

impl From<TokenInput> for cedarling::TokenInput {
    fn from(value: TokenInput) -> Self {
        cedarling::TokenInput {
            mapping: value.mapping,
            payload: value.payload,
        }
    }
}

