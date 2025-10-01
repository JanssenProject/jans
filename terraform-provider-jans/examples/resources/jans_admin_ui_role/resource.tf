resource "jans_admin_ui_role" "api_attribute_manager" {
	role        = "api-attribute-manager"
	description = "Role to manage attributes"
  deletable 	= true
}