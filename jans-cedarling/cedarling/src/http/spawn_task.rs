// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

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
