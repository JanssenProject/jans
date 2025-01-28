// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::interface::{LogStorage, LogWriter, Loggable};
use super::memory_logger::MemoryLogger;
use super::nop_logger::NopLogger;
use super::stdout_logger::StdOutLogger;
use crate::bootstrap_config::log_config::{LogConfig, LogTypeConfig};

/// LogStrategy implements strategy pattern for logging.
/// It is used to provide a single point of access for logging and same api for different loggers.
pub(crate) enum LogStrategy {
    Off(NopLogger),
    MemoryLogger(MemoryLogger),
    StdOut(StdOutLogger),
}

impl LogStrategy {
    /// Creates a new `LogStrategy` based on the provided configuration.
    /// Initializes the corresponding logger accordingly.
    pub fn new(config: &LogConfig) -> Self {
        match config.log_type {
            LogTypeConfig::Off => Self::Off(NopLogger),
            LogTypeConfig::Memory(memory_config) => {
                Self::MemoryLogger(MemoryLogger::new(memory_config, config.log_level))
            },
            LogTypeConfig::StdOut => Self::StdOut(StdOutLogger::new(config.log_level)),
            LogTypeConfig::Lock => todo!(),
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

    fn get_logs_by_request_id_and_tag(&self, id: &str, tag: &str) -> Vec<serde_json::Value> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.get_logs_by_request_id_and_tag(id, tag),
            _ => Vec::new(),
        }
    }
}
