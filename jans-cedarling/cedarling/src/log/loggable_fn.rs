use crate::log::{
    BaseLogEntry,
    interface::{Indexed, Loggable, can_log},
};

/// LoggableFn is a helper struct to create loggable entries using a closure.
///
/// Example usage:
/// ```rust
/// let loggable_entry = LoggableFn::new(|base| MyLogEntry {
///     base,
///     field1: "value1".to_string(),
///     field2: 42,
/// });
/// let log_entry = loggable_entry.build();
#[derive(Clone)]
pub(crate) struct LoggableFn<F> {
    base: BaseLogEntry,
    builder: F,
}

impl<F, R> LoggableFn<F>
where
    R: Loggable + Indexed,
    for<'a> F: Fn(BaseLogEntry) -> R,
{
    pub(crate) fn new(base: BaseLogEntry, builder: F) -> Self {
        Self { base, builder }
    }

    pub(crate) fn build(self) -> R {
        (self.builder)(self.base)
    }
}

impl<F, R> LoggableFn<F>
where
    R: Loggable + Indexed,
    for<'a> F: Fn(&'a BaseLogEntry) -> R,
{
    fn get_log_level(&self) -> Option<super::LogLevel> {
        self.base.get_log_level()
    }

    fn can_log(&self, logger_level: super::LogLevel) -> bool {
        can_log(self.get_log_level(), logger_level)
    }
}

/// Implementation serde::Serialize for LoggableFn
///
/// Implemented only to satisfy the requirement that Loggable extends serde::Serialize.
/// But better to use `build` method to get the actual loggable entry and serialize it.
impl<F, R> serde::Serialize for LoggableFn<F>
where
    R: Loggable + Indexed + serde::Serialize,
    for<'a> F: Fn(BaseLogEntry) -> R,
{
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        let log = (self.builder)(self.base.clone());
        log.serialize(serializer)
    }
}
