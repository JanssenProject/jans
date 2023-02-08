resource jans_user "user" {
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
  
  name {
    family_name     = "Doe"
    given_name       = "John"
    middle_name      = "M"
    honorific_prefix = "Dr"
    honorific_suffix = "Jr"
    formatted				 = "Dr John M Doe Jr"
  }

  emails {
    value   = "john.doe@jans.io"
    display = "Work"
    type    = "work"
    primary = true
  }
  
  phone_numbers {
    value   = "1234567890"
    display = "Mobile"
    type    = "work"
    primary = true
  }

  ims {
    value   = "@john.doe"
    display = "Messenger"
    type    = "Messenger"
    primary = true
  }

  photos {
    value   = "https://localhost:9443/scim/v2/Users/1234/photo"
    display = "Photo"
    type    = "photo"
    primary = true
  }

  addresses {
    formatted       = "123 Main St"
    street_address  = "123 Main St"
    locality        = "New York"
    region          = "NY"
    postal_code     = "12345"
    country         = "US"
    type            = "work"
    primary         = true
  }

  entitlements {
    value   = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:entitlement"
    display = "Entitlement"
    type    = "entitlement"
    primary = true
  }

  roles {
    value   = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:role"
    display = "Role"
    type    = "role"
    primary = true
  }

  x509_certificates {
    value   = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:x509Certificates"
    display = "X509Certificates"
    type    = "PEM"
    primary = true
  }
}