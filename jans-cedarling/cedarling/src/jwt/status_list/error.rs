// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use thiserror::Error;

#[derive(Debug, Error)]
pub enum ParseStatusListError {
    #[error("invalid `bit` size in the status list. expected 1, 2, 4, or 8 but got: {0}")]
    InvalidBitSize(u8),
    #[error("failed to decode status list: {0}")]
    Decode(#[from] base64::DecodeError),
    #[error("failed to read status list: {0}")]
    Read(#[from] std::io::Error),
}
