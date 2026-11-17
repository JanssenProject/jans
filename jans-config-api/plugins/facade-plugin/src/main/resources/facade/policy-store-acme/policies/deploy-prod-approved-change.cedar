@id("deploy-prod-approved-change")
@reason("An SRE agent may deploy to production only for an approved change while a human is on call.")
permit(principal, action == AcmeOps::Action::"Deploy", resource is AcmeOps::Resource)
when {
  !(context has tokens) &&
  principal is AcmeOps::Agent &&
  resource.kind == "workload" &&
  resource.environment == "prod" &&
  principal.role == "sre" &&
  context.change_approved &&
  context.human_on_call &&
  context.change_workload == resource.workload
};
