// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Runtime services that differ between native and WASM: timers and task spawning.
//! `clippy.toml` bans Tokio timers and `tokio::spawn` elsewhere, so they go through here.

use std::future::Future;
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

/// Helper function for spawning async tasks
///
/// Use this instead of the following:
/// - [`tokio::spawn`]
/// - `wasm_bindgen_futures::spawn_local`
#[cfg(not(target_family = "wasm"))]
pub fn spawn_task<F>(future: F) -> JoinHandle<F::Output>
where
    F: Future + Send + 'static,
    F::Output: Send + 'static,
{
    #[allow(
        clippy::disallowed_methods,
        reason = "native branch of the cross-platform wrapper"
    )]
    let handle = tokio::spawn(future);
    JoinHandle { handle }
}

/// Helper function for spawning async tasks
///
/// Use this instead of the following:
/// - [`tokio::spawn`]
/// - [`wasm_bindgen_futures::spawn_local`]
#[cfg(target_family = "wasm")]
pub fn spawn_task<F>(future: F) -> JoinHandle<F::Output>
where
    F: Future + 'static,
    F::Output: Send + 'static,
{
    let (tx, rx) = futures::channel::oneshot::channel();

    wasm_bindgen_futures::spawn_local({
        async move {
            let result = future.await;
            _ = tx.send(result);
        }
    });

    JoinHandle { receiver: rx }
}

/// A spawned task that did not finish: it panicked or was cancelled.
#[derive(Debug, thiserror::Error)]
#[error("{0}")]
pub struct TaskEnded(String);

/// This is a helper struct for managing the async handles.
///
/// This is needed because the WASM bindings need special treatment.
#[derive(Debug)]
pub struct JoinHandle<T>
where
    T: Send + 'static,
{
    #[cfg(not(target_family = "wasm"))]
    handle: tokio::task::JoinHandle<T>,

    #[cfg(target_family = "wasm")]
    receiver: futures::channel::oneshot::Receiver<T>,
}

impl<T> JoinHandle<T>
where
    T: Send + 'static,
{
    /// Like [`Self::await_result`], for a caller that must not fail with the
    /// task: an error says why it did not finish.
    pub async fn try_await_result(self) -> Result<T, TaskEnded> {
        #[cfg(not(target_family = "wasm"))]
        {
            self.handle.await.map_err(|err| TaskEnded(err.to_string()))
        }

        #[cfg(target_family = "wasm")]
        {
            self.receiver
                .await
                .map_err(|_| TaskEnded("the task was dropped before completing".to_string()))
        }
    }

    pub async fn await_result(self) -> T {
        #[cfg(not(target_family = "wasm"))]
        {
            self.handle.await.expect("Task panicked")
        }

        #[cfg(target_family = "wasm")]
        {
            self.receiver
                .await
                .expect("Task was dropped before completing")
        }
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

    #[tokio::test]
    async fn cancelled_task_is_an_error_not_a_panic() {
        let join = spawn_task(std::future::pending::<()>());
        join.handle.abort();
        let err = join
            .try_await_result()
            .await
            .expect_err("a cancelled task did not finish");
        assert!(
            err.to_string().contains("cancelled"),
            "the error says the task was cancelled: {err}"
        );
    }
}
