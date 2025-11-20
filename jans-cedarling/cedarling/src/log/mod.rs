// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Log Engine
//! Log Engine is responsible for log all authz and init events.
//!
//! ## Cedarling log types
//!
//!  In [Cedarling-Nativity-Plan Bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) we have variable `CEDARLING_LOG_TYPE`
//!  and config type [LogType](`crate::models::log_config::LogType`) that may contain next values:
//!
//!  * off
//!  * memory
//!  * std_out
//!  * lock
//!  
//!  #### Log type `off`
//!  
//!  This log type is do nothing. It means that all logs will be ignored.
//!  
//!  #### Log type `memory`
//!  
//!  This log type holds all logs in database (in memory) with eviction policy.
//!  
//!  #### Log type `std_out`
//!  
//!  This log type writes all logs to `stdout`. Without storing or additional handling log messages.
//!  [Standart streams](https://www.gnu.org/software/libc/manual/html_node/Standard-Streams.html).
//!  
//!  #### Log type `lock`
//!  
//!  This log type will send logs to the server (corporate feature). Will be discussed later.
//!
//!  ## Log Strategy
//!
//!  We use [`LogStrategy`] to implement all types of logger under one interface.
//!
//!  ## Interfaces
//!
//!  Currently we have 2 interfaces (traits):  
//!
//!  * `LogWriter` (not public) it is used to write logs.
//!
//!  All log implementation should implement this.
//!
//!  * [`LogStorage`] is used to gettting logs from log storage.
//!
//!  Currently only [MemoryLogger](`memory_logger::MemoryLogger`) implement this.

mod err_log_entry;
pub mod interface;

mod log_entry;
mod log_level;
pub(crate) mod log_strategy;
mod memory_logger;
mod nop_logger;
mod stdout_logger;

pub use log_entry::*;
pub use log_level::*;

#[cfg(test)]
mod test;

#[cfg(test)]
use std::sync::LazyLock;
use std::sync::{Arc, Weak};

pub use interface::LogStorage;
pub(crate) use log_strategy::LogStrategy;

use crate::LockServiceConfig;
use crate::app_types::{ApplicationName, PdpID};
use crate::bootstrap_config::log_config::LogConfig;
use crate::lock::{InitLockServiceError, LockService};

/// Type alias for logger that is used in application
pub(crate) type Logger = Arc<LogStrategy>;
pub(crate) type LoggerWeak = Weak<LogStrategy>;

#[cfg(test)]
pub(crate) static TEST_LOGGER: LazyLock<Logger> = LazyLock::new(|| init_test_logger());

/// Initialize logger.
/// entry point for initialize logger
pub(crate) async fn init_logger(
    config: &LogConfig,
    pdp_id: PdpID,
    app_name: Option<ApplicationName>,
    lock_config: Option<&LockServiceConfig>,
) -> Result<Logger, InitLockServiceError> {
    let logger = Arc::new(LogStrategy::new(config, pdp_id, app_name)?);
    let logger_weak = Arc::downgrade(&logger);
    if let Some(lock_config) = lock_config {
        let lock_service = LockService::new(pdp_id, lock_config, Some(logger_weak)).await?;
        logger.set_lock_service(lock_service);
    }
    Ok(logger)
}

#[cfg(test)]
pub(crate) fn init_test_logger() -> Logger {
    Arc::new(
        LogStrategy::new(
            &LogConfig {
                log_level: LogLevel::DEBUG,
                log_type: crate::LogTypeConfig::StdOut,
            },
            PdpID::new(),
            Some("test".to_string().into()),
        )
        .unwrap(),
    )
}
