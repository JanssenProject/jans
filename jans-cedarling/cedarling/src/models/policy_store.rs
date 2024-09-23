use std::collections::HashMap;

use crate::init::cedar_schema::parse_cedar_schema;

#[derive(Debug, serde::Deserialize)]
pub struct PolicyStore {
    #[serde(deserialize_with = "parse_cedar_schema")]
    pub schema: cedar_policy::Schema,
}

#[derive(Debug, serde::Deserialize)]

pub(crate) struct PolicyStoreSet {
    #[serde(flatten)]
    pub policy_stores: HashMap<String, PolicyStore>,
}
