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
#[pyo3(name = "CedarType")]
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
    /// IP address extension type (ipaddr)
    Ip,
    /// Decimal extension type
    Decimal,
    /// DateTime extension type
    DateTime,
    /// Duration extension type
    Duration,
}

#[pymethods]
impl CedarType {
    /// Get the string representation of the type
    fn __str__(&self) -> &'static str {
        match self {
            CedarType::String => "string",
            CedarType::Long => "long",
            CedarType::Bool => "bool",
            CedarType::Set => "set",
            CedarType::Record => "record",
            CedarType::Entity => "entity",
            CedarType::Ip => "ip",
            CedarType::Decimal => "decimal",
            CedarType::DateTime => "datetime",
            CedarType::Duration => "duration",
        }
    }

    /// Compare two CedarType values for equality
    fn __eq__(&self, other: &Self) -> bool {
        self == other
    }

    /// Get the detailed type representation
    fn __repr__(&self) -> String {
        let variant_name = match self {
            CedarType::String => "String",
            CedarType::Long => "Long",
            CedarType::Bool => "Bool",
            CedarType::Set => "Set",
            CedarType::Record => "Record",
            CedarType::Entity => "Entity",
            CedarType::Ip => "Ip",
            CedarType::Decimal => "Decimal",
            CedarType::DateTime => "DateTime",
            CedarType::Duration => "Duration",
        };
        format!("CedarType.{}", variant_name)
    }
}

impl From<cedarling::CedarType> for CedarType {
    fn from(value: cedarling::CedarType) -> Self {
        match value {
            cedarling::CedarType::String => CedarType::String,
            cedarling::CedarType::Long => CedarType::Long,
            cedarling::CedarType::Bool => CedarType::Bool,
            cedarling::CedarType::Set => CedarType::Set,
            cedarling::CedarType::Record => CedarType::Record,
            cedarling::CedarType::Entity => CedarType::Entity,
            cedarling::CedarType::Ip => CedarType::Ip,
            cedarling::CedarType::Decimal => CedarType::Decimal,
            cedarling::CedarType::DateTime => CedarType::DateTime,
            cedarling::CedarType::Duration => CedarType::Duration,
        }
    }
}

impl From<CedarType> for cedarling::CedarType {
    fn from(value: CedarType) -> Self {
        match value {
            CedarType::String => cedarling::CedarType::String,
            CedarType::Long => cedarling::CedarType::Long,
            CedarType::Bool => cedarling::CedarType::Bool,
            CedarType::Set => cedarling::CedarType::Set,
            CedarType::Record => cedarling::CedarType::Record,
            CedarType::Entity => cedarling::CedarType::Entity,
            CedarType::Ip => cedarling::CedarType::Ip,
            CedarType::Decimal => cedarling::CedarType::Decimal,
            CedarType::DateTime => cedarling::CedarType::DateTime,
            CedarType::Duration => cedarling::CedarType::Duration,
        }
    }
}
