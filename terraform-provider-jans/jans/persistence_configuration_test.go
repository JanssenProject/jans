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

	// Check that we're using a SQL database (MySQL in this case)
	// The API doesn't return persistenceType, but we can infer it from productName
	if cfg.ProductName != "MySQL" {
		t.Fatalf("Expected MySQL database, got %s", cfg.ProductName)
	}

	if cfg.DatabaseName == "" {
		t.Fatal("DatabaseName should not be empty")
	}

}
