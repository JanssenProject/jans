resource "jans_cache_configuration" "global" {
  cache_provider_type = "NATIVE_PERSISTENCE"
  
  in_memory_configuration {
    default_put_expiration = 60
  }

  memcached_configuration {
    buffer_size                = 0
    default_put_expiration     = 0
    max_operation_queue_length = 0
    servers                    = "localhost:11211"
  }

  native_persistence_configuration {
    default_cleanup_batch_size           = 1000
    default_put_expiration               = 60
    delete_expired_on_get_request        = false
    disable_attempt_update_before_insert = false
  }

  redis_configuration {
    connection_timeout     = 3000
    default_put_expiration = 60
    max_idle_connections   = 10
    max_retry_attempts     = 5
    max_total_connections  = 500
    redis_provider_type    = "STANDALONE"
    servers                = "localhost:6379"
    so_timeout             = 3000
    use_ssl                = false
  }
}