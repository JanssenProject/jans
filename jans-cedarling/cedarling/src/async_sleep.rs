// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cross-platform async sleep for background tasks.
//!
//! Tokio timers require a Tokio runtime; browser WASM builds use [`gloo_timers`] instead.

use std::time::Duration;

/// Yields asynchronously for approximately `duration`.
///
/// On native targets this uses [`tokio::time::sleep`]. On WebAssembly it uses a browser
/// timer so background work (for example status list refresh) does not require a Tokio
/// reactor.
pub(crate) async fn sleep(duration: Duration) {
    #[cfg(not(target_family = "wasm"))]
    {
        tokio::time::sleep(duration).await;
    }

    #[cfg(target_family = "wasm")]
    {
        let millis = duration.as_millis().min(u128::from(u32::MAX));
        #[allow(clippy::cast_possible_truncation)]
        let millis: u32 = millis as u32;
        gloo_timers::future::TimeoutFuture::new(millis).await;
    }
}
