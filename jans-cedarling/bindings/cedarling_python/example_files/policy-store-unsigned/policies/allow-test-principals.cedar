@id("allow-test-principals")
permit(
    principal,
    action in [Jans::Action::"UpdateForTestPrincipals"],
    resource
) when {
    principal.is_ok
};
