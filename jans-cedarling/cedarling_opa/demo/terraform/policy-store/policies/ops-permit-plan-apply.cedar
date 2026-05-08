// Ops engineers may plan or apply to any workspace except production.
// Destroying infrastructure in any workspace requires Admin privileges.
@id("ops-permit-plan-apply-non-prod")
permit (
    principal,
    action in [
        Infra::Action::"Plan",
        Infra::Action::"Apply"
    ],
    resource
) when {
    principal.role.contains("Ops") &&
    resource != Infra::TerraformWorkspace::"production"
};
