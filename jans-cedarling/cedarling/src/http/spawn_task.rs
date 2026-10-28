// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// This module is public, to use in unit tests.
// It allows to verify check that `spawn_task` works correctly in WASM environment.
#![allow(unreachable_pub)]

use std::future::Future;

/// Helper function for spawning async tasks
///
/// Use this instead of the following:
/// - [`tokio::spawn`]
/// - [`wasm_bindgen_futures::spawn_local`]
#[cfg(not(any(target_arch = "wasm32", target_arch = "wasm64")))]
pub fn spawn_task<F>(future: F) -> JoinHandle<F::Output>
where
    F: Future + Send + 'static,
    F::Output: Send + 'static,
{
    let handle = tokio::spawn(future);
    JoinHandle { handle }
}

/// Helper function for spawning async tasks
///
/// Use this instead of the following:
/// - [`tokio::spawn`]
/// - [`wasm_bindgen_futures::spawn_local`]
#[cfg(any(target_arch = "wasm32", target_arch = "wasm64"))]
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
    #[cfg(not(any(target_arch = "wasm32", target_arch = "wasm64")))]
    handle: tokio::task::JoinHandle<T>,

    #[cfg(any(target_arch = "wasm32", target_arch = "wasm64"))]
    receiver: futures::channel::oneshot::Receiver<T>,
}

impl<T> JoinHandle<T>
where
    T: Send + 'static,
{
    /// Like [`Self::await_result`], for a caller that must not fail with the
    /// task: an error says why it did not finish.
    pub async fn try_await_result(self) -> Result<T, TaskEnded> {
        #[cfg(not(any(target_arch = "wasm32", target_arch = "wasm64")))]
        {
            self.handle.await.map_err(|err| TaskEnded(err.to_string()))
        }

        #[cfg(any(target_arch = "wasm32", target_arch = "wasm64"))]
        {
            self.receiver
                .await
                .map_err(|_| TaskEnded("the task was dropped before completing".to_string()))
        }
    }

    pub async fn await_result(self) -> T {
        #[cfg(not(any(target_arch = "wasm32", target_arch = "wasm64")))]
        {
            self.handle.await.expect("Task panicked")
        }

        #[cfg(any(target_arch = "wasm32", target_arch = "wasm64"))]
        {
            self.receiver
                .await
                .expect("Task was dropped before completing")
        }
    }
}

#[cfg(all(test, not(any(target_arch = "wasm32", target_arch = "wasm64"))))]
mod tests {
    use super::*;

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
