// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde_json::{Value, json};
use std::collections::VecDeque;
use std::mem;

/// Log storage used for batching logs before sending them to the
/// lock server
///
/// # Usage
///
/// ```text
///     1. Call Self::batch then try to send it to the lock server
///     2. If sending the logs is successful, call Self::flush_batch
///     3. If sending the logs failed, and you want to clear the batch but not lose the logs, call Self::clear_batch
/// ```
pub struct LogStore {
    logs: VecDeque<Value>,
    batch: VecDeque<Value>,
}

pub struct Batch<'a>(&'a VecDeque<Value>);

impl Batch<'_> {
    pub fn json(&self) -> Value {
        json!(self.0)
    }

    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }
}

impl LogStore {
    pub fn new() -> Self {
        Self {
            logs: VecDeque::new(),
            batch: VecDeque::new(),
        }
    }

    /// Stores a log entry
    pub fn store(&mut self, entry: Value) {
        self.logs.push_back(entry);
    }

    /// Returns a batch of logs
    ///
    /// Calling this repeatedly without calling [`Self::clear_batch`]
    /// will return the same batch.
    pub fn batch(&mut self) -> Batch<'_> {
        if !self.batch.is_empty() {
            return Batch(&self.batch);
        }

        self.batch = mem::take(&mut self.logs);
        Batch(&self.batch)
    }

    /// Removes batch from memory
    pub fn flush_batch(&mut self) {
        self.batch.clear();
    }

    /// Clears the current batch and puts back the batched
    /// logs into memory.
    pub fn clear_batch(&mut self) {
        self.batch.append(&mut self.logs);
        mem::swap(&mut self.batch, &mut self.logs);
    }
}

#[cfg(test)]
mod test {
    use serde_json::json;

    use super::LogStore;
    use crate::LogLevel;
    use crate::app_types::PdpID;
    use crate::log::{LogEntry, LogType, gen_uuid7};

    #[test]
    fn can_batch_logs() {
        let mut log_store = LogStore::new();

        let pdp_id = PdpID::new();

        let first_entry = LogEntry::new_with_data(pdp_id, None, LogType::System, Some(gen_uuid7()))
            .set_message("test log 1".to_string())
            .set_level(LogLevel::DEBUG);
        let second_entry =
            LogEntry::new_with_data(pdp_id, None, LogType::System, Some(gen_uuid7()))
                .set_message("test log 2".to_string())
                .set_level(LogLevel::DEBUG);

        log_store.store(
            serde_json::to_value(first_entry.clone()).expect("should serialze lfirst log entry"),
        );
        let batch = log_store.batch().json();

        assert_eq!(
            batch,
            json!([first_entry]),
            "first entry should be in the batch"
        );

        log_store.store(
            serde_json::to_value(second_entry.clone()).expect("should serialize second log entry"),
        );

        let batch = log_store.batch().json();
        assert_eq!(
            batch,
            json!([first_entry]),
            "calling batch again should return the same batch"
        );

        log_store.clear_batch();

        let batch = log_store.batch().json();
        assert_eq!(
            batch,
            json!([first_entry, second_entry]),
            "the new batch should contain the second entry after clear_batch was called"
        );

        log_store.flush_batch();

        let batch = log_store.batch();
        assert!(batch.is_empty(), "batch should be empty");
    }
}
