use cedar_policy::Decision;

/// Result of authorization and evaluation cedar policy
/// based on the [Request](crate::models::request::Request) and policy store
pub struct AuthorizeResult {
    /// Result of authorization where principal is `Jans::Workload`
    pub workload: cedar_policy::Response,
    // The response for the user entity
    // next field will be added in further PR
    // pub person: cedar_policy::Response,
}

impl AuthorizeResult {
    /// Returns true if request is allowed
    pub fn is_allowed(&self) -> bool {
        // in future we should also check if the person is allowed
        self.workload.decision() == Decision::Allow
    }
}
