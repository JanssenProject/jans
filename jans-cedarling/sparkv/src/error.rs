/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */

#[derive(Debug, PartialEq, thiserror::Error)]
pub enum Error {
    #[error("capacity exceeded")]
    CapacityExceeded,
    #[error("item size exceeded")]
    ItemSizeExceeded,
    #[error("ttl too long")]
    TTLTooLong,
}
