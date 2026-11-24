// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Implements a logger that writes to standard output (stdout).
//! Write failures are silently ignored to prevent panics in logging code paths.

// Native-only module (WASM uses `wasm_logger`), so threads and `Instant` are available.
#![allow(clippy::disallowed_methods)]

use std::convert::Infallible;
use std::io::Write;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::mpsc::{self, Receiver, RecvTimeoutError, SyncSender};
use std::sync::{Arc, Mutex, PoisonError};
use std::thread;
use std::time::{Duration, Instant};

use crate::log::interface::{LogWriter, Loggable};
use crate::log::log_json::LogJson;
use crate::log::{BaseLogEntry, LogEntry, LogLevel};

/// Entries the async queue holds before new ones are dropped.
///
/// The queue only absorbs the bursts between flushes (at least every 100 ms by
/// default), which at thousands of authorizations per second is 1 000-3 000
/// entries; this leaves about 3x headroom and bounds the queue at about 10 MB
/// for 1 KB entries.
const QUEUE_CAPACITY: usize = 10_000;

/// How long dropping an async logger waits for the writer to write the queued
/// entries before it detaches the writer, so a blocked stdout cannot hang the caller.
const SHUTDOWN_TIMEOUT: Duration = Duration::from_secs(1);

/// How often an idle writer checks for dropped entries to report, since the
/// caller counts a drop without waking it.
const IDLE_REPORT_INTERVAL: Duration = Duration::from_secs(1);

/// Most bytes handed to stdout in one write, so each finished write counts as progress for `Drop`.
const WRITE_CHUNK: usize = 64 * 1024;

/// Mode for stdout logger in native builds.
/// Supports both immediate and asynchronous logging.
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum StdOutLoggerMode {
    /// Immediate synchronous logging (no buffering).
    Immediate,
    /// Asynchronous logging with configurable timeout and buffer.
    ///
    /// Entries wait in a bounded queue for a writer thread. When stdout is slower
    /// than the log rate and the queue is full, new entries are dropped, and the
    /// writer reports how many in a WARN entry, if the log level allows WARN.
    Async {
        /// Timeout in milliseconds to flush the log buffer.
        timeout_millis: u64,
        /// Maximum buffer size in bytes before an automatic flush.
        buffer_limit: usize,
    },
}

impl StdOutLoggerMode {
    /// 100 ms default timeout
    pub const DEFAULT_FLUSH_TIMEOUT_MILLIS: u64 = 100;
    /// 1 MB limit
    pub const DEFAULT_BUFFER_LIMIT: usize = 1 << 20;
}

/// Serializes an entry on the writer thread, off the caller's path.
type SerializeJob = Box<dyn FnOnce() -> LogJson + Send>;

type BoxedWriter = Box<dyn Write + Send + Sync>;

/// A logger that write to std output.
pub(crate) struct StdOutLogger {
    log_level: LogLevel,
    sink: Sink,
}

enum Sink {
    Immediate(Mutex<BoxedWriter>),
    Async(AsyncSink),
}

/// Settings of the async sink; the bounds are constants outside tests.
#[derive(Clone, Copy)]
struct AsyncConfig {
    flush_timeout: Duration,
    buffer_limit: usize,
    queue_capacity: usize,
    shutdown_timeout: Duration,
    idle_report_interval: Duration,
}

impl AsyncConfig {
    fn new(timeout_millis: u64, buffer_limit: usize) -> Self {
        Self {
            flush_timeout: Duration::from_millis(timeout_millis),
            buffer_limit,
            queue_capacity: QUEUE_CAPACITY,
            shutdown_timeout: SHUTDOWN_TIMEOUT,
            idle_report_interval: IDLE_REPORT_INTERVAL,
        }
    }
}

/// Hands entries to a writer thread without ever blocking or panicking the caller.
struct AsyncSink {
    /// `None` only while dropping: closing the queue tells the writer to write
    /// what is queued and exit.
    jobs: Option<SyncSender<SerializeJob>>,
    /// Entries not queued because the queue was full or the writer had exited,
    /// shared with the writer, which reports and resets it.
    dropped: Arc<AtomicU64>,
    /// Bumped by the writer each time it finishes an entry or a write, so `Drop`
    /// can tell a slow writer from a blocked one.
    progress: Arc<AtomicU64>,
    /// Never receives; it disconnects when the writer thread exits. The mutex
    /// only makes the sink `Sync`, it is never locked.
    writer_exited: Mutex<Receiver<Infallible>>,
    shutdown_timeout: Duration,
}

