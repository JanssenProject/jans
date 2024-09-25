/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use crate::init::cedar_schema::parse_cedar_schema;

/// PolicyStoreMap it is a store for `PolicyStore` accessible by key.
#[derive(Debug, serde::Deserialize)]
pub(crate) struct PolicyStoreMap {
    #[serde(flatten)]
    pub policy_stores: HashMap<String, PolicyStore>,
}

/// PolicyStore contains all the data the Cedarling needs to verify JWT tokens and evaluate policies
#[derive(Debug, serde::Deserialize)]
pub struct PolicyStore {
    #[serde(deserialize_with = "parse_cedar_schema")]
    #[allow(dead_code)]
    pub schema: cedar_policy::Schema,
}
