resource "jans_admin_ui_role" "api_attribute_manager" {
	role        = "api-attribute-manager"
	description = "Role to manage attributes"
	deletable 	= true
}

resource "jans_admin_ui_permission" "attribute_delete" {
	permission  = "https://jans.io/oauth/config/attributes.delete"
	description = "Permission to delete an already existing attribute"
}

resource "jans_admin_ui_role_permission_mapping" "api_attribute_manager_mapping" {
	role        = resource.jans_admin_ui_role.api_attribute_manager.role
	permissions = [
		resource.jans_admin_ui_permission.attribute_delete.permission,
	]
}