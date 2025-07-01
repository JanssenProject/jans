
resource "jans_asset" "example" {
  inum         = "1800.asset-example"
  dn           = "inum=1800.asset-example,ou=assets,o=jans"
  display_name = "Example Asset"
  description  = "Example asset for testing"
  asset_type   = "logo"
  file_path    = "/path/to/asset/file.png"
  content_type = "image/png"
}
