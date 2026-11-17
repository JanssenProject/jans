// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Reading a response body into memory under a size cap.

use bytes::Bytes;

use super::failure::{HttpClientError, ReadBodyError};

/// Reads a response body capped at `max_response_size` and deserializes it as JSON.
pub(crate) async fn read_json<T>(
    response: reqwest::Response,
    max_response_size: Option<u64>,
) -> Result<T, HttpClientError>
where
    T: serde::de::DeserializeOwned,
{
    let status = response.status();
    let bytes = read_body(response, max_response_size).await?;
    serde_json::from_slice::<T>(&bytes)
        .map_err(|error| HttpClientError::DeserializeJson { status, error })
}

/// [`read_response_capped`], keeping the status of the response in the error.
pub(crate) async fn read_body(
    response: reqwest::Response,
    max_response_size: Option<u64>,
) -> Result<Vec<u8>, HttpClientError> {
    let status = response.status();
    read_response_capped(response, max_response_size)
        .await
        .map_err(|error| HttpClientError::ReadBody { status, error })
}

/// Reads a response body into memory, aborting with `TooLarge` when
/// `max_response_size` is exceeded. Rejects upfront on `Content-Length` and
/// also enforces the cap chunk by chunk, so chunked or compressed responses
/// can't slip past: `Content-Length` is the encoded size, and the browser
/// decompresses transparently. `None` disables the cap.
async fn read_response_capped(
    response: reqwest::Response,
    max_response_size: Option<u64>,
) -> Result<Vec<u8>, ReadBodyError> {
    if let Some(limit) = max_response_size
        && let Some(declared) = response.content_length()
        && declared > limit
    {
        return Err(ReadBodyError::TooLarge {
            limit,
            read_so_far: declared,
        });
    }

    let mut chunks = Chunks::new(response);
    let mut buf: Vec<u8> = Vec::new();
    while let Some(chunk) = chunks.next().await.map_err(ReadBodyError::Read)? {
        if let Some(limit) = max_response_size {
            let new_len = (buf.len() as u64).saturating_add(chunk.len() as u64);
            if new_len > limit {
                return Err(ReadBodyError::TooLarge {
                    limit,
                    read_so_far: new_len,
                });
            }
        }
        buf.extend_from_slice(&chunk);
    }
    Ok(buf)
}

/// A body's chunks in order: native `reqwest` reads them directly, the browser's
/// fetch only as a stream.
#[cfg(not(target_arch = "wasm32"))]
struct Chunks(reqwest::Response);

#[cfg(not(target_arch = "wasm32"))]
impl Chunks {
    fn new(response: reqwest::Response) -> Self {
        Self(response)
    }

    async fn next(&mut self) -> Result<Option<Bytes>, reqwest::Error> {
        self.0.chunk().await
    }
}

#[cfg(target_arch = "wasm32")]
struct Chunks(std::pin::Pin<Box<dyn futures::Stream<Item = reqwest::Result<Bytes>>>>);

#[cfg(target_arch = "wasm32")]
impl Chunks {
    fn new(response: reqwest::Response) -> Self {
        Self(Box::pin(response.bytes_stream()))
    }

    async fn next(&mut self) -> Result<Option<Bytes>, reqwest::Error> {
        use futures::StreamExt;

        self.0.next().await.transpose()
    }
}

#[cfg(test)]
mod tests {
    use reqwest::StatusCode;

    use super::*;

    #[tokio::test]
    async fn rejects_body_larger_than_cap() {
        let mut server = mockito::Server::new_async().await;
        server
            .mock("GET", "/")
            .with_status(200)
            .with_body(vec![0u8; 4096])
            .create_async()
            .await;
        let resp = reqwest::get(server.url()).await.expect("send");
        let err = read_response_capped(resp, Some(1024))
            .await
            .expect_err("body exceeds cap");
        assert!(
            matches!(err, ReadBodyError::TooLarge { limit: 1024, .. }),
            "body over the 1024-byte cap must surface as TooLarge {{ limit: 1024, .. }}, got {err:?}",
        );
        assert!(
            err.to_string()
                .contains("CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES"),
            "the error must name the property that controls the cap, got: {err}"
        );
    }

    #[tokio::test]
    async fn truncated_body_is_a_read_error_with_the_status() {
        // We want a real `ReadBodyError::Read`: response status + headers
        // arrive, but the body stream is truncated before all of the
        // advertised `Content-Length` bytes show up.
        //
        // Mockito's `with_chunked_body(|w| Err(...))` *can* produce that
        // state, but in practice mockito tears the connection down before
        // the response line reaches the client, so reqwest reports
        // `IncompleteMessage` at send-time — a flaky race we hit on CI.
        // A one-shot raw TCP listener gives us deterministic control: we
        // send full headers (with `Content-Length: 1000`), flush, write 7
        // bytes of body, then close — the client always sees the response,
        // then always sees the truncated body.
        use std::io::{Read, Write};
        let listener = std::net::TcpListener::bind("127.0.0.1:0").expect("bind");
        let addr = listener.local_addr().expect("local_addr");
        let server = std::thread::spawn(move || {
            let (mut sock, _) = listener.accept().expect("accept");
            // Drain the request headers so the kernel doesn't RST the
            // connection while we're writing the response.
            let mut buf = [0u8; 1024];
            let mut total = 0usize;
            while total < buf.len() {
                match sock.read(&mut buf[total..]) {
                    Ok(0) | Err(_) => break,
                    Ok(n) => {
                        total += n;
                        if buf[..total].windows(4).any(|w| w == b"\r\n\r\n") {
                            break;
                        }
                    },
                }
            }
            sock.write_all(b"HTTP/1.1 200 OK\r\nContent-Length: 1000\r\nConnection: close\r\n\r\n")
                .expect("write headers");
            sock.flush().expect("flush headers");
            // Far fewer than the advertised 1000 bytes, then drop = close.
            let _ = sock.write_all(b"partial");
        });
        let resp = reqwest::get(format!("http://{addr}/")).await.expect("send");
        let err = read_body(resp, None)
            .await
            .expect_err("a body cut short must fail to read");
        server.join().ok();
        assert!(
            matches!(
                err,
                HttpClientError::ReadBody {
                    status: StatusCode::OK,
                    error: ReadBodyError::Read(_),
                }
            ),
            "a truncated body must be a ReadBody read error keeping the 200 status, got {err:?}",
        );
    }
}
