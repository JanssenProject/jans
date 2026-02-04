/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;

/// CedarType
/// =========
///
/// Represents the type of a Cedar value based on JSON structure.
///
/// Values
/// ------
/// - String: String type
/// - Long: Long (integer) type
/// - Bool: Boolean type
/// - Set: Set (array) type
/// - Record: Record (object) type
/// - Entity: Entity reference type
/// - Ip: IP address extension type (ipaddr)
/// - Decimal: Decimal extension type
/// - DateTime: DateTime extension type
/// - Duration: Duration extension type
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[pyclass]
pub struct CedarType {
    inner: cedarling::CedarType,
}

#[pymethods]
impl CedarType {
    /// Get the string representation of the type
    fn __str__(&self) -> String {
        match self.inner {
            cedarling::CedarType::String => "string".to_string(),
            cedarling::CedarType::Long => "long".to_string(),
            cedarling::CedarType::Bool => "bool".to_string(),
            cedarling::CedarType::Set => "set".to_string(),
            cedarling::CedarType::Record => "record".to_string(),
            cedarling::CedarType::Entity => "entity".to_string(),
            cedarling::CedarType::Ip => "ip".to_string(),
            cedarling::CedarType::Decimal => "decimal".to_string(),
            cedarling::CedarType::DateTime => "datetime".to_string(),
            cedarling::CedarType::Duration => "duration".to_string(),
        }
    }

    /// Get the detailed type representation
    fn __repr__(&self) -> String {
        format!("CedarType({})", self.__str__())
    }

    /// Equality operator
    fn __eq__(&self, other: &CedarType) -> bool {
        self.inner == other.inner
    }
}

impl From<cedarling::CedarType> for CedarType {
    fn from(value: cedarling::CedarType) -> Self {
        Self { inner: value }
    }
}

impl From<CedarType> for cedarling::CedarType {
    fn from(value: CedarType) -> Self {
        value.inner
    }
}
