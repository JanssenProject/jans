// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::log::LogLevel;
use crate::log::interface::{LogWriter, Loggable};
use crate::log::log_json::LogJson;

use web_sys::console;
use web_sys::js_sys::Array;
use web_sys::wasm_bindgen::JsValue;

/// Mode for stdout logger in WASM builds.
/// In WASM, only immediate logging is supported.
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum StdOutLoggerMode {
    /// Immediate logging to console.
    Immediate,
}

/// A logger that write to std output.
pub(crate) struct StdOutLogger {
    log_level: LogLevel,
}

impl StdOutLogger {
    // mode with `_mode` parameter is kept for compatibility with native version
    pub(crate) fn new(log_level: LogLevel, _mode: StdOutLoggerMode) -> Self {
        Self { log_level }
    }

    /// Matches the native logger; every entry is written when logged, so none is pending.
    #[allow(clippy::unused_self)] // same signature as the native logger
    pub(crate) fn shut_down(&self) -> std::future::Ready<()> {
        std::future::ready(())
    }
}

// Implementation of LogWriter
impl LogWriter for StdOutLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }

        let js_string = JsValue::from(LogJson::from_loggable(&entry).as_str());

        let js_array = Array::new();
        js_array.push(&js_string);

        match entry.get_log_level() {
            Some(LogLevel::FATAL) => {
                // error is highest level of logging
                console::error(&js_array);
            },
            Some(LogLevel::ERROR) => {
                console::error(&js_array);
            },
            Some(LogLevel::WARN) => {
                console::warn(&js_array);
            },
            Some(LogLevel::INFO) => {
                console::info(&js_array);
            },
            Some(LogLevel::DEBUG) => {
                console::debug(&js_array);
            },
            Some(LogLevel::TRACE) => {
                console::trace(&js_array);
            },
            None => console::log(&js_array),
        }
    }

    fn log_fn<F, R>(&self, log_fn: crate::log::loggable_fn::LoggableFn<F>)
    where
        R: Loggable,
        F: Fn(crate::log::BaseLogEntry) -> R,
    {
        if !log_fn.can_log(self.log_level) {
            // do nothing
            return;
        }

        self.log_any(log_fn.build());
    }
}
