use cedar_policy::Decision;

/// Result of authorization and evaluation cedar policy
/// based on the [Request](crate::models::request::Request) and policy store
pub struct AuthorizeResult {
    /// Result of authorization where principal is `Jans::Workload`
    pub workload: cedar_policy::Response,
    /// Result of authorization where principal is `Jans::User`
    pub person: cedar_policy::Response,
    /// Result of authorization where principal is `Jans::Role`
    pub role: Option<cedar_policy::Response>,
}

impl AuthorizeResult {
    /// Returns true if request is allowed
    /// check decision algorithm:
    /// PRINCIPAL AND (USER OR ROLE)
    pub fn is_allowed(&self) -> bool {
        let role_decision = self
            .role
            .as_ref()
            .map(|result| result.decision())
            .unwrap_or(Decision::Deny);

        self.workload.decision() == Decision::Allow
            && (self.person.decision() == Decision::Allow || role_decision == Decision::Allow)
    }
}
