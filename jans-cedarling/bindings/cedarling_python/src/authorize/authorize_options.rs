/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

use super::trace_context::TraceContext;

/// AuthorizeOptions
/// ================
///
/// A Python wrapper for the Rust `cedarling::AuthorizeOptions` struct.
/// Per-call metadata that travels with an authorization request. None of it
/// takes part in policy evaluation: the decision is the same with or without
/// options.
///
/// Attributes
/// ----------
/// :param trace: Optional `TraceContext`. `None` means the host has no
///     execution context, so no TRACE record can be emitted for the decision.
///     The getter returns a copy, so `opts.trace.workload_id = ...` is
///     discarded; assign a new `TraceContext` instead.
///
/// Example
/// -------
/// ```python
/// options = AuthorizeOptions(trace=TraceContext("exec-1", "https://gw.example.com", "workload-1"))
/// request = RequestUnsigned(action=action, resource=resource, context={}, options=options)
/// ```
#[pyclass(get_all, set_all, from_py_object)]
#[derive(Clone)]
pub struct AuthorizeOptions {
    pub trace: Option<TraceContext>,
}

#[pymethods]
impl AuthorizeOptions {
    #[new]
    #[pyo3(signature = (trace=None))]
    fn new(trace: Option<TraceContext>) -> Self {
        Self { trace }
    }
}

impl From<AuthorizeOptions> for cedarling::AuthorizeOptions {
    fn from(value: AuthorizeOptions) -> Self {
        cedarling::AuthorizeOptions {
            trace: value.trace.map(Into::into),
        }
    }
}

/// Converts the optional `options` attribute of a request wrapper; `None` is
/// the same as default options.
pub(crate) fn to_cedarling(options: Option<&AuthorizeOptions>) -> cedarling::AuthorizeOptions {
    options.cloned().map(Into::into).unwrap_or_default()
}
