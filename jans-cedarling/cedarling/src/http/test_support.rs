// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Test-only helpers for code that builds an [`HttpClient`] or reads its answers.

use std::time::Duration;

use serde::Deserialize;

use super::{HttpClient, HttpClientConfig, HttpClientError};

/// The default HTTP client settings with a 10 ms retry delay, so a test that
/// exercises retries finishes in milliseconds rather than 3 + 6 + 12 seconds.
pub(crate) fn fast_retries() -> HttpClientConfig {
    HttpClientConfig {
        retry_delay: Duration::from_millis(10),
        ..HttpClientConfig::default()
    }
}

impl HttpClient {
    /// GET with retries, returning the body for the test to deserialize.
    pub(crate) async fn get(&self, uri: &str) -> Result<Response, HttpClientError> {
        let body = self.get_bytes(uri).await?;
        Ok(Response { body })
    }

    /// GET with retries, returning the raw body.
    pub(crate) async fn get_bytes(&self, uri: &str) -> Result<Vec<u8>, HttpClientError> {
        let response = self.get_with_retry(uri).await?;
        self.read_response_capped(response).await
    }
}

/// A response body a test deserializes when it needs it.
#[derive(Debug)]
pub(crate) struct Response {
    body: Vec<u8>,
}

impl Response {
    pub(crate) fn json<'a, T>(&'a self) -> Result<T, serde_json::Error>
    where
        T: Deserialize<'a>,
    {
        serde_json::from_slice::<'a, T>(&self.body)
    }
}

/// A loopback port that refuses every connection for as long as it lives: bound
/// but never listening. Binding and dropping a listener instead would free the
/// port for the next test that binds `:0`, which then answers in its place.
#[cfg(not(target_arch = "wasm32"))]
pub(crate) struct ClosedPort {
    /// Held only to keep the port reserved.
    _socket: tokio::net::TcpSocket,
    addr: std::net::SocketAddr,
}

#[cfg(not(target_arch = "wasm32"))]
impl ClosedPort {
    pub(crate) fn new() -> Self {
        let socket = tokio::net::TcpSocket::new_v4().expect("a TCP socket can be created");
        socket
            .bind("127.0.0.1:0".parse().expect("a valid address"))
            .expect("a free loopback port can be bound");
        let addr = socket.local_addr().expect("a bound socket has an address");
        Self {
            _socket: socket,
            addr,
        }
    }

    /// `http://127.0.0.1:<port>` followed by `path`.
    pub(crate) fn url(&self, path: &str) -> String {
        format!("http://{}{path}", self.addr)
    }
}
