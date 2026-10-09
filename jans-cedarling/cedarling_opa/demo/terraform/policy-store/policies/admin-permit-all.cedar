// Admins may plan, apply, or destroy any workspace including production.
@id("admin-permit-all")
permit (
    principal,
    action in [
        Infra::Action::"Plan",
        Infra::Action::"Apply",
        Infra::Action::"Destroy"
    ],
    resource
) when {
    principal.role.contains("Admin")
};
