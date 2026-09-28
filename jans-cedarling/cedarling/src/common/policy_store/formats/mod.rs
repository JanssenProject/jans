// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Versioned on-disk formats of the directory / `.cjar` policy store.
//!
//! Each `vN` module owns the `Deserialize` structs for its version and must not
//! import another version module. Runtime types never reference `formats::vN`.

pub(crate) mod v1;
