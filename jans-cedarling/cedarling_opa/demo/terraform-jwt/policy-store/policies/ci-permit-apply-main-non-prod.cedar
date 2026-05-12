// CI workflows on the main branch may apply to any non-production workspace.
// Claims are read from context.tokens.ci_githubworkflow (multi-issuer path — no principal).
@id("ci-permit-apply-main-non-prod")
permit (
    principal,
    action == CI::Action::"Apply",
    resource
) when {
    context.tokens has ci_githubworkflow &&
    context.tokens.ci_githubworkflow.iss == CI::TrustedIssuer::"https://token.actions.githubusercontent.com" &&
    (
        context.tokens.ci_githubworkflow.repository == "octoorg/myrepo" ||
        context.tokens.ci_githubworkflow.repository == "JanssenProject/jans"
    ) &&
    context.tokens.ci_githubworkflow.ref == "refs/heads/main" &&
    resource != CI::TerraformWorkspace::"production"
};
