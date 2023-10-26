data "jans_custom_script_types" "script_types" {
}

output "script_type_client_registration_enabled" {
  value = contains(data.jans_custom_script_types.script_types, "client_registration")
}