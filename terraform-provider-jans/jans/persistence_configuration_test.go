package jans

import (
	"context"
	"testing"
)

func TestPersistance(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cfg, err := client.GetPersistenceConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	// Check that we're using a SQL database. The backend may be MySQL or PostgreSQL
	// depending on the CI matrix leg, so accept either.
	if cfg.ProductName != "MySQL" && cfg.ProductName != "PostgreSQL" {
		t.Fatalf("Expected a SQL database (MySQL or PostgreSQL), got %s", cfg.ProductName)
	}

	if cfg.DatabaseName == "" {
		t.Fatal("DatabaseName should not be empty")
	}

}
