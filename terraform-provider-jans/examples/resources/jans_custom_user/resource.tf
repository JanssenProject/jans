resource "jans_custom_user" "test" {

	jans_status 						= "active"
	user_id 								= "test"
	ox_auth_persistent_jwt 	= ["jwt1", "jwt2"]
	mail 										= "test@jans.io"
	display_name 						= "display-test"
	given_name 							= "given-name-test"
	user_password 					= "password"

	custom_attributes {
		name 					= "nickname"
		multi_valued 	= false
		value 				= "\"jdoe\""
		display_value = "jdoe"
	}

	lifecycle {
		# ignore changes to password, as it will not be 
		# returned from the API
    ignore_changes = [ user_password ]
  }
}