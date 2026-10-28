// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;
use tokio::time::sleep;

/// The longest delay of [`Backoff::new_exponential`].
pub(crate) const MAX_EXPONENTIAL_DELAY: Duration = Duration::from_secs(300);

/// Implements a backoff strategy for retrying operations.
pub(crate) struct Backoff {
    attempts: u32,
    max_attempts: Option<u32>,
    #[allow(clippy::struct_field_names)]
    backoff_duration: Box<dyn Fn(u32) -> Duration + Send + Sync>,
}

impl Backoff {
    /// Creates an exponential backoff strategy: the delay doubles from
    /// `base_delay` up to [`MAX_EXPONENTIAL_DELAY`] (or `base_delay`, if
    /// longer), so a long outage is retried every few minutes rather than
    /// after delays of days, and a high attempt count cannot overflow.
    ///
    /// Set `max_attempts` to [`None`] to retry indefinitely.
    #[must_use]
    pub(crate) fn new_exponential(base_delay: Duration, max_attempts: Option<u32>) -> Self {
        let cap = MAX_EXPONENTIAL_DELAY.max(base_delay);
        let backoff_dur_calc = Box::new(move |attempts| {
            2u32.checked_pow(attempts)
                .and_then(|factor| base_delay.checked_mul(factor))
                .map_or(cap, |delay| delay.min(cap))
        });
        Self {
            attempts: 0,
            max_attempts,
            backoff_duration: backoff_dur_calc,
        }
    }

    /// Creates an fixed backoff strategy
    ///
    /// Set `max_attempts` to [`None`] to retry indefinitely.
    #[must_use]
    pub(crate) fn new_fixed(base_delay: Duration, max_attempts: Option<u32>) -> Self {
        let backoff_dur_calc = Box::new(move |_| base_delay);
        Self {
            attempts: 0,
            max_attempts,
            backoff_duration: backoff_dur_calc,
        }
    }

    /// Creates an fixed backoff strategy with the following configuratoin:
    ///
    /// - `max_attempts`: `None` (retries indefinitely)
    /// - `base_delay`: `10` seconds (the delay before the first retry)
    #[must_use]
    pub(crate) fn default_fixed() -> Self {
        Self::new_fixed(Duration::from_secs(10), None)
    }

    pub(crate) async fn snooze(&mut self) -> Result<(), ()> {
        self.snooze_at_least(Duration::ZERO).await
    }

    /// Like [`Self::snooze`], but waits at least `floor`: as long as a server
    /// asked with `Retry-After`, while a shorter request keeps the backoff.
    pub(crate) async fn snooze_at_least(&mut self, floor: Duration) -> Result<(), ()> {
        let backoff_duration = (self.backoff_duration)(self.attempts).max(floor);
        self.attempts += 1;

        if let Some(max_attempts) = self.max_attempts
            && self.attempts > max_attempts
        {
            return Err(());
        }

        sleep(backoff_duration).await;
        Ok(())
    }

    /// Counts an attempt and returns the delay before it, without waiting:
    /// for a caller that waits itself, for example while watching for
    /// cancellation. `max_attempts` is not checked.
    #[cfg(not(target_arch = "wasm32"))]
    pub(crate) fn next_delay(&mut self) -> Duration {
        let delay = (self.backoff_duration)(self.attempts);
        self.attempts = self.attempts.saturating_add(1);
        delay
    }

    /// Resets `attempts` to `0`.
    pub(crate) fn reset(&mut self) {
        self.attempts = 0;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn delays(backoff: &Backoff, attempts: impl IntoIterator<Item = u32>) -> Vec<Duration> {
        attempts
            .into_iter()
            .map(|attempt| (backoff.backoff_duration)(attempt))
            .collect()
    }

    #[test]
    fn exponential_delay_doubles_until_capped() {
        let backoff = Backoff::new_exponential(Duration::from_secs(10), None);
        let secs: Vec<u64> = delays(&backoff, 0..8)
            .iter()
            .map(Duration::as_secs)
            .collect();
        assert_eq!(
            secs,
            [10, 20, 40, 80, 160, 300, 300, 300],
            "the delay doubles from the base, then stays at five minutes"
        );
    }

    #[test]
    fn exponential_delay_does_not_overflow() {
        let backoff = Backoff::new_exponential(Duration::from_secs(10), None);
        assert_eq!(
            delays(&backoff, [31, 32, 64, u32::MAX]),
            [Duration::from_secs(300); 4],
            "a high attempt count waits the capped delay, never zero"
        );
    }

    #[test]
    fn exponential_cap_does_not_shorten_a_longer_base() {
        let base = Duration::from_secs(600);
        let backoff = Backoff::new_exponential(base, None);
        assert_eq!(
            delays(&backoff, [0, 3]),
            [base, base],
            "a base delay above the cap is kept"
        );
    }
}
