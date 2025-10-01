data "jans_service_provider_config" "config" {
	id = "urn:ietf:params:scim:schemas:core:2.0:Group"
}

output "group_attribute" {
  value = data.jans_schema.group.attributes[0].name
}