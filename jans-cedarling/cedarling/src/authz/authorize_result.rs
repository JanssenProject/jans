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
    /// Evaluates the authorization result to determine if the request is allowed.  
    ///  
    /// This function checks the decision based on the following rule:  
    /// - The `workload` must allow the request (PRINCIPAL).  
    /// - Either the `person` or `role` must also allow the request (USER OR ROLE).  
    ///  
    /// This approach represents a hierarchical decision-making model, where the  
    /// `workload` (i.e., primary principal) needs to permit the request and
    /// additional conditions (either `person` or `role`) must also indicate allowance.
    pub fn is_allowed(&self) -> bool {
        let role_decision = self
            .role
            .as_ref()
            .map(|result| result.decision())
            .unwrap_or(Decision::Deny);

        let workload_allowed = self.workload.decision() == Decision::Allow;
        let person_or_role_allowed =
            self.person.decision() == Decision::Allow || role_decision == Decision::Allow;

        workload_allowed && person_or_role_allowed
    }
}
