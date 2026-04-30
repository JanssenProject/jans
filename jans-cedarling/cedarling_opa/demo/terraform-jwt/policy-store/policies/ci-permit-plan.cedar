// Any CI workflow from a trusted repository may run terraform plan on any workspace.
// Claims are read from context.tokens.ci_githubworkflow (multi-issuer path — no principal).
@id("ci-permit-plan")
permit (
    principal,
    action == CI::Action::"Plan",
    resource
) when {
    context.tokens has ci_githubworkflow &&
    context.tokens.ci_githubworkflow.iss == CI::TrustedIssuer::"https://token.actions.githubusercontent.com" &&
    (
        context.tokens.ci_githubworkflow.repository == "octoorg/myrepo" ||
        context.tokens.ci_githubworkflow.repository == "janssenproject/jans"
    )
};
