/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub(crate) use cedar_json::CedarSchemaJson;

pub(crate) mod cedar_json;

/// Box that holds the [`cedar_policy::Schema`] and
/// JSON representation that is used to create entities from the schema in the policy store.
#[derive(Debug, Clone)]
#[allow(dead_code)]
pub(crate) struct CedarSchema {
    pub schema: cedar_policy::Schema,
    pub json: cedar_json::CedarSchemaJson,
}
