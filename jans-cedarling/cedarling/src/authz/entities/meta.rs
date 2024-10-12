/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use lazy_static::lazy_static;

use super::create::EntityMetadata;

// Represent meta information about entity from cedar-policy schema.
lazy_static! {
    pub(crate) static ref WorkloadEntityMeta: EntityMetadata<'static> =
        EntityMetadata::new("Jans::Workload", "client_id",);
    pub(crate) static ref AccessTokenMeta: EntityMetadata<'static> =
        EntityMetadata::new("Jans::Access_token", "jti",);
}
