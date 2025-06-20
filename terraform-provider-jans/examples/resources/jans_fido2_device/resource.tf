
resource "jans_fido2_device" "example" {
  id           = "example-fido2-device"
  user_inum    = "1800.user-example"
  device_data  = jsonencode({
    "deviceName" = "Example FIDO2 Device"
    "keyType"    = "EC"
  })
  device_hash_code = 12345
  device_key_handle = "example-key-handle"
  device_registration_conf = jsonencode({
    "attestationType" = "basic"
  })
  counter = 1
  status  = "active"
  application_id = "https://example.com"
  creation_date = "2023-01-01T00:00:00Z"
}
