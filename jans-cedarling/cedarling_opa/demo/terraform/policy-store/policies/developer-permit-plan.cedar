// Developers may only run terraform plan on any workspace.
@id("developer-permit-plan")
permit (
    principal,
    action == Infra::Action::"Plan",
    resource
) when {
    principal.role.contains("Developer")
};
