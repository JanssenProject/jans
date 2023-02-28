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

	// this is the original value against the server, used during
	// development. The value might be different on a different
	// development server.
	if cfg.PersistenceType != "sql" {
		t.Fatal("PersistenceType is not sql")
	}

}
