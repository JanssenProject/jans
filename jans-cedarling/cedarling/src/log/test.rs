// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Log unit test module
//! Contains unit tests for the main code flow with the `LogStrategy``
//! `LogStrategy` wraps all other logger implementations.

use std::io::Write;

use interface::{Indexed, LogWriter};
use nop_logger::NopLogger;
use serde_json::json;
use stdout_logger::StdOutLogger;
use test_utils::assert_eq;

use super::*;
use crate::bootstrap_config::log_config;
use crate::log::log_strategy::{LogEntryWithClientInfo, LogStrategyLogger};
use crate::log::stdout_logger::TestWriter;

#[test]
fn test_new_log_strategy_off() {
    // Arrange
    let config = LogConfig {
        log_type: log_config::LogTypeConfig::Off,
        log_level: crate::LogLevel::DEBUG,
    };

    // Act
    let strategy = LogStrategy::new(&config, PdpID::new(), None);

    // Assert
    assert!(matches!(strategy.logger(), LogStrategyLogger::Off(_)));
}

#[test]
fn test_new_log_strategy_memory() {
    // Arrange
    let config = LogConfig {
        log_type: log_config::LogTypeConfig::Memory(log_config::MemoryLogConfig {
            log_ttl: 60,
            max_item_size: None,
            max_items: None,
        }),
        log_level: crate::LogLevel::DEBUG,
    };

    // Act
    let strategy = LogStrategy::new(&config, PdpID::new(), None);

    // Assert
    assert!(matches!(
        strategy.logger(),
        LogStrategyLogger::MemoryLogger(_)
    ));
}

#[test]
fn test_new_logstrategy_stdout() {
    // Arrange
    let config = LogConfig {
        log_type: log_config::LogTypeConfig::StdOut,
        log_level: crate::LogLevel::DEBUG,
    };

    // Act
    let strategy = LogStrategy::new(&config, PdpID::new(), None);

    // Assert
    assert!(matches!(strategy.logger(), LogStrategyLogger::StdOut(_)));
}

#[test]
fn test_log_memory_logger() {
    let pdp_id = PdpID::new();
    let app_name = None;
    // Arrange
    let config = LogConfig {
        log_type: log_config::LogTypeConfig::Memory(log_config::MemoryLogConfig {
            log_ttl: 60,
            max_item_size: None,
            max_items: None,
        }),
        log_level: crate::LogLevel::TRACE,
    };
    let strategy = LogStrategy::new(&config, pdp_id, app_name.clone());
    let entry = LogEntry {
        base: BaseLogEntry::new(LogType::Decision, gen_uuid7()),
        auth_info: None,
        msg: "Test message".to_string(),
        error_msg: None,
        cedar_lang_version: None,
        cedar_sdk_version: None,
    };

    // Act
    strategy.log_any(entry);

    // Assert
    match &strategy.logger() {
        LogStrategyLogger::MemoryLogger(memory_logger) => {
            assert!(!memory_logger.get_log_ids().is_empty());
            memory_logger.pop_logs();
            // after popping, the memory logger should be empty
            assert!(memory_logger.get_log_ids().is_empty());
            // it is empty after popping, so we can continue testing
        },
        _ => panic!("Expected MemoryLogger"),
    }

    // make same test as for the memory logger
    // create log entries
    let entry1 =
        LogEntry::new_with_data(LogType::Decision, None).set_message("some message".to_string());

    let entry2 = LogEntry::new_with_data(LogType::System, None);

    // log entries
    strategy.log_any(entry1.clone());
    strategy.log_any(entry2.clone());

    let entry1_json = json!(LogEntryWithClientInfo::from_loggable(
        entry1.clone(),
        pdp_id,
        app_name.clone()
    ));
    let entry2_json = json!(LogEntryWithClientInfo::from_loggable(
        entry2.clone(),
        pdp_id,
        app_name.clone()
    ));

    // check that we have two entries in the log database
    assert_eq!(strategy.get_log_ids().len(), 2);
    assert_eq!(
        strategy
            .get_log_by_id(&entry1.get_id().to_string())
            .unwrap(),
        entry1_json,
        "Failed to get log entry by id"
    );
    assert_eq!(
        strategy
            .get_log_by_id(&entry2.get_id().to_string())
            .unwrap(),
        entry2_json,
        "Failed to get log entry by id"
    );

    // get logs using `pop_logs`
    let logs = strategy.pop_logs();
    assert_eq!(logs.len(), 2);
    assert_eq!(logs[0], entry1_json, "First log entry is incorrect");
    assert_eq!(logs[1], entry2_json, "Second log entry is incorrect");

    // check that we have no entries in the log database
    assert!(
        strategy.get_log_ids().is_empty(),
        "Logs were not fully popped"
    );
}

#[test]
fn test_log_stdout_logger() {
    let pdp_id = PdpID::new();
    let app_name = None;
    // Arrange
    let log_entry = LogEntry {
        base: BaseLogEntry::new(LogType::Decision, gen_uuid7()),
        auth_info: None,
        msg: "Test message".to_string(),
        error_msg: None,
        cedar_lang_version: None,
        cedar_sdk_version: None,
    };
    // Serialize the log entry to JSON
    let json_str = json!(LogEntryWithClientInfo::from_loggable(
        log_entry.clone(),
        pdp_id,
        app_name.clone()
    ))
    .to_string();

    let test_writer = TestWriter::new();
    let buffer = Box::new(test_writer.clone()) as Box<dyn Write + Send + Sync + 'static>;
    let logger = StdOutLogger::new_with(buffer, LogLevel::TRACE);
    let strategy =
        LogStrategy::new_with_logger(LogStrategyLogger::StdOut(logger), pdp_id, app_name);

    // Act
    strategy.log_any(log_entry);

    let logged_content = test_writer.into_inner_buf();

    assert_eq!(logged_content, json_str + "\n");
}

#[test]
fn test_log_storage_for_only_writer() {
    let logger = LogStrategyLogger::Off(NopLogger);
    let strategy = LogStrategy::new_with_logger(logger, PdpID::new(), None);

    // make same test as for the memory logger
    // create log entries
    let entry1 =
        LogEntry::new_with_data(LogType::Decision, None).set_message("some message".to_string());

    let entry2 = LogEntry::new_with_data(LogType::System, None);

    // log entries
    strategy.log_any(entry1.clone());
    strategy.log_any(entry2.clone());

    // check that we have two entries in the log database
    // we should not have any entries in the memory logger
    assert_eq!(strategy.get_log_ids().len(), 0);
    assert!(
        strategy
            .get_log_by_id(&entry1.get_id().to_string())
            .is_none(),
        "We should not have entry1 entry in the memory logger"
    );
    assert!(
        strategy
            .get_log_by_id(&entry2.get_id().to_string())
            .is_none(),
        "We should not have entry2 entry in the memory logger"
    );

    // get logs using `pop_logs`
    let logs = strategy.pop_logs();
    assert_eq!(logs.len(), 0);

    // check that we have no entries in the log database
    assert!(
        strategy.get_log_ids().is_empty(),
        "We should not have any logs"
    );
}
