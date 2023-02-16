
resource "jans_fido2_configuration" "global" {
  base_endpoint                   = "https://test-instance.jans.io/jans-fido2/restv1"
  clean_service_batch_chunk_size  = 10000
  clean_service_interval          = 60
  disable_jdk_logger              = true
  issuer                          = "https://test-instance.jans.io"
  logging_layout                  = "text"
  logging_level                   = "INFO"
  metric_reporter_enabled         = true
  metric_reporter_interval        = 300
  metric_reporter_keep_data_days  = 15
  person_custom_object_class_list = [
    "jansCustomPerson",
    "jansPerson",
  ]
  use_local_cache                 = true 

  fido2_configuration {
    authentication_history_expiration = 1296000
    authenticator_certs_folder        = "/etc/jans/conf/fido2/authenticator_cert"
    mds_certs_folder                  = "/etc/jans/conf/fido2/mds/cert"
    mds_tocs_folder                   = "/etc/jans/conf/fido2/mds/toc"
    requested_credential_types        = [
        "RS256",
        "ES256",
      ]
    server_metadata_folder            = "/etc/jans/conf/fido2/server_metadata"
    unfinished_request_expiration     = 180
    user_auto_enrollment              = false

    requested_parties {
      domains = [
        "test-instance.jans.io",
      ]
      name    = "https://test-instance.jans.io"
    }
  }
}