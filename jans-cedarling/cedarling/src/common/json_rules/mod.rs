// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod rule;
use std::sync::LazyLock;

use datalogic_rs::DataLogic;

pub use self::rule::*;

mod rule_applier;
pub(crate) use self::rule_applier::*;

static ENGINE: LazyLock<DataLogic> = LazyLock::new(DataLogic::new);
