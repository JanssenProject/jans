// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use jans_trace_core::Digest;

use crate::common::policy_store::PolicyStoreWithID;
use crate::common::policy_store::archive_handler::ArchiveLimits;
use crate::common::policy_store::loader::read_policy_store_archive_bytes;
use crate::common::policy_store::test_utils::PolicyStoreTestBuilder;

/// `builder`'s archive loaded as TRACE requires, and its `bundle_hash`.
pub(crate) fn cjar_store(builder: &PolicyStoreTestBuilder) -> (PolicyStoreWithID, Digest) {
    let bytes = builder.build_archive().expect("test archive builds");
    let hash = jans_trace_core::inputs::compute_bundle_hash(&bytes);
    let store = read_policy_store_archive_bytes(bytes, false, ArchiveLimits::default())
        .expect("test archive parses")
        .store;
    (store, hash)
}
