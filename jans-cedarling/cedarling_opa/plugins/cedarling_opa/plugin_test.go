package cedarlingopa

import (
	"testing"
)

func TestPluginName(t *testing.T) {
	if PluginName != "cedarling_opa" {
		t.Errorf("Plugin name should be cedarling_opa, got %s", PluginName)
	}
}

func TestValidateConfig(t *testing.T) {
	factory := Factory{}
	config := []byte(`{"bootstrap_config": {}, "host": "test.example.org"}`)
	result, err := factory.Validate(nil, config)
	if err != nil {
		t.Errorf("Error should not be raised. Raised %s", err.Error())
	}
	cfg, ok := result.(Config)
	if !ok {
		t.Errorf("Validate should return Config, returned %T", result)
	}
	if cfg.Host != "test.example.org" {
		t.Errorf("Host should be test.example.org, got %s", cfg.Host)
	}
}
