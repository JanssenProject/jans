// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use chrono::Duration;
use std::sync::Mutex;

use sparkv::{Config as ConfigSparKV, SparKV};

use super::LogLevel;
use super::interface::{LogStorage, LogWriter, Loggable};
use crate::bootstrap_config::log_config::MemoryLogConfig;

const STORAGE_MUTEX_EXPECT_MESSAGE: &str = "MemoryLogger storage mutex should unlock";
const STORAGE_JSON_PARSE_EXPECT_MESSAGE: &str =
    "In MemoryLogger storage value should be valid LogEntry json value";

/// A logger that store logs in-memory.
pub(crate) struct MemoryLogger {
    storage: Mutex<SparKV<serde_json::Value>>,
    log_level: LogLevel,
}

impl MemoryLogger {
    pub fn new(config: MemoryLogConfig, log_level: LogLevel) -> Self {
        let sparkv_config = ConfigSparKV {
            default_ttl: Duration::new(
                config.log_ttl.try_into().expect("u64 that fits in a i64"),
                0,
            )
            .expect("a valid duration"),
            ..Default::default()
        };

        MemoryLogger {
            storage: Mutex::new(SparKV::with_config(sparkv_config)),
            log_level,
        }
    }
}

// Implementation of LogWriter
impl LogWriter for MemoryLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }

        let json = serde_json::json!(entry);

        let result = self
            .storage
            .lock()
            .expect(STORAGE_MUTEX_EXPECT_MESSAGE)
            .set(&entry.get_request_id().to_string(), json);

        if let Err(err) = result {
            // log error to stderr
            eprintln!("could not store LogEntry to memory: {err:?}");
        };
    }
}

// Implementation of LogStorage
impl LogStorage for MemoryLogger {
    fn pop_logs(&self) -> Vec<serde_json::Value> {
        // TODO: implement more efficient implementation

        let mut storage_guard = self.storage.lock().expect(STORAGE_MUTEX_EXPECT_MESSAGE);

        let keys = storage_guard.get_keys();

        keys.iter()
            .filter_map(|key| storage_guard.pop(key))
            // TODO we call unwrap, because we know that the value is valid json
            .map(|value| serde_json::from_value(value).expect(STORAGE_JSON_PARSE_EXPECT_MESSAGE) )
            .collect()
    }

    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value> {
        self.storage.lock().expect(STORAGE_MUTEX_EXPECT_MESSAGE)
        .get(id)
        .and_then(|value|
            // TODO we call unwrap, because we know that the value is valid json
            serde_json::from_value(value.clone()).expect(STORAGE_JSON_PARSE_EXPECT_MESSAGE)
        )
    }

    fn get_log_ids(&self) -> Vec<String> {
        self.storage
            .lock()
            .expect(STORAGE_MUTEX_EXPECT_MESSAGE)
            .get_keys()
    }
}

#[cfg(test)]
mod tests {
    use test_utils::assert_eq;

    use super::super::{AuthorizationLogInfo, LogEntry, LogType};
    use super::*;
    use crate::common::app_types;

    fn create_memory_logger() -> MemoryLogger {
        let config = MemoryLogConfig { log_ttl: 60 };
        MemoryLogger::new(config, LogLevel::TRACE)
    }

    #[test]
    fn test_log_and_get_logs() {
        let logger = create_memory_logger();

        // create log entries
        let entry1 = LogEntry::new_with_data(
            app_types::PdpID::new(),
            Some(app_types::ApplicationName("app1".to_string())),
            LogType::Decision,
        )
        .set_message("some message".to_string())
        .set_auth_info(AuthorizationLogInfo {
            action: "test_action".to_string(),
            resource: "test_resource".to_string(),
            context: serde_json::json!({}),
            person_authorize_info: Default::default(),
            workload_authorize_info: Default::default(),
            authorized: true,
            entities: serde_json::json!({}),
        });
        let entry2 = LogEntry::new_with_data(
            app_types::PdpID::new(),
            Some(app_types::ApplicationName("app2".to_string())),
            LogType::System,
        );

        assert!(
            entry1.base.request_id < entry2.base.request_id,
            "entry1.base.request_id should be lower than in entry2"
        );

        // log entries
        logger.log(entry1.clone());
        logger.log(entry2.clone());

        let entry1_json = serde_json::json!(entry1);
        let entry2_json = serde_json::json!(entry2);

        // check that we have two entries in the log database
        assert_eq!(logger.get_log_ids().len(), 2);
        assert_eq!(
            logger
                .get_log_by_id(&entry1.get_request_id().to_string())
                .unwrap(),
            entry1_json,
            "Failed to get log entry by id"
        );
        assert_eq!(
            logger
                .get_log_by_id(&entry2.get_request_id().to_string())
                .unwrap(),
            entry2_json,
            "Failed to get log entry by id"
        );

        // get logs using `pop_logs`
        let logs = logger.pop_logs();
        assert_eq!(logs.len(), 2);
        assert_eq!(logs[0], entry1_json, "First log entry is incorrect");
        assert_eq!(logs[1], entry2_json, "Second log entry is incorrect");

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
        let entry1 = LogEntry::new_with_data(
            app_types::PdpID::new(),
            Some(app_types::ApplicationName("app1".to_string())),
            LogType::Decision,
        );
        let entry2 = LogEntry::new_with_data(
            app_types::PdpID::new(),
            Some(app_types::ApplicationName("app2".to_string())),
            LogType::Metric,
        );

        // log entries
        logger.log(entry1.clone());
        logger.log(entry2.clone());

        let entry1_json = serde_json::json!(entry1);
        let entry2_json = serde_json::json!(entry2);

        // check that we have two entries in the log database
        let logs = logger.pop_logs();
        assert_eq!(logs.len(), 2);
        assert_eq!(logs[0], entry1_json, "First log entry is incorrect");
        assert_eq!(logs[1], entry2_json, "Second log entry is incorrect");

        // check that we have no entries in the log database
        assert!(
            logger.get_log_ids().is_empty(),
            "Logs were not fully popped"
        );
    }
}
