// CI workflows may apply to production only from main, from a trusted repository,
// and when the OIDC token carries environment == "production" — proof that a
// designated reviewer approved the GitHub Environment gate before the job ran.
// Claims are read from context.tokens.ci_githubworkflow (multi-issuer path — no principal).
@id("ci-permit-apply-prod-via-environment")
permit (
    principal,
    action == CI::Action::"Apply",
    resource == CI::TerraformWorkspace::"production"
) when {
    context.tokens has ci_githubworkflow &&
    context.tokens.ci_githubworkflow.iss == CI::TrustedIssuer::"https://token.actions.githubusercontent.com" &&
    (
        context.tokens.ci_githubworkflow.repository == "octoorg/myrepo" ||
        context.tokens.ci_githubworkflow.repository == "JanssenProject/jans"
    ) &&
    context.tokens.ci_githubworkflow.ref == "refs/heads/main" &&
    // `has` guard required: `environment` is optional — absent on plan and non-prod-apply tokens.
    context.tokens.ci_githubworkflow has environment &&
    context.tokens.ci_githubworkflow.environment == "production"
};
