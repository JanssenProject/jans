resource "jans_ldap_database_configuration" "test" {
  local_primary_key = "uid"
  config_id 				= "test"
  max_connections 	= 200
  servers 					= ["ldap.default:1636"]
  use_ssl 					= true
  bind_dn 					= "cn=directory manager"
  bind_password 		= "CLcHdW8FPW40PByaxmcXaQ=="
  primary_key 			= "uid"
  base_dns 					= ["ou=people,o=jans"]

	lifecycle {
		# ignore changes to password, as it will be returned as a hash
		# from the API
    ignore_changes = [ bind_password ]
  }
}