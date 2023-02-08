resource "jans_admin_ui_permission" "attribute_delete" {
	permission  = "https://jans.io/oauth/config/attributes.delete"
	description = "Permission to delete an already existing attribute"
}