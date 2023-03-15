resource "jans_scim_app_configuration" "global" {
  application_url                = "https://test-instance.jans.io"
  base_dn                        = "o=jans"
  base_endpoint                  = "https://test-instance.jans.io/jans-scim/restv1"
  bulk_max_operations            = 30
  bulk_max_payload_size          = 3072000
  disable_jdk_logger             = true
  logging_layout                 = "text"
  logging_level                  = "INFO"
  max_count                      = 200
  metric_reporter_enabled        = true
  metric_reporter_interval       = 300
  metric_reporter_keep_data_days = 15
  ox_auth_issuer                 = "https://test-instance.jans.io"
  person_custom_object_class     = "jansCustomPerson"
  use_local_cache                = true
  user_extension_schema_uri      = "urn:ietf:params:scim:schemas:extension:gluu:2.0:User"
}