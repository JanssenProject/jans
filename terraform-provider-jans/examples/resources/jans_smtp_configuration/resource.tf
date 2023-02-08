resource "jans_smtp_configuration" "global" {
  host                    = "smtp.janssen.io"
  port                    = 587
  requires_ssl            = true
  trust_host              = true
  from_name               = "Janssen"
  from_email_address      = "jans@janssen.io"
  requires_authentication = true
  user_name               = "janssen"
  password                = "password"

  lifecycle {
		# ignore changes to password, as it will be returned as a hash
		# from the API
    ignore_changes = [ password ]
  }
}