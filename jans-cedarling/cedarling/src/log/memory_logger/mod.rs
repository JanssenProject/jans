// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use chrono::Duration;
use serde_json::Value;
use std::sync::Mutex;

use sparkv::{Config as ConfigSparKV, Error, SparKV};

use super::LogLevel;
use super::err_log_entry::ErrorLogEntry;
use super::interface::{LogStorage, LogWriter, Loggable, composite_key};
use crate::app_types::{ApplicationName, PdpID};
use crate::bootstrap_config::log_config::MemoryLogConfig;

mod memory_calc;
use memory_calc::calculate_memory_usage;

const STORAGE_MUTEX_EXPECT_MESSAGE: &str = "MemoryLogger storage mutex should unlock";

/// A logger that store logs in-memory.
pub(crate) struct MemoryLogger {
    storage: Mutex<SparKV<serde_json::Value>>,
    log_level: LogLevel,
    pdp_id: PdpID,
    app_name: Option<ApplicationName>,
}

impl MemoryLogger {
    pub fn new(
        config: MemoryLogConfig,
        log_level: LogLevel,
        pdp_id: PdpID,
        app_name: Option<ApplicationName>,
    ) -> Self {
        let default_config: ConfigSparKV = Default::default();

        let sparkv_config = ConfigSparKV {
            default_ttl: Duration::new(
                config.log_ttl.try_into().expect("u64 that fits in a i64"),
                0,
            )
            .expect("a valid duration"),
            max_items: config.max_items.unwrap_or(default_config.max_items),
            max_item_size: config.max_item_size.unwrap_or(default_config.max_item_size),
            ..Default::default()
        };

        MemoryLogger {
            storage: Mutex::new(SparKV::with_config_and_sizer(
                sparkv_config,
                Some(calculate_memory_usage),
            )),
            log_level,
            pdp_id,
            app_name,
        }
    }
}

/// In case of failure in MemoryLogger, log to stderr where supported.
/// On WASM, stderr is not supported, so log to whatever the wasm logger uses.
mod fallback {
    use crate::LogLevel;
    use crate::app_types::{ApplicationName, PdpID};

    /// conform to Loggable requirement imposed by LogStrategy
    #[derive(serde::Serialize)]
    struct StrWrap<'a>(&'a str);

    impl crate::log::interface::Indexed for StrWrap<'_> {
        fn get_id(&self) -> uuid7::Uuid {
            crate::log::log_entry::gen_uuid7()
        }

        fn get_additional_ids(&self) -> Vec<uuid7::Uuid> {
            Vec::new()
        }

        fn get_tags(&self) -> Vec<&str> {
            Vec::new()
        }
    }

    impl crate::log::interface::Loggable for StrWrap<'_> {
        fn get_log_level(&self) -> Option<LogLevel> {
            // These must always be logged.
            Some(LogLevel::TRACE)
        }
    }

    /// Fetch the correct logger. That takes some work, and it's done on every
    /// call. But this is a fallback logger, so it is not intended to be used
    /// often, and in this case correctness and non-fallibility are far more
    /// important than performance.
    pub fn log(msg: &str, pdp_id: &PdpID, app_name: &Option<ApplicationName>) {
        let log_config = crate::bootstrap_config::LogConfig {
            log_type: crate::bootstrap_config::log_config::LogTypeConfig::StdOut,
            // level is so that all messages passed here are logged.
            log_level: LogLevel::TRACE,
        };
        // This should always be a LogStrategy::StdOut(StdOutLogger)
        let log_strategy = crate::log::LogStrategy::new(&log_config, *pdp_id, app_name.clone());
        use crate::log::interface::LogWriter;
        // a string is always serializable
        log_strategy.log_any(StrWrap(msg))
    }
}

fn to_json_value<T: Loggable>(entry: &T) -> Value {
    match serde_json::to_value(entry) {
        Ok(json) => json,
        Err(err) => {
            let err_msg = format!("failed to serialize log entry to JSON: {err}");
            serde_json::to_value(ErrorLogEntry::from_loggable(entry, err_msg.clone()))
                .expect(&err_msg)
        },
    }
}

