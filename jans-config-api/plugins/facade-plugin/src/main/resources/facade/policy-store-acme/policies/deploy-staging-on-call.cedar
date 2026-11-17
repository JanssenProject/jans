@id("deploy-staging-on-call")
@reason("An on-call SRE agent may deploy the workload named in the change to staging.")
permit(principal, action == AcmeOps::Action::"Deploy", resource is AcmeOps::Resource)
when {
  !(context has tokens) &&
  principal is AcmeOps::Agent &&
  resource.kind == "workload" &&
  resource.environment == "staging" &&
  principal.role == "sre" &&
  principal.on_call &&
  context.change_workload == resource.workload
};
