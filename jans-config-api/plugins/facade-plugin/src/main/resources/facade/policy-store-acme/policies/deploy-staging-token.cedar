@id("deploy-staging-token")
@reason("A signed-in admin may deploy the workload named in the change to staging.")
permit(principal, action == AcmeOps::Action::"Deploy", resource is AcmeOps::Resource)
when {
  context has tokens &&
  context.tokens has acme_userinfo_token &&
  context.tokens.acme_userinfo_token has role &&
  context.tokens.acme_userinfo_token.role.contains("admin") &&
  resource.kind == "workload" &&
  resource.environment == "staging" &&
  context.change_workload == resource.workload
};
