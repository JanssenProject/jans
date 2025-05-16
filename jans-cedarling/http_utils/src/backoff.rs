// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::time::Duration;
use tokio::time::sleep;

/// Implements a backoff strategy for retrying operations.
pub struct Backoff {
    attempts: u32,
    max_attempts: Option<u32>,
    backoff_duration: Box<dyn Fn(u32) -> Duration>,
}

impl Backoff {
    /// Creates an exponenital backoff strategy
    ///
    /// Set `max_attempts` to [`None`] to retry indefinitely.
    pub fn new_exponential(base_delay: Duration, max_attempts: Option<u32>) -> Self {
        let backoff_dur_calc = Box::new(move |attempts| base_delay * 2u32.pow(attempts));
        Self {
            attempts: 0,
            max_attempts,
            backoff_duration: backoff_dur_calc,
        }
    }

    /// Creates an fixed backoff strategy
    ///
    /// Set `max_attempts` to [`None`] to retry indefinitely.
    pub fn new_fixed(base_delay: Duration, max_attempts: Option<u32>) -> Self {
        let backoff_dur_calc = Box::new(move |_| base_delay);
        Self {
            attempts: 0,
            max_attempts,
            backoff_duration: backoff_dur_calc,
        }
    }

    /// Creates an exponential backoff strategy with the following configuratoin:
    ///
    /// - `max_attempts`: `Some(3)` (retries up to three times before failing)
    /// - `base_delay`: `3` seconds (the delay before the first retry)
    pub fn default_exponential() -> Self {
        Self::new_exponential(Duration::from_secs(3), Some(3))
    }

    /// Creates an fixed backoff strategy with the following configuratoin:
    ///
    /// - `max_attempts`: `None` (retries indefinitely)
    /// - `base_delay`: `10` seconds (the delay before the first retry)
    pub fn default_fixed() -> Self {
        Self::new_fixed(Duration::from_secs(10), None)
    }

    pub async fn snooze(&mut self) -> Result<(), ()> {
        let backoff_duration = (self.backoff_duration)(self.attempts);
        self.attempts += 1;

        if let Some(max_attempts) = self.max_attempts {
            if self.attempts > max_attempts {
                return Err(());
            }
        }

        sleep(backoff_duration).await;
        Ok(())
    }

    /// Resets `attempts` to `0`.
    pub fn reset(&mut self) {
        self.attempts = 0;
    }
}
