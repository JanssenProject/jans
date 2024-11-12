use super::claim_mapping::ClaimMapping;
use super::{parse_option_hashmap, parse_option_string};
use serde::Deserialize;
use std::collections::HashMap;

#[derive(Debug, PartialEq, Clone, Default, Deserialize)]
#[allow(dead_code)]
pub struct TokenEntityMetadata {
    #[serde(deserialize_with = "parse_option_string", default)]
    pub user_id: Option<String>,
    #[serde(deserialize_with = "parse_option_string", default)]
    pub role_mapping: Option<String>,
    #[serde(deserialize_with = "parse_option_hashmap", default)]
    pub claim_mapping: Option<HashMap<String, ClaimMapping>>,
}