impl AsyncSink {
    fn spawn(out: BoxedWriter, log_level: LogLevel, config: AsyncConfig) -> Self {
        let (jobs, jobs_receiver) = mpsc::sync_channel(config.queue_capacity);
        let (exited_sender, writer_exited) = mpsc::channel();
        let dropped = Arc::new(AtomicU64::new(0));
        let progress = Arc::new(AtomicU64::new(0));

        let writer = QueueWriter {
            out,
            buffer: String::new(),
            buffer_limit: config.buffer_limit,
            flush_timeout: config.flush_timeout,
            flush_deadline: None,
            idle_report_interval: config.idle_report_interval,
            dropped: Arc::clone(&dropped),
            progress: Arc::clone(&progress),
            log_level,
        };
        thread::spawn(move || {
            writer.run(&jobs_receiver);
            drop(exited_sender);
        });

        Self {
            jobs: Some(jobs),
            dropped,
            progress,
            writer_exited: Mutex::new(writer_exited),
            shutdown_timeout: config.shutdown_timeout,
        }
    }

    fn send(&self, job: SerializeJob) {
        let queued = self
            .jobs
            .as_ref()
            .is_some_and(|jobs| jobs.try_send(job).is_ok());
        if !queued {
            self.dropped.fetch_add(1, Ordering::Relaxed);
        }
    }
}

impl Drop for AsyncSink {
    fn drop(&mut self) {
        drop(self.jobs.take());
        let writer_exited = self
            .writer_exited
            .get_mut()
            .unwrap_or_else(PoisonError::into_inner);
        // The queue is closed, so a writer that keeps making progress finishes.
        let mut last_progress = self.progress.load(Ordering::Relaxed);
        loop {
            match writer_exited.recv_timeout(self.shutdown_timeout) {
                Ok(never) => match never {},
                Err(RecvTimeoutError::Disconnected) => break,
                Err(RecvTimeoutError::Timeout) => {
                    let progress = self.progress.load(Ordering::Relaxed);
                    if progress == last_progress {
                        // Blocked: detach the writer.
                        break;
                    }
                    last_progress = progress;
                },
            }
        }
    }
}

/// The writer thread's state: lines waiting to be written and when they must be.
struct QueueWriter {
    out: BoxedWriter,
    buffer: String,
    buffer_limit: usize,
    flush_timeout: Duration,
    /// Set while the buffer holds lines, and not moved by later lines, so a steady
    /// stream of entries cannot postpone the flush.
    flush_deadline: Option<Instant>,
    idle_report_interval: Duration,
    dropped: Arc<AtomicU64>,
    progress: Arc<AtomicU64>,
    log_level: LogLevel,
}

impl QueueWriter {
    fn run(mut self, jobs: &Receiver<SerializeJob>) {
        loop {
            let wait = match self.flush_deadline {
                None => self.idle_report_interval,
                Some(deadline) => deadline.saturating_duration_since(Instant::now()),
            };
            let next = jobs.recv_timeout(wait);

            // Written by the writer itself, so the report cannot be dropped too.
            if let Some(report) = dropped_report(self.dropped.swap(0, Ordering::Relaxed)) {
                self.push_entry(&report);
            }

            match next {
                Ok(serialize) => {
                    self.push_line(&serialize());
                    self.made_progress();
                },
                Err(RecvTimeoutError::Timeout) => {},
                Err(RecvTimeoutError::Disconnected) => break,
            }

            self.flush_if_due();
        }
        self.flush();
    }

    fn push_entry<T: Loggable>(&mut self, entry: &T) {
        if entry.can_log(self.log_level) {
            self.push_line(&LogJson::from_loggable(entry));
        }
    }

    fn push_line(&mut self, json: &LogJson) {
        self.buffer.push_str(json.as_str());
        self.buffer.push('\n');
    }

    fn flush_if_due(&mut self) {
        if self.buffer.is_empty() {
            return;
        }
        let now = Instant::now();
        let deadline = *self.flush_deadline.get_or_insert(now + self.flush_timeout);
        if self.buffer.len() >= self.buffer_limit || now >= deadline {
            self.flush();
        }
    }

    fn flush(&mut self) {
        if !self.buffer.is_empty() {
            for chunk in self.buffer.as_bytes().chunks(WRITE_CHUNK) {
                if self.out.write_all(chunk).is_err() {
                    break;
                }
                self.made_progress();
            }
            self.buffer.clear();
        }
        self.flush_deadline = None;
    }

