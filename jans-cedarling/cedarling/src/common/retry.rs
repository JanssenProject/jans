// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Repeating one request: how long to wait between attempts, and the one loop that
//! decides whether to repeat. Callers sort each failure and keep their own logging.

use std::future::Future;
use std::time::Duration;

use tokio_util::sync::CancellationToken;

use crate::rt::sleep;

/// The longest delay of [`Backoff::exponential`].
pub(crate) const MAX_EXPONENTIAL_DELAY: Duration = Duration::from_secs(300);

/// The delay before each retry, computed from the retry number alone, so it holds
/// no state and is copied rather than shared.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Backoff {
    /// Doubles from `base` up to `cap`.
    Exponential { base: Duration, cap: Duration },
}

impl Backoff {
    /// Doubles from `base` up to [`MAX_EXPONENTIAL_DELAY`] (or `base`, if longer), so a
    /// long outage is retried every few minutes rather than after delays of days.
    pub(crate) fn exponential(base: Duration) -> Self {
        Self::Exponential {
            base,
            cap: MAX_EXPONENTIAL_DELAY.max(base),
        }
    }

    /// The delay before retry number `retry`, `0` being the first.
    pub(crate) fn delay(self, retry: u32) -> Duration {
        match self {
            Self::Exponential { base, cap } => 2u32
                .checked_pow(retry)
                .and_then(|factor| base.checked_mul(factor))
                .map_or(cap, |delay| delay.min(cap)),
        }
    }
}

/// How often and how far apart a request is repeated.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) struct RetryPolicy {
    pub(crate) backoff: Backoff,
    pub(crate) max_retries: u32,
}

/// What one attempt says about the next one. The caller sorts its failure once;
/// [`retry`] only acts on the answer.
pub(crate) enum Attempt<T, R, F> {
    Done(T),
    /// Worth repeating; `wait_at_least` is how long the server asked to wait.
    Retryable {
        error: R,
        wait_at_least: Option<Duration>,
    },
    Final(F),
}

/// Why [`retry`] gave up: a `RetriesExhausted` holds only a failure worth repeating.
#[derive(Debug, thiserror::Error)]
pub(crate) enum RetryError<R, F> {
    #[error(transparent)]
    Final(F),
    #[error("{last} (gave up after {retries} retries)")]
    RetriesExhausted { retries: u32, last: R },
}

/// Runs `attempt` until it is done or final, or until `policy.max_retries` retries
/// failed, waiting [`RetryPolicy::backoff`] (and at least what the server asked)
/// before each retry.
pub(crate) async fn retry<T, R, F, Op, Fut>(
    policy: RetryPolicy,
    attempt: Op,
) -> Result<T, RetryError<R, F>>
where
    Op: FnMut() -> Fut,
    Fut: Future<Output = Attempt<T, R, F>>,
{
    repeat(policy, attempt, |delay| async move {
        sleep(delay).await;
        Ok(())
    })
    .await
}

/// Why [`retry_until_cancelled`] gave up.
#[derive(Debug, thiserror::Error)]
pub(crate) enum RetryUntilCancelledError<R, F> {
    #[error(transparent)]
    GaveUp(#[from] RetryError<R, F>),
    /// The caller stopped waiting for the next retry.
    #[error("cancelled while waiting to retry")]
    Cancelled,
}

/// [`retry`], giving up when `cancel` fires during a wait. An attempt already
/// sent is let finish, so a request in flight is never cut off.
pub(crate) async fn retry_until_cancelled<T, R, F, Op, Fut>(
    policy: RetryPolicy,
    cancel: &CancellationToken,
    attempt: Op,
) -> Result<T, RetryUntilCancelledError<R, F>>
where
    Op: FnMut() -> Fut,
    Fut: Future<Output = Attempt<T, R, F>>,
{
    repeat(policy, attempt, |delay| async move {
        // Cancellation wins a tie, so no attempt starts after it.
        tokio::select! {
            biased;
            () = cancel.cancelled() => Err(RetryUntilCancelledError::Cancelled),
            () = sleep(delay) => Ok(()),
        }
    })
    .await
}

/// The loop of [`retry`] and [`retry_until_cancelled`], which differ only in how
/// they wait.
async fn repeat<T, R, F, E, Op, Fut, Wait, WaitFut>(
    policy: RetryPolicy,
    mut attempt: Op,
    mut wait: Wait,
) -> Result<T, E>
where
    Op: FnMut() -> Fut,
    Fut: Future<Output = Attempt<T, R, F>>,
    Wait: FnMut(Duration) -> WaitFut,
    E: From<RetryError<R, F>>,
    WaitFut: Future<Output = Result<(), E>>,
{
    let mut retries = 0;
    loop {
        match attempt().await {
            Attempt::Done(value) => return Ok(value),
            Attempt::Final(error) => return Err(RetryError::Final(error).into()),
            Attempt::Retryable {
                error,
                wait_at_least,
            } => {
                if retries == policy.max_retries {
                    return Err(RetryError::RetriesExhausted {
                        retries,
                        last: error,
                    }
                    .into());
                }
                let delay = policy
                    .backoff
                    .delay(retries)
                    .max(wait_at_least.unwrap_or_default());
                wait(delay).await?;
                retries += 1;
            },
        }
    }
}

#[cfg(test)]
mod tests {
    use std::sync::atomic::{AtomicU32, Ordering};
    use std::time::Instant;

