// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::VecDeque;
use std::mem;
use std::sync::Mutex;

use super::LogLevel;
use super::interface::Loggable;
use crate::log::fallback;
use crate::{LogConfig, LogWriter};
use serde_json::{Value, json};

/// Logger used for sending logs to the lock server
///
/// # Usage
/// 
/// ```text
///     1. Call Self::batch then try to send it to the lock server
///     2. If sending the logs is successful, call Self::flush_batch
///     3. If sending the logs failed, and you want to clear the batch but not lose the logs, call Self::clear_batch
/// ```
pub(crate) struct LockLogger {
    storage: Mutex<VecDeque<Value>>,
    log_level: LogLevel,
    batch: Mutex<Option<VecDeque<Value>>>,
}

impl LockLogger {
    pub fn new(config: &LogConfig) -> Self {
        Self {
            log_level: config.log_level,
            storage: Mutex::new(VecDeque::new()),
            batch: Mutex::new(None),
        }
    }

    /// Returns a batch of logs
    ///
    /// Calling this repeatedly without calling [`Self::clear_batch`]
    /// will return the same batch.
    pub fn batch(&self) -> BatchedLogs<'_> {
        {
            let batch_lock = self.batch.lock().expect("should acquire batched logs lock");
            if batch_lock.is_some() {
                return BatchedLogs {
                    batch_lock: &self.batch,
                };
            }
        }

        let mut storage_lock = self
            .storage
            .lock()
            .expect("should acquire log storage lock");
        let mut batch_lock = self.batch.lock().expect("should acquire batched logs lock");
        *batch_lock = Some(mem::take(&mut storage_lock));

        BatchedLogs {
            batch_lock: &self.batch,
        }
    }

    /// Removes batch from memory
    pub fn flush_batch(&self) {
        let mut batch_lock = self.batch.lock().expect("should acquire batched logs lock");
        *batch_lock = None;
    }

    /// Clears the current batch and puts back the batched
    /// logs into memory.
    pub fn clear_batch(&self) {
        let batch;
        {
            let mut batch_lock = self.batch.lock().expect("should acquire batched logs lock");
            batch = batch_lock.take();
        }

        let mut batch = match batch {
            Some(b) => b,
            None => return,
        };

        let mut storage_lock = self
            .storage
            .lock()
            .expect("should acquire log storage lock");

        batch.append(&mut storage_lock);
        mem::swap(&mut batch, &mut storage_lock);
    }
}

pub(crate) struct BatchedLogs<'a> {
    batch_lock: &'a Mutex<Option<VecDeque<Value>>>,
}

impl BatchedLogs<'_> {
    /// Serializes the batched logs into a JSON array
    pub fn json(&self) -> Value {
        match self
            .batch_lock
            .lock()
            .expect("should acquire batched logs lock")
            .as_ref()
        {
            Some(batch) => json!(batch),
            None => json!([]),
        }
    }

    /// Checks if the batch has any entries
    pub fn is_empty(&self) -> bool {
        match self
            .batch_lock
            .lock()
            .expect("should acquire batched logs lock")
            .as_ref()
        {
            Some(batch) => batch.is_empty(),
            None => true,
        }
    }
}

impl LogWriter for LockLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            return;
        }

        let entry = match serde_json::to_value(&entry) {
            Ok(entry) => entry,
            Err(e) => {
                fallback::log(&format!("failed to serialize log entry: {e:?}"));
                return;
            },
        };

        let mut storage_lock = self
            .storage
            .lock()
            .expect("should acquire log storage lock");

        storage_lock.push_back(entry);
    }
}

#[cfg(test)]
mod test {
    use serde_json::json;

    use super::LockLogger;
    use crate::app_types::PdpID;
    use crate::log::{LogEntry, LogType, gen_uuid7};
    use crate::{LogConfig, LogTypeConfig, LogWriter};

    #[test]
    fn can_batch_logs() {
        let logger = LockLogger::new(&LogConfig {
            log_type: LogTypeConfig::Lock,
            log_level: crate::LogLevel::DEBUG,
        });

        let pdp_id = PdpID::new();

        let first_entry = LogEntry::new_with_data(pdp_id, None, LogType::System, Some(gen_uuid7()))
            .set_message("test log 1".to_string())
            .set_level(crate::LogLevel::DEBUG);
        let second_entry =
            LogEntry::new_with_data(pdp_id, None, LogType::System, Some(gen_uuid7()))
                .set_message("test log 2".to_string())
                .set_level(crate::LogLevel::DEBUG);

        logger.log_any(first_entry.clone());
        let batch = logger.batch().json();

        assert_eq!(
            batch,
            json!([first_entry]),
            "first entry should be in the batch"
        );

        logger.log_any(second_entry.clone());

        let batch = logger.batch().json();
        assert_eq!(
            batch,
            json!([first_entry]),
            "calling batch again should return the same batch"
        );

        logger.clear_batch();

        let batch = logger.batch().json();
        assert_eq!(
            batch,
            json!([first_entry, second_entry]),
            "the new batch should contain the second entry after clear_batch was called"
        );

        logger.flush_batch();

        let batch = logger.batch();
        assert!(batch.is_empty(), "batch should be empty");
    }
}
