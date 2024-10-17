/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

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

pub mod interface;
pub(crate) mod log_strategy;
mod memory_logger;
mod nop_logger;
mod stdout_logger;

#[cfg(test)]
mod test;

use std::sync::Arc;

use crate::models::log_config::LogConfig;
pub use interface::LogStorage;
pub(crate) use interface::LogWriter;
pub(crate) use log_strategy::LogStrategy;

/// Type alias for logger that is used in application
pub(crate) type Logger = Arc<LogStrategy>;

/// Initialize logger.
/// entry point for initialize logger
pub(crate) fn init_logger(config: LogConfig) -> LogStrategy {
    LogStrategy::new(config)
}
