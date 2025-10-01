data "jans_persistence_config" "default" {
}

output "persistence_config" {
  value = data.jans_persistence_config.default.persistence_type
}