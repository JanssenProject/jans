// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Masking namespace facade.
//!
//! The current implementation lives in `functions::mask`.
//! This module is a placeholder for the upcoming typed split (`types`, `config`, `plan`).
#![allow(unused_imports)]

pub(crate) mod plan {
    #[allow(unused_imports)]
    pub(crate) use crate::functions::mask::{cedarling_mask_plan, cedarling_mask_row};
}
