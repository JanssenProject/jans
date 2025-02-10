/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::policy_evaluation_error::PolicyEvaluationError;
use cedarling::bindings::cedar_policy;
use std::collections::HashSet;

use pyo3::prelude::*;

/// Diagnostics
/// ===========
///
/// Provides detailed information about how a policy decision was made, including policies that contributed to the decision and any errors encountered during evaluation.
///
/// Attributes
/// ----------
/// reason : set of str
///     A set of `PolicyId`s for the policies that contributed to the decision. If no policies applied, this set is empty.
/// errors : list of PolicyEvaluationError
///     A list of errors that occurred during the authorization process. These are unordered as policies may be evaluated in any order.
#[derive(Debug, Clone)]
#[pyclass(get_all)]
pub struct Diagnostics {
    /// `PolicyId`s of the policies that contributed to the decision.
    /// If no policies applied to the request, this set will be empty.
    reason: HashSet<String>,
    /// Errors that occurred during authorization. The errors should be
    /// treated as unordered, since policies may be evaluated in any order.
    errors: Vec<PolicyEvaluationError>,
}

impl From<&cedar_policy::Diagnostics> for Diagnostics {
    fn from(value: &cedar_policy::Diagnostics) -> Self {
        let errors = value
            .errors()
            .map(|err| {
                // convert to cedarling::bindings::PolicyEvaluationError
                let mapped_error: cedarling::bindings::PolicyEvaluationError = err.into();
                // convert to PolicyEvaluationError
                mapped_error.into()
            })
            .collect();

        Self {
            reason: HashSet::from_iter(value.reason().map(|policy_id| policy_id.to_string())),
            errors,
        }
    }
}
