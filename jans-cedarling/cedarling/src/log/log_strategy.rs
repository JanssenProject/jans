// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::interface::{LogStorage, LogWriter, Loggable};
use super::lock_logger::LockLogger;
use super::memory_logger::MemoryLogger;
use super::nop_logger::NopLogger;
use super::stdout_logger::StdOutLogger;
use crate::bootstrap_config::log_config::{LogConfig, LogTypeConfig};
use crate::init::service_config::LockClientConfig;

/// LogStrategy implements strategy pattern for logging.
/// It is used to provide a single point of access for logging and same api for different loggers.
pub(crate) enum LogStrategy {
    Off(NopLogger),
    MemoryLogger(MemoryLogger),
    StdOut(StdOutLogger),
    Lock(LockLogger),
}

impl LogStrategy {
    /// Creates a new `LogStrategy` based on the provided configuration.
    /// Initializes the corresponding logger accordingly.
    pub fn new(log_config: &LogConfig, lock_client_config: Option<LockClientConfig>) -> Self {
        match &log_config.log_type {
            LogTypeConfig::Off => Self::Off(NopLogger),
            LogTypeConfig::Memory(memory_config) => {
                Self::MemoryLogger(MemoryLogger::new(*memory_config, log_config.log_level))
            },
            LogTypeConfig::StdOut => Self::StdOut(StdOutLogger::new(log_config.log_level)),
            LogTypeConfig::Lock(lock_config) => Self::Lock(LockLogger::new(
                log_config.log_level,
                *lock_config,
                lock_client_config
                    .expect("the lock logger requires lock client configs to function"),
            )),
        }
    }

    /// Closes connections to the lock server
    pub async fn close_lock_connections(&self) {
        if let LogStrategy::Lock(lock_logger) = self {
            lock_logger.close().await
        }
    }
}

// Implementation of LogWriter
impl LogWriter for LogStrategy {
    fn log_any<T: Loggable>(&self, entry: T) {
        match self {
            LogStrategy::Off(log) => log.log_any(entry),
            LogStrategy::MemoryLogger(memory_logger) => memory_logger.log_any(entry),
            LogStrategy::StdOut(std_out_logger) => std_out_logger.log_any(entry),
            LogStrategy::Lock(lock_logger) => lock_logger.log_any(entry),
        }
    }
}

// Implementation of LogStorage
// for cases where we not use memory logger we return default value
impl LogStorage for LogStrategy {
    fn pop_logs(&self) -> Vec<serde_json::Value> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.pop_logs(),
            _ => Vec::new(),
        }
    }

    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.get_log_by_id(id),
            _ => None,
        }
    }

    fn get_log_ids(&self) -> Vec<String> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.get_log_ids(),
            _ => Vec::new(),
        }
    }

    fn get_logs_by_tag(&self, tag: &str) -> Vec<serde_json::Value> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.get_logs_by_tag(tag),
            _ => Vec::new(),
        }
    }

    fn get_logs_by_request_id(&self, request_id: &str) -> Vec<serde_json::Value> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.get_logs_by_request_id(request_id),
            // always empty vector for not MemoryLogger
            _ => Vec::new(),
        }
    }

    fn get_logs_by_request_id_and_tag(&self, id: &str, tag: &str) -> Vec<serde_json::Value> {
        match self {
            Self::MemoryLogger(memory_logger) => {
                memory_logger.get_logs_by_request_id_and_tag(id, tag)
            },
            _ => Vec::new(),
        }
    }
}
