resource "jans_scope" "test" {
	display_name  = "test groups.read"
	scope_id      = "https://jans.io/test/groups.read"
	description 	= "Query test group resources"
	scope_type    = "oauth"
	creation_date = "2022-09-01T13:42:58"
	uma_type      = false
	
	attributes {
		show_in_configuration_endpoint = true
	}
}