    fn made_progress(&self) {
        self.progress.fetch_add(1, Ordering::Relaxed);
    }
}

/// The WARN entry reporting `count` dropped entries, if any were dropped.
fn dropped_report(count: u64) -> Option<LogEntry> {
    (count > 0).then(|| {
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
            LogLevel::WARN,
            None,
        ))
        .set_message(format!(
            "{count} log entries were dropped because the stdout log queue was full"
        ))
    })
}

impl StdOutLogger {
    // is used as fallback in memory logger, so default is immediate mode
    pub(crate) fn new(log_level: LogLevel, mode: StdOutLoggerMode) -> Self {
        Self::new_inner(Box::new(std::io::stdout()), log_level, mode)
    }

    // Create a new StdOutLogger with custom writer.
    // is used in tests.
    #[cfg(test)]
    pub(crate) fn new_with(
        writer: BoxedWriter,
        log_level: LogLevel,
        mode: StdOutLoggerMode,
    ) -> Self {
        Self::new_inner(writer, log_level, mode)
    }

    fn new_inner(writer: BoxedWriter, log_level: LogLevel, mode: StdOutLoggerMode) -> Self {
        let sink = match mode {
            StdOutLoggerMode::Immediate => Sink::Immediate(Mutex::new(writer)),
            StdOutLoggerMode::Async {
                timeout_millis,
                buffer_limit,
            } => Sink::Async(AsyncSink::spawn(
                writer,
                log_level,
                AsyncConfig::new(timeout_millis, buffer_limit),
            )),
        };
        Self { log_level, sink }
    }
}

