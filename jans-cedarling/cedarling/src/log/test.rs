//! Log unit test module
//! Contains unit tests for the main code flow with the `LogStrategy``
//! `LogStrategy` wraps all other logger implementations.

use std::{
    io::Write,
    time::{SystemTime, UNIX_EPOCH},
};

use super::*;
use crate::{common::app_types, log::stdout_logger::TestWriter};
use interface::LogWriter;
use nop_logger::NopLogger;
use stdout_logger::StdOutLogger;
use uuid7::uuid7;

use crate::bootstrap_config::log_config;

#[test]
fn test_new_log_strategy_off() {
    // Arrange
    let config = LogConfig {
        log_type: log_config::LogTypeConfig::Off,
    };

    // Act
    let strategy = LogStrategy::new(&config);

    // Assert
    assert!(matches!(strategy, LogStrategy::OnlyWriter(_)));
}

#[test]
fn test_new_log_strategy_memory() {
    // Arrange
    let config = LogConfig {
        log_type: log_config::LogTypeConfig::Memory(log_config::MemoryLogConfig { log_ttl: 60 }),
    };

    // Act
    let strategy = LogStrategy::new(&config);

    // Assert
    assert!(matches!(strategy, LogStrategy::MemoryLogger(_)));
}

#[test]
fn test_new_logstrategy_stdout() {
    // Arrange
    let config = LogConfig {
        log_type: log_config::LogTypeConfig::StdOut,
    };

    // Act
    let strategy = LogStrategy::new(&config);

    // Assert
    assert!(matches!(strategy, LogStrategy::OnlyWriter(_)));
}

#[test]
fn test_log_memory_logger() {
    // Arrange
    let config = LogConfig {
        log_type: log_config::LogTypeConfig::Memory(log_config::MemoryLogConfig { log_ttl: 60 }),
    };
    let strategy = LogStrategy::new(&config);
    let entry = LogEntry {
        id: uuid7(),
        time: SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs(),
        log_kind: LogType::Decision,
        pdp_id: uuid7(),
        application_id: Some("test_app".to_string().into()),
        auth_info: None,
        msg: "Test message".to_string(),
        error_msg: None,
        cedar_lang_version: None,
        cedar_sdk_version: None,
    };

    // Act
    strategy.log(entry);

    // Assert
    match &strategy {
        LogStrategy::MemoryLogger(memory_logger) => {
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
    let entry1 = LogEntry::new_with_data(
        app_types::PdpID::new(),
        Some(app_types::ApplicationName("app1".to_string())),
        LogType::Decision,
    )
    .set_message("some message".to_string());

    let entry2 = LogEntry::new_with_data(
        app_types::PdpID::new(),
        Some(app_types::ApplicationName("app2".to_string())),
        LogType::System,
    );

    // log entries
    strategy.log(entry1.clone());
    strategy.log(entry2.clone());

    // check that we have two entries in the log database
    assert_eq!(strategy.get_log_ids().len(), 2);
    assert_eq!(
        strategy.get_log_by_id(&entry1.id.to_string()).unwrap(),
        entry1,
        "Failed to get log entry by id"
    );
    assert_eq!(
        strategy.get_log_by_id(&entry2.id.to_string()).unwrap(),
        entry2,
        "Failed to get log entry by id"
    );

    // get logs using `pop_logs`
    let logs = strategy.pop_logs();
    assert_eq!(logs.len(), 2);
    assert_eq!(logs[0], entry1, "First log entry is incorrect");
    assert_eq!(logs[1], entry2, "Second log entry is incorrect");

    // check that we have no entries in the log database
    assert!(
        strategy.get_log_ids().is_empty(),
        "Logs were not fully popped"
    );
}

#[test]
fn test_log_stdout_logger() {
    // Arrange
    let log_entry = LogEntry {
        id: uuid7(),
        time: SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs(),
        log_kind: LogType::Decision,
        pdp_id: uuid7(),
        application_id: Some("test_app".to_string().into()),
        auth_info: None,
        msg: "Test message".to_string(),
        error_msg: None,
        cedar_lang_version: None,
        cedar_sdk_version: None,
    };
    // Serialize the log entry to JSON
    let json_str = serde_json::json!(&log_entry).to_string();

    let test_writer = TestWriter::new();
    let buffer = Box::new(test_writer.clone()) as Box<dyn Write + Send + Sync + 'static>;
    let logger = StdOutLogger::new_with(buffer);
    let strategy = LogStrategy::OnlyWriter(Box::new(logger));

    // Act
    strategy.log(log_entry);

    let logged_content = test_writer.into_inner_buf();

    assert_eq!(logged_content, json_str + "\n");
}

#[test]
fn test_log_storage_for_only_writer() {
    let strategy = LogStrategy::OnlyWriter(Box::new(NopLogger));

    // make same test as for the memory logger
    // create log entries
    let entry1 = LogEntry::new_with_data(
        app_types::PdpID::new(),
        Some(app_types::ApplicationName("app1".to_string())),
        LogType::Decision,
    )
    .set_message("some message".to_string());

    let entry2 = LogEntry::new_with_data(
        app_types::PdpID::new(),
        Some(app_types::ApplicationName("app2".to_string())),
        LogType::System,
    );

    // log entries
    strategy.log(entry1.clone());
    strategy.log(entry2.clone());

    // check that we have two entries in the log database
    // we should not have any entries in the memory logger
    assert_eq!(strategy.get_log_ids().len(), 0);
    assert!(
        strategy.get_log_by_id(&entry1.id.to_string()).is_none(),
        "We should not have entry1 entry in the memory logger"
    );
    assert!(
        strategy.get_log_by_id(&entry2.id.to_string()).is_none(),
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
