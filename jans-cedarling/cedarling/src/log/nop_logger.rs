// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::log::loggable_fn::LoggableFn;

use super::interface::LogWriter;

/// A logger that do nothing.
pub(crate) struct NopLogger;

// Implementation of LogWriter
impl LogWriter for NopLogger {
    fn log_any<T>(&self, _entry: T) {
        // Do nothing
    }

    fn log_fn<F, R>(&self, _log_fn: LoggableFn<F>)
    where
        R: super::interface::Loggable,
        F: Fn(super::BaseLogEntry) -> R,
    {
        // Do nothing
    }
}
