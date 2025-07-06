package jans

import (
	"context"
	"testing"
)

func TestMessage(t *testing.T) {
	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	// Test GetMessageConfiguration
	config, err := client.GetMessageConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if config == nil {
		t.Error("expected message configuration to be non-nil")
	}

	// Test message configuration patch
	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/messageProviderType",
			Value: "REDIS",
		},
	}

	updatedConfig, err := client.PatchMessage(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	if updatedConfig.MessageProviderType != "REDIS" {
		t.Errorf("expected message provider type to be REDIS, got %s", updatedConfig.MessageProviderType)
	}

	// Test Redis configuration if available
	if updatedConfig.RedisConfiguration != nil {
		redisConfig, err := client.GetMessageRedis(ctx)
		if err != nil {
			t.Fatal(err)
		}

		if redisConfig == nil {
			t.Error("expected redis configuration to be non-nil")
		}
	}

	// Test Postgres configuration
	postgresConfig, err := client.GetMessagePostgres(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if postgresConfig == nil {
		t.Error("expected postgres configuration to be non-nil")
	}
}
