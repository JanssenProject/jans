package jans

import (
	"context"
	"testing"
)

func TestHealthStatus(t *testing.T) {
	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	// Test GetHealthStatus
	health, err := client.GetHealthStatus(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(health) == 0 || health[0].Status == "" {
		t.Error("expected health status to be non-empty")
	}
}

func TestServerStats(t *testing.T) {
	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	// Test GetServerStats
	stats, err := client.GetServerStats(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if stats == nil {
		t.Error("expected server stats to be non-nil")
	}
}
