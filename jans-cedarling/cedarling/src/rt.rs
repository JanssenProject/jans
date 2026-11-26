// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Runtime services that differ between native and WASM: timers and task spawning.
//! `clippy.toml` bans raw Tokio timers, spawns and `spawn_local` elsewhere, so use these.

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
        #[expect(
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

/// What [`run_steps`] hands back: the state after the last step, and one output
/// per item, in item order.
#[derive(Debug)]
pub(crate) struct Steps<S, O> {
    pub(crate) state: S,
    pub(crate) outputs: Vec<O>,
}

/// Runs `step` on each of `items` in order, threading `state` through, without
/// holding up other tasks: for CPU-heavy work such as signing or hashing.
///
/// On native targets the whole run goes to the blocking pool, and a panicking
/// step comes back as [`TaskEnded`]. WebAssembly has one thread, so the run stays
/// on it and hands the thread back to the browser event loop whenever a slice of
/// [`STEP_SLICE_MILLIS`] has passed; a panic there aborts the module, so the
/// result is always `Ok`.
pub(crate) async fn run_steps<S, I, O, F>(
    state: S,
    items: Vec<I>,
    step: F,
) -> Result<Steps<S, O>, TaskEnded>
where
    S: Send + 'static,
    I: Send + 'static,
    O: Send + 'static,
    F: FnMut(&mut S, I) -> O + Send + 'static,
{
    #[cfg(not(target_family = "wasm"))]
    {
        #[expect(
            clippy::disallowed_methods,
            reason = "native branch of the cross-platform wrapper"
        )]
        tokio::task::spawn_blocking(move || run_all(state, items, step))
            .await
            .map_err(|err| TaskEnded(err.to_string()))
    }

    #[cfg(target_family = "wasm")]
    {
        let mut state = state;
        let mut step = step;
        let mut outputs = Vec::with_capacity(items.len());
        let mut slice_start = chrono::Utc::now();
        for item in items {
            if (chrono::Utc::now() - slice_start).num_milliseconds() >= STEP_SLICE_MILLIS {
                yield_now().await;
                slice_start = chrono::Utc::now();
            }
            outputs.push(step(&mut state, item));
        }
        Ok(Steps { state, outputs })
    }
}

/// The steps of a native [`run_steps`], on a blocking pool thread.
#[cfg(not(target_family = "wasm"))]
fn run_all<S, I, O>(
    mut state: S,
    items: Vec<I>,
    mut step: impl FnMut(&mut S, I) -> O,
) -> Steps<S, O> {
    let outputs = items
        .into_iter()
        .map(|item| step(&mut state, item))
        .collect();
    Steps { state, outputs }
}

/// How long [`run_steps`] runs on WebAssembly before it hands the thread back.
/// Timed rather than counted, so a caller need not know what one step costs;
/// `chrono::Utc::now()` reads `Date.now()`, as `std::time::Instant` is not
/// available on `wasm32-unknown-unknown`. 4 ms is the shortest wait the browser
/// gives a nested zero timer, so yielding never waits longer than it worked.
#[cfg(target_family = "wasm")]
const STEP_SLICE_MILLIS: i64 = 4;

/// Hands the thread back to the browser event loop once. A zero timer rather
/// than a re-woken future: a wake only reorders the microtask queue, while a
/// timer lets the event loop run authorization calls and UI events.
#[cfg(target_family = "wasm")]
async fn yield_now() {
    sleep(Duration::ZERO).await;
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
    #[expect(
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

    #[expect(
        clippy::disallowed_methods,
        reason = "WASM branch of the cross-platform wrapper"
    )]
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

    #[cfg(not(target_family = "wasm"))]
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

    #[tokio::test]
    async fn run_steps_threads_state_and_keeps_item_order() {
        let Steps { state, outputs } = run_steps(0_u32, vec![1_u32, 2, 3], |sum, item| {
            *sum += item;
            *sum
        })
        .await
        .expect("steps that do not panic should finish");

        assert_eq!(
            state, 6,
            "each step should see the state the previous one left"
        );
        assert_eq!(outputs, vec![1, 3, 6], "outputs should follow item order");
    }

    #[tokio::test]
    async fn run_steps_runs_off_the_runtime_thread() {
        let runtime_thread = std::thread::current().id();
        let Steps { outputs, .. } =
            run_steps((), vec![(); 3], |(), ()| std::thread::current().id())
                .await
                .expect("steps that do not panic should finish");

        assert!(
            outputs.iter().all(|thread| *thread != runtime_thread),
            "steps should run on the blocking pool, not on the runtime thread"
        );
    }

    #[tokio::test]
    async fn run_steps_returns_a_panicking_step_as_task_ended() {
        let err = run_steps((), vec![1, 2], |(), item: u32| {
            assert!(item < 2, "test step panics");
        })
        .await
        .expect_err("a panicking step should end the run");

        assert!(
            err.to_string().contains("panicked"),
            "the error should say the task panicked: {err}"
        );
    }
}