// Implementation of LogWriter
impl LogWriter for MemoryLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }

        let entry_id = entry.get_id().to_string();
        let index_keys = entry.get_index_keys();
        let json = to_json_value(&entry);

        let mut storage = self.storage.lock().expect(STORAGE_MUTEX_EXPECT_MESSAGE);

        let set_result = storage.set(&entry_id, json, index_keys.as_slice());

        let err = match set_result {
            Ok(_) => return,
            Err(Error::CapacityExceeded) => {
                // remove oldest key and try again

                let key_to_delete = storage
                    .get_oldest_key_by_expiration()
                    .map(|exp_entry| exp_entry.key.clone());

                if let Some(key) = key_to_delete {
                    storage.pop(&key);
                }

                // It should be rare case, so instead of cloning the whole entry,
                // in success case we convert raw value to json (here).
                // Or we should use Rc<LogEntry> and use Rc::clone() instead.
                let json = to_json_value(&entry);

                // set_again
                if let Err(err) = storage.set(&entry_id, json, index_keys.as_slice()) {
                    err
                } else {
                    return;
                }
            },
            Err(err) => err,
        };

        fallback::log(
            &format!("could not store LogEntry to memory: {err:?}"),
            &self.pdp_id,
            &self.app_name,
        );
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

    fn get_logs_by_tag(&self, tag: &str) -> Vec<serde_json::Value> {
        self.storage
            .lock()
            .expect(STORAGE_MUTEX_EXPECT_MESSAGE)
            .get_by_index_key(tag)
            .map(|v| v.to_owned())
            .collect()
    }

    fn get_logs_by_request_id(&self, request_id: &str) -> Vec<serde_json::Value> {
        self.storage
            .lock()
            .expect(STORAGE_MUTEX_EXPECT_MESSAGE)
            .get_by_index_key(request_id)
            .map(|v| v.to_owned())
            .collect()
    }

    fn get_logs_by_request_id_and_tag(
        &self,
        request_id: &str,
        tag: &str,
    ) -> Vec<serde_json::Value> {
        let key = composite_key(request_id, tag);

        self.storage
            .lock()
            .expect(STORAGE_MUTEX_EXPECT_MESSAGE)
            .get_by_index_key(&key)
            .map(|v| v.to_owned())
            .collect()
    }
}

#[cfg(test)]
mod tests {
    use super::super::interface::Indexed;
    use super::super::{AuthorizationLogInfo, LogEntry, LogType};
    use super::*;
    use crate::log::gen_uuid7;
    use serde_json::json;
    use test_utils::assert_eq;

    fn create_memory_logger(pdp_id: PdpID, app_name: Option<ApplicationName>) -> MemoryLogger {
        let config = MemoryLogConfig {
            log_ttl: 60,
            max_items: None,
            max_item_size: None,
        };
        MemoryLogger::new(config, LogLevel::TRACE, pdp_id, app_name)
    }

