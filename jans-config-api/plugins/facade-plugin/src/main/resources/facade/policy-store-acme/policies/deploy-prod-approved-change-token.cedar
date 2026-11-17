@id("deploy-prod-approved-change-token")
@reason("A signed-in admin may deploy to production only for an approved change while a human is on call.")
permit(principal, action == AcmeOps::Action::"Deploy", resource is AcmeOps::Resource)
when {
  context has tokens &&
  context.tokens has acme_userinfo_token &&
  context.tokens.acme_userinfo_token has role &&
  context.tokens.acme_userinfo_token.role.contains("admin") &&
  resource.kind == "workload" &&
  resource.environment == "prod" &&
  context.change_approved &&
  context.human_on_call &&
  context.change_workload == resource.workload
};
