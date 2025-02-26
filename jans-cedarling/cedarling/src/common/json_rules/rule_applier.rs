// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use crate::log::Decision;
use datalogic_rs::JsonLogic;
use smol_str::SmolStr;

use super::JsonRule;

pub(crate) struct RuleApplier<'a> {
    rule: &'a JsonRule,
    data: serde_json::Value,
}

impl<'a> RuleApplier<'a> {
    pub fn new(rule: &'a JsonRule, data: HashMap<SmolStr, Decision>) -> Self {
        Self {
            rule,
            data: serde_json::json!(data),
        }
    }

    pub fn apply(&self) -> Result<bool, ApplyRuleError> {
        let result = JsonLogic::apply(self.rule.rule(), &self.data)?;

        if let serde_json::Value::Bool(b) = result {
            Ok(b)
        } else {
            Err(ApplyRuleError::from(datalogic_rs::Error::Custom(
                "Result is not a boolean.".to_string(),
            )))
        }
    }
}

#[derive(Debug, derive_more::Display, derive_more::Error)]
#[display("Could not apply rule due to error: {source}")]
pub struct ApplyRuleError {
    source: datalogic_rs::Error,
}

impl From<datalogic_rs::Error> for ApplyRuleError {
    fn from(source: datalogic_rs::Error) -> Self {
        Self { source }
    }
}