    use super::*;

    fn policy(max_retries: u32) -> RetryPolicy {
        RetryPolicy {
            backoff: Backoff::exponential(Duration::from_millis(1)),
            max_retries,
        }
    }

    #[test]
    fn exponential_delay_doubles_until_capped() {
        let backoff = Backoff::exponential(Duration::from_secs(10));
        let secs: Vec<u64> = (0..8).map(|retry| backoff.delay(retry).as_secs()).collect();
        assert_eq!(
            secs,
            [10, 20, 40, 80, 160, 300, 300, 300],
            "the delay doubles from the base up to the cap"
        );
        assert_eq!(
            backoff.delay(u32::MAX),
            MAX_EXPONENTIAL_DELAY,
            "a retry number past the overflow point is capped, not a panic"
        );
        let long = Backoff::exponential(Duration::from_secs(600));
        assert_eq!(
            long.delay(3),
            Duration::from_secs(600),
            "a base longer than the cap is the cap"
        );
    }

    #[tokio::test]
    async fn final_failure_makes_one_attempt() {
        let attempts = AtomicU32::new(0);
        let result: Result<(), RetryError<(), &str>> = retry(policy(3), || async {
            attempts.fetch_add(1, Ordering::SeqCst);
            Attempt::Final("refused")
        })
        .await;

        assert!(
            matches!(result, Err(RetryError::Final("refused"))),
            "a final failure is returned as is, got {result:?}"
        );
        assert_eq!(
            attempts.load(Ordering::SeqCst),
            1,
            "a final failure is not repeated"
        );
    }

    #[tokio::test]
    async fn retryable_failure_is_repeated_up_to_max_retries() {
        let attempts = AtomicU32::new(0);
        let result: Result<(), RetryError<u32, ()>> = retry(policy(3), || async {
            let attempt = attempts.fetch_add(1, Ordering::SeqCst);
            Attempt::Retryable {
                error: attempt,
                wait_at_least: None,
            }
        })
        .await;

        assert!(
            matches!(
                result,
                Err(RetryError::RetriesExhausted {
                    retries: 3,
                    last: 3
                })
            ),
            "the first attempt and 3 retries, reporting the last error, got {result:?}"
        );
        assert_eq!(
            attempts.load(Ordering::SeqCst),
            4,
            "1 attempt and 3 retries"
        );
    }

    #[tokio::test]
    async fn success_after_a_retry_is_returned() {
        let attempts = AtomicU32::new(0);
        let result: Result<&str, RetryError<(), ()>> = retry(policy(3), || async {
            if attempts.fetch_add(1, Ordering::SeqCst) == 0 {
                Attempt::Retryable {
                    error: (),
                    wait_at_least: None,
                }
            } else {
                Attempt::Done("sent")
            }
        })
        .await;

        assert!(
            matches!(result, Ok("sent")),
            "the retry succeeded, got {result:?}"
        );
        assert_eq!(attempts.load(Ordering::SeqCst), 2, "1 attempt and 1 retry");
    }

    #[tokio::test]
    async fn waits_at_least_what_the_server_asked() {
        let asked = Duration::from_millis(80);
        let attempts = AtomicU32::new(0);
        let started = Instant::now();
        let result: Result<(), RetryError<(), ()>> = retry(policy(1), || async {
            attempts.fetch_add(1, Ordering::SeqCst);
            Attempt::Retryable {
                error: (),
                wait_at_least: Some(asked),
            }
        })
        .await;

        assert!(
            matches!(result, Err(RetryError::RetriesExhausted { retries: 1, .. })),
            "one retry, got {result:?}"
        );
        assert!(
            started.elapsed() >= asked,
            "the 1 ms backoff gives way to the server's {asked:?}, waited {:?}",
            started.elapsed()
        );
    }

    #[tokio::test]
    async fn cancellation_stops_the_wait_but_not_an_attempt() {
        let cancel = CancellationToken::new();
        let attempts = AtomicU32::new(0);
        let long_wait = RetryPolicy {
            backoff: Backoff::exponential(Duration::from_secs(60)),
            max_retries: 3,
        };
        let started = Instant::now();
        let result: Result<(), RetryUntilCancelledError<(), ()>> =
            retry_until_cancelled(long_wait, &cancel, || async {
                attempts.fetch_add(1, Ordering::SeqCst);
                // Cancelled while this attempt is in flight: it still completes.
                cancel.cancel();
                tokio::task::yield_now().await;
                Attempt::Retryable {
                    error: (),
                    wait_at_least: None,
                }
            })
            .await;

        assert!(
            matches!(result, Err(RetryUntilCancelledError::Cancelled)),
            "cancelled during the wait, got {result:?}"
        );
        assert_eq!(
            attempts.load(Ordering::SeqCst),
            1,
            "the attempt finished and no retry was made"
        );
        assert!(
            started.elapsed() < Duration::from_secs(1),
            "the 60 s wait was cut short, waited {:?}",
            started.elapsed()
        );
    }
}
