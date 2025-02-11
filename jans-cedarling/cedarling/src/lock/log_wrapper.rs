// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::LogWriter;
use crate::log::LogStrategy;
use crate::log::lock_logger::BatchedLogs;

/// This is a wrapper over [`crate::log::Logger`] which will only work
/// if [`LogStrategy`] is [`LogStrategy::Lock`]
#[derive(Clone)]
pub struct Logger(crate::log::Logger);

impl Logger {
    pub fn new(logger: crate::log::Logger) -> Self {
        debug_assert!(matches!(*logger, LogStrategy::Lock(_)));
        Self(logger)
    }

    /// Returns a batch of logs
    ///
    /// Calling this repeatedly without calling [`Self::clear_batch`]
    /// will return the same batch.
    pub fn batch(&self) -> BatchedLogs<'_> {
        debug_assert!(matches!(*self.0, LogStrategy::Lock(_)));
        if let LogStrategy::Lock(logger) = self.0.as_ref() {
            logger.batch()
        } else {
            unreachable!("lock::log_wrapper::Logger should only be used with LogStrategy::Lock")
        }
    }

    /// Removes batch from memory
    pub fn flush_batch(&self) {
        debug_assert!(matches!(*self.0, LogStrategy::Lock(_)));
        if let LogStrategy::Lock(logger) = self.0.as_ref() {
            logger.flush_batch()
        } else {
            unreachable!("lock::log_wrapper::Logger should only be used with LogStrategy::Lock")
        }
    }

    /// Clears the current batch and puts back the batched
    /// logs into memory.
    pub fn clear_batch(&self) {
        debug_assert!(matches!(*self.0, LogStrategy::Lock(_)));
        if let LogStrategy::Lock(logger) = self.0.as_ref() {
            logger.clear_batch()
        } else {
            unreachable!("lock::log_wrapper::Logger should only be used with LogStrategy::Lock")
        }
    }
}

impl LogWriter for Logger {
    fn log_any<T: crate::log::interface::Loggable>(&self, entry: T) {
        self.0.log_any(entry);
    }
}
