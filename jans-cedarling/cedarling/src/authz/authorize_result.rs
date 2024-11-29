use cedar_policy::Decision;

/// Result of authorization and evaluation cedar policy
/// based on the [Request](crate::models::request::Request) and policy store
pub struct AuthorizeResult {
    /// Result of authorization where principal is `Jans::Workload`
    pub workload: Option<cedar_policy::Response>,
    /// Result of authorization where principal is `Jans::User`
    pub person: Option<cedar_policy::Response>,
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
        let workload_allowed = self
            .workload
            .as_ref()
            .map(|response| response.decision() == Decision::Allow);

        let person_allowed = self
            .person
            .as_ref()
            .map(|response| response.decision() == Decision::Allow);

        // cover each possible case when any of value is Some or None
        match (workload_allowed, person_allowed) {
            (None, None) => false,
            (None, Some(person)) => person,
            (Some(workload), None) => workload,
            (Some(workload), Some(person)) => workload && person,
        }
    }
}
