// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL-exposed function namespace.
//!
//! Top-level authorization, row helpers, and build-resource functions live here as real
//! submodules. Other `#[pg_extern]` functions live in their domain modules
//! (`authz/`, `observability/`, `policy/`, `mask/`, `tokens/`) and are registered
//! by pgrx via their original path.

pub(crate) mod authorized;
pub(crate) mod authorized_row;
pub(crate) mod build_resource;
pub(crate) mod error;

#[cfg(feature = "pg_test")]
pub(crate) mod pg_test_rls_unsigned;