    #[test]
    fn test_log_and_get_logs() {
        let pdp_id = PdpID::new();
        let app_name = None;
        let logger = create_memory_logger(pdp_id.clone(), app_name.clone());

        // create log entries
        let entry1 = LogEntry::new_with_data(LogType::Decision, None)
            .set_message("some message".to_string())
            .set_auth_info(AuthorizationLogInfo {
                action: "test_action".to_string(),
                resource: "test_resource".to_string(),
                context: serde_json::json!({}),
                authorize_info: Default::default(),
                authorized: true,
                entities: serde_json::json!({}),
            });

        let entry2 = LogEntry::new_with_data(LogType::System, None);

        assert!(
            entry1.base.id < entry2.base.id,
            "entry1.base.id should be lower than in entry2"
        );

        // log entries
        logger.log_any(entry1.clone());
        logger.log_any(entry2.clone());

        let entry1_json = json!(entry1.clone());
        let entry2_json = json!(entry2.clone());

        // check that we have two entries in the log database
        assert_eq!(logger.get_log_ids().len(), 2);
        assert_eq!(
            logger.get_log_by_id(&entry1.get_id().to_string()).unwrap(),
            entry1_json,
            "Failed to get log entry by id"
        );
        assert_eq!(
            logger.get_log_by_id(&entry2.get_id().to_string()).unwrap(),
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
        let pdp_id = PdpID::new();
        let app_name = None;
        let logger = create_memory_logger(pdp_id.clone(), app_name.clone());

        // create log entries
        let entry1 = LogEntry::new_with_data(LogType::Decision, None);
        let entry2 = LogEntry::new_with_data(LogType::Metric, None);

        // log entries
        logger.log_any(entry1.clone());
        logger.log_any(entry2.clone());

        let entry1_json = json!(entry1.clone());
        let entry2_json = json!(entry2.clone());

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
    fn test_log_index() {
        let request_id = gen_uuid7();

        let logger = MemoryLogger::new(
            MemoryLogConfig {
                log_ttl: 10,
                max_item_size: None,
                max_items: None,
            },
            LogLevel::DEBUG,
            PdpID::new(),
            None,
        );

        let entry_decision = LogEntry::new_with_data(LogType::Decision, None);
        logger.log_any(entry_decision);

        let entry_system_info =
            LogEntry::new_with_data(LogType::System, Some(request_id)).set_level(LogLevel::INFO);
        logger.log_any(entry_system_info);

        let entry_system_debug =
            LogEntry::new_with_data(LogType::System, Some(request_id)).set_level(LogLevel::DEBUG);
        logger.log_any(entry_system_debug);

        let entry_metric = LogEntry::new_with_data(LogType::Metric, None);
        logger.log_any(entry_metric);

        // without request id
        let entry_system_warn =
            LogEntry::new_with_data(LogType::System, None).set_level(LogLevel::WARN);
        logger.log_any(entry_system_warn);

        assert!(
            logger
                .get_logs_by_request_id(request_id.to_string().as_str())
                .len()
                == 2,
            "2 log entries should be present for request id: {request_id}"
        );

        assert!(
            logger
                .get_logs_by_request_id_and_tag(
                    request_id.to_string().as_str(),
                    LogLevel::DEBUG.to_string().as_str()
                )
                .len()
                == 1,
            "1 log entries should be present for request id: {request_id} and debug level"
        );

        assert!(
            logger
                .get_logs_by_tag(LogType::System.to_string().as_str())
                .len()
                == 3,
            "3 system log entries should be present"
        );

        assert!(
            logger
                .get_logs_by_tag(LogLevel::WARN.to_string().as_str())
                .len()
                == 1,
            "1 system log entry should be present with WARN level"
        );
    }

    #[test]
    fn test_max_items_config() {
        let default_config: ConfigSparKV = Default::default();

        // Test default value when None
        let logger = MemoryLogger::new(
            MemoryLogConfig {
                log_ttl: 10,
                max_items: None,
                max_item_size: None,
            },
            LogLevel::DEBUG,
            PdpID::new(),
            None,
        );
        assert_eq!(
            logger.storage.lock().unwrap().config.max_items,
            default_config.max_items
        );

        // Test disabled check when 0
        let logger = MemoryLogger::new(
            MemoryLogConfig {
                log_ttl: 10,
                max_items: Some(0),
                max_item_size: None,
            },
            LogLevel::DEBUG,
            PdpID::new(),
            None,
        );
        assert_eq!(logger.storage.lock().unwrap().config.max_items, 0);

        // Test custom value
        let logger = MemoryLogger::new(
            MemoryLogConfig {
                log_ttl: 10,
                max_items: Some(500),
                max_item_size: None,
            },
            LogLevel::DEBUG,
            PdpID::new(),
            None,
        );
        assert_eq!(logger.storage.lock().unwrap().config.max_items, 500);
    }

    #[test]
    fn test_max_item_size_config() {
        let default_config: ConfigSparKV = Default::default();

        // Test default value when None
        let logger = MemoryLogger::new(
            MemoryLogConfig {
                log_ttl: 10,
                max_items: None,
                max_item_size: None,
            },
            LogLevel::DEBUG,
            PdpID::new(),
            None,
        );
        assert_eq!(
            logger.storage.lock().unwrap().config.max_item_size,
            default_config.max_item_size
        );

        // Test disabled check when 0
        let logger = MemoryLogger::new(
            MemoryLogConfig {
                log_ttl: 10,
                max_items: None,
                max_item_size: Some(0),
            },
            LogLevel::DEBUG,
            PdpID::new(),
            None,
        );
        assert_eq!(logger.storage.lock().unwrap().config.max_item_size, 0);

        // Test custom value
        let logger = MemoryLogger::new(
            MemoryLogConfig {
                log_ttl: 10,
                max_items: None,
                max_item_size: Some(10_000),
            },
            LogLevel::DEBUG,
            PdpID::new(),
            None,
        );
        assert_eq!(logger.storage.lock().unwrap().config.max_item_size, 10_000);
    }
}
