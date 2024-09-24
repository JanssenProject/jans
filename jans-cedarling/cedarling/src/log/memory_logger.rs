/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::interface::{LogStorage, LogWriter};
use crate::models::log_config::MemoryLogConfig;
use crate::models::log_entry::LogEntry;
use sparkv::{Config as ConfigSparKV, SparKV};
use std::{cell::RefCell, time::Duration};

/// A logger that store logs in-memory.
pub(crate) struct MemoryLogger {
    // Using RefCell to enable interior mutability
    storage: RefCell<SparKV>,
}

impl MemoryLogger {
    pub fn new(config: MemoryLogConfig) -> Self {
        let sparkv_config = ConfigSparKV {
            default_ttl: Duration::from_secs(config.log_ttl),
            ..Default::default()
        };

        MemoryLogger {
            storage: RefCell::new(SparKV::with_config(sparkv_config)),
        }
    }
}

// Implementation of LogWriter
impl LogWriter for MemoryLogger {
    fn log(&self, entry: LogEntry) {
        let json_string = serde_json::json!(entry).to_string();

        let result = self
            .storage
            .borrow_mut()
            .set(entry.id.to_string().as_str(), &json_string);

        if let Err(err) = result {
            // log error to stderr
            eprintln!("could not store LogEntry to memory: {err:?}");
        };
    }
}

// Implementation of LogStorage
impl LogStorage for MemoryLogger {
    fn pop_logs(&self) -> Vec<LogEntry> {
        // TODO: implement more efficient implementation

        let keys = self.storage.borrow().get_keys();

        keys.iter()
            .filter_map(|key| self.storage.borrow_mut().pop(key))
            // we call unwrap, because we know that the value is valid json
            .map(|str_json| serde_json::from_str::<LogEntry>(str_json.as_str()).unwrap())
            .collect()
    }

    fn get_log_by_id(&self, id: &str) -> Option<LogEntry> {
        self.storage
            .borrow()
            .get(id)
            // we call unwrap, because we know that the value is valid json
            .map(|str_json| serde_json::from_str::<LogEntry>(str_json.as_str()).unwrap())
    }

    fn get_log_ids(&self) -> Vec<String> {
        self.storage.borrow().get_keys()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::log_entry::{AuthorizationLogInfo, Decision, LogEntry, LogType};
    use uuid7::uuid7;

    fn create_memory_logger() -> MemoryLogger {
        let config = MemoryLogConfig { log_ttl: 60 };
        MemoryLogger::new(config)
    }

    #[test]
    fn test_log_and_get_logs() {
        let logger = create_memory_logger();

        // create log entries
        let entry1 = LogEntry::new_with_data(uuid7(), "app1".to_string(), LogType::Decision)
            .set_message("some message".to_string())
            .set_auth_info(AuthorizationLogInfo {
                principal: "test_principal".to_string(),
                action: "test_action".to_string(),
                resource: "test_resource".to_string(),
                context: "{}".to_string(),
                decision: Decision::Allow,
                diagnostics: "test diagnostic info".to_string(),
            });
        let entry2 = LogEntry::new_with_data(uuid7(), "app2".to_string(), LogType::System);

        // log entries
        logger.log(entry1.clone());
        logger.log(entry2.clone());

        // check that we have two entries in the log database
        assert_eq!(logger.get_log_ids().len(), 2);
        assert_eq!(
            logger.get_log_by_id(&entry1.id.to_string()).unwrap(),
            entry1,
            "Failed to get log entry by id"
        );
        assert_eq!(
            logger.get_log_by_id(&entry2.id.to_string()).unwrap(),
            entry2,
            "Failed to get log entry by id"
        );

        // get logs using `pop_logs`
        let logs = logger.pop_logs();
        assert_eq!(logs.len(), 2);
        assert_eq!(logs[0], entry1, "First log entry is incorrect");
        assert_eq!(logs[1], entry2, "Second log entry is incorrect");

        // check that we have no entries in the log database
        assert!(
            logger.get_log_ids().is_empty(),
            "Logs were not fully popped"
        );
    }

    #[test]
    fn test_pop_logs() {
        let logger = create_memory_logger();

        // create log entries
        let entry1 = LogEntry::new_with_data(uuid7(), "app1".to_string(), LogType::Decision);
        let entry2 = LogEntry::new_with_data(uuid7(), "app2".to_string(), LogType::Metric);

        // log entries
        logger.log(entry1.clone());
        logger.log(entry2.clone());

        // check that we have two entries in the log database
        let logs = logger.pop_logs();
        assert_eq!(logs.len(), 2);
        assert_eq!(logs[0], entry1, "First log entry is incorrect");
        assert_eq!(logs[1], entry2, "Second log entry is incorrect");

        // check that we have no entries in the log database
        assert!(
            logger.get_log_ids().is_empty(),
            "Logs were not fully popped"
        );
    }
}
