use std::collections::HashMap;

use crate::init::cedar_schema::parse_cedar_schema;

/// PolicyStore contains all the data the Cedarling needs to verify JWT tokens and evaluate policies
#[derive(Debug, serde::Deserialize)]
pub struct PolicyStore {
    #[serde(deserialize_with = "parse_cedar_schema")]
    pub schema: cedar_policy::Schema,
}

/// PolicyStoreSet it is a store for `PolicyStore` accessible by key.
#[derive(Debug, serde::Deserialize)]
pub(crate) struct PolicyStoreSet {
    #[serde(flatten)]
    pub policy_stores: HashMap<String, PolicyStore>,
}
