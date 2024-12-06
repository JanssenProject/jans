/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::WorkloadBoolOp;

/// Configuration to specify authorization workflow.
/// - If we use user entity as principal.
/// - If we use workload entity as principal.
/// - What boolean operator we need to use when both is used
#[derive(Debug, Clone, Default, PartialEq)]
pub struct AuthorizationConfig {
    /// When `enabled`, Cedar engine authorization is queried for a User principal.
    /// bootstrap property: `CEDARLING_USER_AUTHZ`
    pub use_user_principal: bool,

    /// When `enabled`, Cedar engine authorization is queried for a Workload principal.
    /// bootstrap property: `CEDARLING_WORKLOAD_AUTHZ`
    pub use_workload_principal: bool,

    /// Specifies what boolean operation to use for the `USER` and `WORKLOAD` when
    /// making authz (authorization) decisions.
    ///
    /// # Available Operations
    /// - **AND**: authz will be successful if `USER` **AND** `WORKLOAD` is valid.
    /// - **OR**: authz will be successful if `USER` **OR** `WORKLOAD` is valid.
    pub user_workload_operator: WorkloadBoolOp,

    /// Name of Cedar Context schema entity
    pub mapping_user: Option<String>,

    /// Name of Cedar Workload schema entity
    pub mapping_workload: Option<String>,

    /// Name of Cedar id_token schema entity
    pub mapping_id_token: Option<String>,

    /// Name of Cedar access_token schema entity
    pub mapping_access_token: Option<String>,

    /// Name of Cedar userinfo schema entity
    pub mapping_userinfo_token: Option<String>,
}
