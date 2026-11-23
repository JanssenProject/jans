// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cross-platform async timers for background tasks.
//!
//! Tokio timers require a Tokio runtime; browser WASM builds use [`gloo_timers`] instead.
//! `clippy.toml` bans the Tokio timers elsewhere, so every timer goes through this module.

use std::pin::Pin;
use std::time::Duration;

/// Future returned by [`sleep`].
#[cfg(not(target_family = "wasm"))]
pub(crate) type Sleep = tokio::time::Sleep;

/// Future returned by [`sleep`].
#[cfg(target_family = "wasm")]
pub(crate) type Sleep = gloo_timers::future::TimeoutFuture;

/// Completes after approximately `duration`.
///
/// On native targets this uses [`tokio::time::sleep`]. On WebAssembly it uses a browser
/// timer so background work (for example status list refresh) does not require a Tokio
/// reactor.
pub(crate) fn sleep(duration: Duration) -> Sleep {
    #[cfg(not(target_family = "wasm"))]
    {
        #[allow(
            clippy::disallowed_methods,
            reason = "native branch of the cross-platform wrapper"
        )]
        tokio::time::sleep(duration)
    }

    #[cfg(target_family = "wasm")]
    {
        let millis = duration.as_millis().min(u128::from(u32::MAX));
        #[allow(clippy::cast_possible_truncation)]
        let millis: u32 = millis as u32;
        gloo_timers::future::TimeoutFuture::new(millis)
    }
}

/// Fires every `period`, starting one `period` after creation.
///
/// Replaces `tokio::time::interval` with `MissedTickBehavior::Delay`, which needs a Tokio
/// timer. The deadline lives in the ticker, not in the [`Ticker::tick`] future, so a
/// `tokio::select!` that drops an unfinished tick for another branch does not push the
/// next tick back.
pub(crate) struct Ticker {
    period: Duration,
    next: Pin<Box<Sleep>>,
}

impl Ticker {
    pub(crate) fn new(period: Duration) -> Self {
        Self {
            period,
            next: Box::pin(sleep(period)),
        }
    }

    /// Waits for the next deadline. A tick that comes late starts the next period from
    /// when it fired, so ticks never burst to catch up.
    pub(crate) async fn tick(&mut self) {
        self.next.as_mut().await;
        // No await between the deadline passing and the reset: a cancelled tick keeps
        // the old deadline, a finished one always has a new one.
        self.next.set(sleep(self.period));
    }
}

#[cfg(test)]
mod tests {
    use std::future::Future;
    use std::task::{Context, Poll, Waker};

    use super::*;

    fn poll_once<F: Future>(fut: Pin<&mut F>) -> Poll<F::Output> {
        fut.poll(&mut Context::from_waker(Waker::noop()))
    }

    #[tokio::test(start_paused = true)]
    async fn ticker_fires_one_period_apart() {
        let period = Duration::from_secs(10);
        let start = tokio::time::Instant::now();
        let mut ticker = Ticker::new(period);

        ticker.tick().await;
        assert_eq!(
            start.elapsed(),
            period,
            "first tick should come one period after creation"
        );

        ticker.tick().await;
        assert_eq!(
            start.elapsed(),
            period * 2,
            "second tick should come one period later"
        );
    }

    #[tokio::test(start_paused = true)]
    async fn cancelled_tick_keeps_its_deadline() {
        let period = Duration::from_secs(10);
        let start = tokio::time::Instant::now();
        let mut ticker = Ticker::new(period);

        tokio::time::advance(Duration::from_secs(6)).await;
        {
            let tick = std::pin::pin!(ticker.tick());
            assert!(
                poll_once(tick).is_pending(),
                "tick should not fire before the deadline"
            );
        }

        ticker.tick().await;
        assert_eq!(
            start.elapsed(),
            period,
            "a dropped tick future must not restart the period"
        );
    }

    #[tokio::test(start_paused = true)]
    async fn late_tick_delays_the_next_one() {
        let period = Duration::from_secs(10);
        let start = tokio::time::Instant::now();
        let mut ticker = Ticker::new(period);

        tokio::time::advance(Duration::from_secs(25)).await;
        ticker.tick().await;
        assert_eq!(
            start.elapsed(),
            Duration::from_secs(25),
            "late tick fires at once"
        );

        ticker.tick().await;
        assert_eq!(
            start.elapsed(),
            Duration::from_secs(35),
            "next tick should come one period after the late one, not burst"
        );
    }
}
