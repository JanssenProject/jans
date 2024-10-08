/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use lazy_static::lazy_static;

use super::create::{CedarPolicyType, EntityAttributeMetadata, EntityMetadata};

// Represent meta information about entity from cedar-policy schema.
lazy_static! {
    pub(crate) static ref WorkloadEntityMeta: EntityMetadata<'static> = EntityMetadata::new(
        "Jans::Workload",
        "sub",
        vec![
            EntityAttributeMetadata {
                attribute_name: "client_id",
                token_claims_key: "sub",
                cedar_policy_type: CedarPolicyType::String,
            },
            EntityAttributeMetadata {
                attribute_name: "iss",
                token_claims_key: "iss",
                cedar_policy_type: CedarPolicyType::EntityUid {
                    entity_type: "Jans::TrustedIssuer",
                },
            },
        ]
    );
}
