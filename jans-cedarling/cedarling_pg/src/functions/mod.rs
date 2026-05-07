// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL-exposed function namespace.
//!
//! Top-level authorization, row helpers, and build-resource functions live here as real
//! submodules. Remaining pg_extern functions in other domain modules are re-exported via
//! thin inline aliases below.
#![allow(unused_imports)]

pub(crate) mod authorized;
pub(crate) mod authorized_row;
pub(crate) mod build_resource;
pub(crate) mod error;

#[cfg(feature = "pg_test")]
pub(crate) mod pg_test_rls_unsigned;

pub(crate) mod where_fn {
    pub(crate) use crate::authz::where_clause::cedarling_where;
}

pub(crate) mod explain {
    pub(crate) use crate::observability::trace::cedarling_explain;
}

pub(crate) mod last_trace {
    pub(crate) use crate::observability::trace::{cedarling_last_trace, cedarling_recent_traces};
}

pub(crate) mod status {
    pub(crate) use crate::observability::status::cedarling_status;
}

pub(crate) mod policy {
    pub(crate) use crate::policy::versions::{
        cedarling_diff_policies, cedarling_register_policy_version, cedarling_rollback_policy,
        cedarling_use_policy,
    };
}

pub(crate) mod schema {
    pub(crate) use crate::policy::schema::cedarling_validate_schema;
}

pub(crate) mod tokens {
    pub(crate) use crate::tokens::sql::{
        cedarling_clear_tokens, cedarling_current_tokens, cedarling_set_tokens,
    };
}
