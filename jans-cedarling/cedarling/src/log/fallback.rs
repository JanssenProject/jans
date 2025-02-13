// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In case of failure in MemoryLogger, log to stderr where supported.
//! On WASM, stderr is not supported, so log to whatever the wasm logger uses.

use crate::LogLevel;

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
pub fn log(msg: &str) {
    let log_config = crate::bootstrap_config::LogConfig {
        log_type: crate::bootstrap_config::log_config::LogTypeConfig::StdOut,
        // level is so that all messages passed here are logged.
        log_level: LogLevel::TRACE,
    };
    // This should always be a LogStrategy::StdOut(StdOutLogger)
    let log_strategy = crate::log::LogStrategy::new(&log_config, None);
    use crate::log::interface::LogWriter;
    // a string is always serializable
    log_strategy.log_any(StrWrap(msg))
}
