// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In-memory migration between adjacent policy store formats.
//!
//! Each `vN_to_vM` step depends only on its two version modules; older
//! stores reach the current version by chaining steps.

pub(crate) mod v0_to_v1;