// Implementation of LogWriter
impl LogWriter for StdOutLogger {
    fn log_any<T: Loggable>(&self, entry: T) {
        if !entry.can_log(self.log_level) {
            // do nothing
            return;
        }

        match &self.sink {
            Sink::Immediate(writer) => {
                let json = LogJson::from_loggable(&entry);
                // A poisoned lock still holds a usable writer; logging must not panic.
                let mut writer = writer.lock().unwrap_or_else(PoisonError::into_inner);
                let _ = writer.write_all(json.as_str().as_bytes());
                let _ = writer.write_all(b"\n");
            },
            Sink::Async(sink) => sink.send(Box::new(move || LogJson::from_loggable(&entry))),
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

// Test writer created for mocking LogWriter
#[cfg(test)]
#[derive(Clone)]
pub(crate) struct TestWriter {
    buf: Arc<Mutex<Vec<u8>>>,
}

#[cfg(test)]
impl TestWriter {
    pub(crate) fn new() -> Self {
        Self {
            buf: Arc::new(Mutex::new(Vec::new())),
        }
    }

    pub(crate) fn into_inner_buf(self) -> String {
        let buf = self.buf.lock().unwrap();
        String::from_utf8_lossy(buf.as_slice()).into_owned()
    }

    pub(crate) fn get_buf_contents(&self) -> String {
        let buf = self.buf.lock().unwrap();
        String::from_utf8_lossy(buf.as_slice()).into_owned()
    }
}

#[cfg(test)]
impl Write for TestWriter {
    fn write(&mut self, buf: &[u8]) -> std::io::Result<usize> {
        self.buf.lock().unwrap().extend_from_slice(buf);
        Ok(buf.len())
    }

    fn flush(&mut self) -> std::io::Result<()> {
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use std::io::Write;

    use serde_json::json;

    use super::*;
    use crate::common::app_types::PdpID;
    use crate::log::gen_uuid7;
    use crate::log::log_strategy::LogEntryWithClientInfo;

    #[test]
    fn write_log_ok() {
        let pdp_id = PdpID::new();
        let app_name = None;
        // Create a log entry
        let log_entry = LogEntry {
            base: BaseLogEntry::new_decision(gen_uuid7()),
            auth_info: None,
            msg: "Test message".to_string(),
            error_msg: None,
            cedar_lang_version: None,
            cedar_sdk_version: None,
            build_commit: None,
            build_timestamp: None,
            batch_id: None,
        };
        let log_entry =
            LogEntryWithClientInfo::from_loggable(log_entry.clone(), pdp_id, app_name.clone());

        // Serialize the log entry to JSON
        let json_str = json!(log_entry).to_string();

        // Create a test writer
        let test_writer = TestWriter::new();
        let buffer = Box::new(test_writer.clone()) as Box<dyn Write + Send + Sync + 'static>;

        // Create logger with test writer
        let logger = StdOutLogger::new_with(buffer, LogLevel::TRACE, StdOutLoggerMode::Immediate);

        // Log the entry
        logger.log_any(log_entry);

        // Drop logger to ensure writer thread finishes
        drop(logger);

        // Check logged content
        let logged_content = test_writer.into_inner_buf();

        // Verify that the log entry was logged correctly
        assert_eq!(logged_content, json_str + "\n");
    }

    #[test]
    fn flush_by_timeout_even_with_continuous_messages() {
        use std::thread;

        let pdp_id = PdpID::new();
        let app_name = None;

        // Create a test writer
        let test_writer = TestWriter::new();
        let buffer = Box::new(test_writer.clone()) as Box<dyn Write + Send + Sync + 'static>;

        // Use a short timeout for testing (20ms)
        let flush_timeout_u64 = 20;
        let flush_timeout = Duration::from_millis(flush_timeout_u64);
        let logger = StdOutLogger::new_with(
            buffer,
            LogLevel::TRACE,
            StdOutLoggerMode::Async {
                timeout_millis: flush_timeout_u64,
                buffer_limit: StdOutLoggerMode::DEFAULT_BUFFER_LIMIT,
            },
        );

        // Create first log entry
        let log_entry1 = LogEntry {
            base: BaseLogEntry::new_decision(gen_uuid7()),
            auth_info: None,
            msg: "First message".to_string(),
            error_msg: None,
            cedar_lang_version: None,
            cedar_sdk_version: None,
            build_commit: None,
            build_timestamp: None,
            batch_id: None,
        };

        // Create second log entry
        let log_entry2 = LogEntry {
            base: BaseLogEntry::new_decision(gen_uuid7()),
            auth_info: None,
            msg: "Second message".to_string(),
            error_msg: None,
            cedar_lang_version: None,
            cedar_sdk_version: None,
            build_commit: None,
            build_timestamp: None,
            batch_id: None,
        };

        // Log first entry
        logger.log_any(log_entry1.clone());

        // Wait for half the timeout - buffer may still be empty or may have flushed early
        thread::sleep(flush_timeout / 2);
        let buf_contents = test_writer.get_buf_contents();
        if !buf_contents.is_empty() {
            // Flush may have happened early, ensure it contains the first message
            let expected_json = json!(log_entry1).to_string() + "\n";
            assert!(
                buf_contents.starts_with(&expected_json),
                "If buffer not empty, it should start with first message, got: {buf_contents}"
            );
        } // else buffer is empty, which is also valid

        logger.log_any(log_entry2);

        // Wait for the full timeout plus small margin (timer resets on each log, so wait full timeout after entry2)
        thread::sleep(flush_timeout + Duration::from_millis(5));

        // Buffer should now contain the first message (may also contain second message)
        let expected_json = json!(log_entry1).to_string() + "\n";
        let buf_contents = test_writer.get_buf_contents();
        assert!(
            buf_contents.starts_with(&expected_json),
            "First message should appear after flush timeout, got: {buf_contents}"
        );

        // Now continuously send messages at intervals shorter than timeout
        for i in 0..5 {
            let log_entry = LogEntry {
                base: BaseLogEntry::new_decision(gen_uuid7()),
                auth_info: None,
                msg: format!("Continuous message {i}"),
                error_msg: None,
                cedar_lang_version: None,
                cedar_sdk_version: None,
                build_commit: None,
                build_timestamp: None,
                batch_id: None,
            };
            let log_entry =
                LogEntryWithClientInfo::from_loggable(log_entry.clone(), pdp_id, app_name.clone());

            logger.log_any(log_entry);

            // Wait shorter than timeout
            thread::sleep(flush_timeout / 4);

            // Verify that previous continuous messages might not be flushed yet,
            // but they shouldn't block the flush of the first message (already verified)
        }

        // Wait for final flush
        thread::sleep(flush_timeout * 2);

        // Clean up logger
        drop(logger);

        // Verify that all messages eventually arrived
        let final_content = test_writer.into_inner_buf();
        assert!(
            final_content.contains("First message"),
            "First message should still be in final output"
        );
        for i in 0..5 {
            assert!(
                final_content.contains(&format!("Continuous message {i}")),
                "Continuous message {i} should be in final output"
            );
        }
    }

    /// Lets the test hold the writer thread inside a write.
    struct Gate {
        /// Dropping it releases every write, now and later.
        open: mpsc::Sender<()>,
        /// Receives once, when the first write has started.
        first_write_started: Receiver<()>,
    }

    /// Writes to a [`TestWriter`], each write blocked until the [`Gate`] opens.
    struct GatedWriter {
        inner: TestWriter,
        gate: Mutex<Receiver<()>>,
        first_write_started: Mutex<Option<mpsc::Sender<()>>>,
    }

    struct Gated {
        writer: GatedWriter,
        gate: Gate,
    }

    fn gated(inner: TestWriter) -> Gated {
        let (open, gate) = mpsc::channel();
        let (started_sender, first_write_started) = mpsc::channel();
        Gated {
            writer: GatedWriter {
                inner,
                gate: Mutex::new(gate),
                first_write_started: Mutex::new(Some(started_sender)),
            },
            gate: Gate {
                open,
                first_write_started,
            },
        }
    }

    impl Write for GatedWriter {
        fn write(&mut self, buf: &[u8]) -> std::io::Result<usize> {
            if let Some(started) = self.first_write_started.lock().unwrap().take() {
                let _ = started.send(());
            }
            // Returns once the gate's sender is dropped.
            let _ = self.gate.lock().unwrap().recv();
            self.inner.write(buf)
        }

        fn flush(&mut self) -> std::io::Result<()> {
            Ok(())
        }
    }

    /// Writes to a [`TestWriter`], each write taking `delay`.
    struct SlowWriter {
        inner: TestWriter,
        delay: Duration,
    }

    impl Write for SlowWriter {
        fn write(&mut self, buf: &[u8]) -> std::io::Result<usize> {
            thread::sleep(self.delay);
            self.inner.write(buf)
        }

        fn flush(&mut self) -> std::io::Result<()> {
            Ok(())
        }
    }

    struct PanickingWriter;

    impl Write for PanickingWriter {
        fn write(&mut self, _: &[u8]) -> std::io::Result<usize> {
            panic!("writer thread fails");
        }

        fn flush(&mut self) -> std::io::Result<()> {
            Ok(())
        }
    }

    /// Writes every line as soon as it is queued.
    fn unbuffered(queue_capacity: usize, shutdown_timeout: Duration) -> AsyncConfig {
        AsyncConfig {
            flush_timeout: Duration::from_secs(60),
            buffer_limit: 1,
            queue_capacity,
            shutdown_timeout,
            idle_report_interval: Duration::from_secs(60),
        }
    }

    fn async_logger(writer: BoxedWriter, log_level: LogLevel, config: AsyncConfig) -> StdOutLogger {
        StdOutLogger {
            log_level,
            sink: Sink::Async(AsyncSink::spawn(writer, log_level, config)),
        }
    }

    fn async_sink(logger: &StdOutLogger) -> &AsyncSink {
        let Sink::Async(sink) = &logger.sink else {
            panic!("the logger should be in async mode");
        };
        sink
    }

    fn system_entry(level: LogLevel, msg: &str) -> LogEntry {
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(level, None))
            .set_message(msg.to_string())
    }

    fn written_messages(output: &str) -> Vec<String> {
        output
            .lines()
            .map(|line| {
                let value: serde_json::Value =
                    serde_json::from_str(line).expect("each written line should be JSON");
                value["msg"]
                    .as_str()
                    .expect("each written line should have a message")
                    .to_string()
            })
            .collect()
    }

    #[test]
    fn full_queue_drops_entries_and_reports_their_count() {
        let output = TestWriter::new();
        let Gated { writer, gate } = gated(output.clone());
        let logger = async_logger(
            Box::new(writer),
            LogLevel::TRACE,
            unbuffered(2, Duration::from_secs(5)),
        );

        logger.log_any(system_entry(LogLevel::INFO, "written"));
        gate.first_write_started
            .recv()
            .expect("the writer should start writing the first entry");
        // The writer is blocked, so these fill the queue and the rest are dropped.
        for msg in [
            "queued 1",
            "queued 2",
            "dropped 1",
            "dropped 2",
            "dropped 3",
        ] {
            logger.log_any(system_entry(LogLevel::INFO, msg));
        }
        assert_eq!(
            async_sink(&logger).dropped.load(Ordering::Relaxed),
            3,
            "entries that do not fit in the queue should be counted"
        );

        drop(gate.open);
        drop(logger);

        assert_eq!(
            written_messages(&output.into_inner_buf()),
            [
                "written",
                "3 log entries were dropped because the stdout log queue was full",
                "queued 1",
                "queued 2",
            ],
            "the next write should report the exact dropped count, and dropped entries \
             should not be written"
        );
    }

    #[test]
    fn dropped_count_report_obeys_log_level() {
        let output = TestWriter::new();
        let Gated { writer, gate } = gated(output.clone());
        let logger = async_logger(
            Box::new(writer),
            LogLevel::ERROR,
            unbuffered(1, Duration::from_secs(5)),
        );

        logger.log_any(system_entry(LogLevel::ERROR, "written"));
        gate.first_write_started
            .recv()
            .expect("the writer should start writing the first entry");
        logger.log_any(system_entry(LogLevel::ERROR, "queued"));
        logger.log_any(system_entry(LogLevel::ERROR, "dropped"));

        drop(gate.open);
        drop(logger);

        assert_eq!(
            written_messages(&output.into_inner_buf()),
            ["written", "queued"],
            "the WARN report should not be written when the log level is ERROR"
        );
    }

    #[test]
    fn log_after_writer_exited_does_not_panic() {
        let logger = async_logger(
            Box::new(PanickingWriter),
            LogLevel::TRACE,
            unbuffered(2, Duration::from_secs(5)),
        );

        logger.log_any(system_entry(LogLevel::INFO, "kills the writer"));
        let writer_exited = async_sink(&logger)
            .writer_exited
            .lock()
            .unwrap()
            .recv()
            .expect_err("the writer should never send");
        assert_eq!(
            writer_exited,
            mpsc::RecvError,
            "the writer should have exited"
        );

        logger.log_any(system_entry(LogLevel::INFO, "after the writer exited"));

        assert_eq!(
            async_sink(&logger).dropped.load(Ordering::Relaxed),
            1,
            "an entry logged after the writer exited should be counted as dropped"
        );
    }

    #[test]
    fn drop_with_blocked_writer_returns_after_timeout() {
        let shutdown_timeout = Duration::from_millis(50);
        let Gated { writer, gate } = gated(TestWriter::new());
        let logger = async_logger(
            Box::new(writer),
            LogLevel::TRACE,
            unbuffered(2, shutdown_timeout),
        );

        logger.log_any(system_entry(LogLevel::INFO, "blocks the writer"));
        gate.first_write_started
            .recv()
            .expect("the writer should start writing the first entry");

        let started = Instant::now();
        drop(logger);
        let elapsed = started.elapsed();

        assert!(
            elapsed >= shutdown_timeout,
            "drop should wait for the writer up to the timeout, waited {elapsed:?}"
        );
        assert!(
            elapsed < Duration::from_secs(5),
            "drop should not wait for a blocked writer past the timeout, waited {elapsed:?}"
        );
        drop(gate.open);
    }

    #[test]
    fn idle_writer_reports_pending_drop_count() {
        let output = TestWriter::new();
        let logger = async_logger(
            Box::new(output.clone()),
            LogLevel::TRACE,
            AsyncConfig {
                idle_report_interval: Duration::from_millis(10),
                ..unbuffered(2, Duration::from_secs(5))
            },
        );

        // What a caller leaves behind when its `try_send` fails and the writer has
        // already emptied the queue: a count with nothing queued to wake the writer.
        async_sink(&logger).dropped.fetch_add(2, Ordering::Relaxed);

        let started = Instant::now();
        while output.get_buf_contents().is_empty() && started.elapsed() < Duration::from_secs(5) {
            thread::sleep(Duration::from_millis(5));
        }

        assert_eq!(
            written_messages(&output.get_buf_contents()),
            ["2 log entries were dropped because the stdout log queue was full"],
            "an idle writer should report a pending drop count without another entry"
        );
    }

    #[test]
    fn drop_waits_for_slow_writer_that_makes_progress() {
        let output = TestWriter::new();
        let write_delay = Duration::from_millis(20);
        let shutdown_timeout = Duration::from_millis(200);
        let entries = 20;
        let logger = async_logger(
            Box::new(SlowWriter {
                inner: output.clone(),
                delay: write_delay,
            }),
            LogLevel::TRACE,
            unbuffered(entries, shutdown_timeout),
        );

        for i in 0..entries {
            logger.log_any(system_entry(LogLevel::INFO, &format!("entry {i}")));
        }
        assert!(
            write_delay * u32::try_from(entries).expect("entry count fits in u32")
                > shutdown_timeout,
            "draining should take longer than the shutdown timeout"
        );
        drop(logger);

        let expected: Vec<String> = (0..entries).map(|i| format!("entry {i}")).collect();
        assert_eq!(
            written_messages(&output.into_inner_buf()),
            expected,
            "drop should wait for every entry while the writer keeps making progress"
        );
    }
}
