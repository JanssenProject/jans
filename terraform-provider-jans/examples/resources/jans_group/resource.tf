resource jans_user "test_user" {
  display_name        = "test-user"
  schemas             = [ "urn:ietf:params:scim:schemas:core:2.0:User" ]
  external_id         = "ext1234"
  user_name           = "test-user"
  nick_name           = "test-user"
  profile_url         = "https://localhost:9443/scim/v2/Users/1234"
  title               = "Mr"
  user_type           = "Employee"
  preferred_language  = "en"
  locale              = "en_US"
  timezone            = "UTC"
  active              = true
  password            = "password"
}

resource "jans_group" "test" {
	display_name 	= "test-group"
	schemas 			= ["urn:ietf:params:scim:schemas:core:2.0:Group"]

  members{
    value   = jans_user.test_user.id
    display = jans_user.test_user.display_name
    type    = "User"
    ref     = jans_user.test_user.meta.location
  }
}
