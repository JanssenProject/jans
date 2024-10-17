/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::interface::{LogStorage, LogWriter};
use super::memory_logger::MemoryLogger;
use super::nop_logger::NopLogger;
use super::stdout_logger::StdOutLogger;
use crate::models::log_config::LogConfig;
use crate::models::log_config::LogTypeConfig;
use crate::models::log_entry::LogEntry;

/// LogStrategy implements strategy pattern for logging.
/// It is used to provide a single point of access for logging and same api for different loggers.
pub(crate) enum LogStrategy {
    MemoryLogger(MemoryLogger),
    OnlyWriter(Box<dyn LogWriter + Send + Sync>),
}

impl LogStrategy {
    /// Creates a new `LogStrategy` based on the provided configuration.
    /// Initializes the corresponding logger accordingly.
    pub fn new(config: LogConfig) -> Self {
        match config.log_type {
            LogTypeConfig::Off => Self::OnlyWriter(Box::new(NopLogger)),
            LogTypeConfig::Memory(config) => Self::MemoryLogger(MemoryLogger::new(config)),
            LogTypeConfig::StdOut => Self::OnlyWriter(Box::new(StdOutLogger::new())),
            LogTypeConfig::Lock => todo!(),
        }
    }
}

// Implementation of LogWriter
impl LogWriter for LogStrategy {
    fn log(&self, entry: LogEntry) {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.log(entry),
            Self::OnlyWriter(log_writer) => log_writer.log(entry),
        }
    }
}

// Implementation of LogStorage
// for cases where we not use memory logger we return default value
impl LogStorage for LogStrategy {
    fn pop_logs(&self) -> Vec<LogEntry> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.pop_logs(),
            Self::OnlyWriter(_log_writer) => Vec::new(),
        }
    }

    fn get_log_by_id(&self, id: &str) -> Option<LogEntry> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.get_log_by_id(id),
            Self::OnlyWriter(_log_writer) => None,
        }
    }

    fn get_log_ids(&self) -> Vec<String> {
        match self {
            Self::MemoryLogger(memory_logger) => memory_logger.get_log_ids(),
            Self::OnlyWriter(_log_writer) => Vec::new(),
        }
    }
}
