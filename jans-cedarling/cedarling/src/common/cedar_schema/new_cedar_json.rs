// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module is responsible for deserializing the JSON Cedar schema

use serde::Deserialize;

mod action;
mod attr_kind;
mod entity_type;

#[doc(hidden)]
/// A macro to define a newtype wrapping a `String` with automatic implementations for
/// converting from `&str` and `String`.
///
/// # Usage
///
/// ```rust,ignore
/// define_newtype!(AttributeName);
/// define_newtype!(ActionName);
///
/// let attr: AttributeName = "example_attr".into();
/// let action: ActionName = String::from("example_action").into();
///
/// println!("{:?}, {:?}", attr, action);
/// ```
macro_rules! define_newtype {
    ($name:ident) => {
        #[derive(Debug, Clone, PartialEq, Eq, Hash, Deserialize)]
        pub struct $name(pub String);

        impl From<&str> for $name {
            fn from(value: &str) -> Self {
                $name(value.to_string())
            }
        }

        impl From<String> for $name {
            fn from(value: String) -> Self {
                $name(value)
            }
        }
    };
}

define_newtype!(ActionName);
define_newtype!(ActionGroupName);
define_newtype!(AttributeName);
define_newtype!(EntityName);
define_newtype!(EntityOrCommonName);
define_newtype!(ExtensionName);
