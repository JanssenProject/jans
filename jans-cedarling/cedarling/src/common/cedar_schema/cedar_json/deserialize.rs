// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use serde::de;
use serde_json::Value;
use std::collections::HashMap;

/// Deserializes a [`Value`] to a String
pub fn deserialize_to_string<'de, D>(value: Value) -> Result<String, D::Error>
where
    D: serde::Deserializer<'de>,
{
    serde_json::from_value::<String>(value).map_err(|e| {
        de::Error::custom(format!(
            "error while desrializing JSON Value to a String: {e}"
        ))
    })
}

/// Deserialize a [`Value`] to a to the attrs of a [`AttributeKind::Record`]
pub fn deserialize_record_attrs<'de, D>(
    attrs: Value,
) -> Result<HashMap<AttributeName, Attribute>, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let attrs_json = serde_json::from_value::<HashMap<String, Value>>(attrs).map_err(|e| {
        de::Error::custom(format!(
            "error while deserializing cedar record attribute: {e}"
        ))
    })?;

    // loop through each attr then deserialize into Self
    let mut attrs = HashMap::<AttributeName, Attribute>::new();
    for (key, val) in attrs_json.into_iter() {
        let val = serde_json::from_value::<Attribute>(val).map_err(|e| {
            de::Error::custom(format!(
                "error while deserializing cedar record attribute: {e}"
            ))
        })?;
        attrs.insert(key, val);
    }

    Ok(attrs)
}
