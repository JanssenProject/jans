/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// TraceContext
/// ============
///
/// A Python wrapper for the Rust `cedarling::TraceContext` struct.
/// TRACE execution context, already normalized by the host. Cedarling does not
/// parse headers, tokens or baggage and never generates `execution_id`: the
/// execution initiator (gateway, PEP, host) owns the `execution_id` +
/// `execution_authority` pair.
///
/// Attributes
/// ----------
/// :param execution_id: `trace_execution_id` issued by the execution initiator.
/// :param execution_authority: Authority that issued `execution_id`.
/// :param workload_id: Stable id of the workload the decision is made for.
/// :param workload_instance_id: Optional instance of that workload.
///
/// Example
/// -------
/// ```python
/// trace = TraceContext(
///     execution_id="exec-01JABC",
///     execution_authority="https://gateway.example.com",
///     workload_id="spiffe://example.org/agent/planner",
/// )
/// ```
#[pyclass(get_all, set_all, from_py_object)]
#[derive(Clone)]
pub struct TraceContext {
    pub execution_id: String,
    pub execution_authority: String,
    pub workload_id: String,
    pub workload_instance_id: Option<String>,
}

#[pymethods]
impl TraceContext {
    #[new]
    #[pyo3(signature = (execution_id, execution_authority, workload_id, workload_instance_id=None))]
    fn new(
        execution_id: String,
        execution_authority: String,
        workload_id: String,
        workload_instance_id: Option<String>,
    ) -> Self {
        Self {
            execution_id,
            execution_authority,
            workload_id,
            workload_instance_id,
        }
    }
}

impl From<TraceContext> for cedarling::TraceContext {
    fn from(value: TraceContext) -> Self {
        cedarling::TraceContext {
            execution_id: value.execution_id,
            execution_authority: value.execution_authority,
            workload_id: value.workload_id,
            workload_instance_id: value.workload_instance_id,
        }
    }
}
