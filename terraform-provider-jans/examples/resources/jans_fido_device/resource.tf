
resource "jans_fido_device" "example" {
  id           = "example-fido-device"
  user_inum    = "1800.user-example"
  device_data  = jsonencode({
    "deviceName" = "Example FIDO Device"
    "keyType"    = "RSA"
  })
  device_hash_code = 54321
  device_key_handle = "example-fido-key-handle"
  device_registration_conf = jsonencode({
    "attestationType" = "self"
  })
  counter = 5
  status  = "active"
  application = "https://example.com"
  creation_date = "2023-01-01T00:00:00Z"
}
