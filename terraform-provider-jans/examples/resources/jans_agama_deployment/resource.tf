resource "jans_agama_deployment" "example" {
  name = "example-agama-project"
  
  source = "path/to/agama-project.gama"
  
  autoconfigure = true
}
