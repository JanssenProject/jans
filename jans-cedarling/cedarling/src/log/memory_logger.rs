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

/// In case of failure in MemoryLogger, log to stderr where supported.
/// On WASM, stderr is not supported, so log to whatever the wasm logger uses.
mod fallback {
    use crate::LogLevel;

    /// conform to Loggable requirement imposed by LogStrategy
    #[derive(serde::Serialize)]
    struct StrWrap<'a>(&'a str);

    impl crate::log::interface::Loggable for StrWrap<'_> {
        fn get_request_id(&self) -> uuid7::Uuid {
            crate::log::log_entry::gen_uuid7()
        }

        fn get_log_level(&self) -> Option<LogLevel> {
            // These must always be logged.
            Some(LogLevel::TRACE)
        }
    }

    /// Fetch the correct logger. That takes some work, and it's done on every
    /// call. But this is a fallback logger, so it is not intended to be used
    /// often, and in this case correctness and non-fallibility are far more
    /// important than performance.
    pub fn log(msg: &str) {
        let log_config = crate::bootstrap_config::LogConfig {
            log_type: crate::bootstrap_config::log_config::LogTypeConfig::StdOut,
            // level is so that all messages passed here are logged.
            log_level: LogLevel::TRACE,
        };
        // This should always be a LogStrategy::StdOut(StdOutLogger)
        let log_strategy = crate::log::LogStrategy::new(&log_config);
        use crate::log::interface::LogWriter;
        // a string is always serializable
        log_strategy.log_any(StrWrap(msg))
    }
}

// Implementation of LogWriter
impl LogWriter for MemoryLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }

        let json = match serde_json::to_value(&entry) {
            Ok(json) => json,
            Err(err) => {
                fallback::log(&format!(
                    "could not serialize LogEntry to serde_json::Value: {err:?}"
                ));
                return;
            },
        };

        let set_result = self
            .storage
            .lock()
            .expect(STORAGE_MUTEX_EXPECT_MESSAGE)
            .set(&entry.get_request_id().to_string(), json);

        if let Err(err) = set_result {
            fallback::log(&format!("could not store LogEntry to memory: {err:?}"));
        };
    }
}

// Implementation of LogStorage
impl LogStorage for MemoryLogger {
    fn pop_logs(&self) -> Vec<serde_json::Value> {
        self.storage
            .lock()
            .expect(STORAGE_MUTEX_EXPECT_MESSAGE)
            .drain()
            .map(|(_k, value)| value)
            .collect()
    }

    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value> {
        self.storage
            .lock()
            .expect(STORAGE_MUTEX_EXPECT_MESSAGE)
            .get(id)
            .cloned()
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

    #[test]
    fn fallback_logger() {
        struct FailSerialize;

        impl serde::Serialize for FailSerialize {
            fn serialize<S>(&self, _serializer: S) -> Result<S::Ok, S::Error>
            where
                S: serde::Serializer,
            {
                Err(serde::ser::Error::custom("this always fails"))
            }
        }

        impl crate::log::interface::Loggable for FailSerialize {
            fn get_request_id(&self) -> uuid7::Uuid {
                crate::log::log_entry::gen_uuid7()
            }

            fn get_log_level(&self) -> Option<LogLevel> {
                // These must always be logged.
                Some(LogLevel::TRACE)
            }
        }

        let logger = create_memory_logger();
        logger.log_any(FailSerialize);

        // There isn't a good way, in unit tests, to verify the output was
        // actually written to stderr/json console.
        //
        // To eyeball-verify it:
        //   cargo test -- --nocapture fall
        // and look in the output for
        // "could not serialize LogEntry to serde_json::Value: Error(\"this always fails\", line: 0, column: 0)"
        assert!(logger.pop_logs().is_empty(), "logger should be empty");
    }
}
