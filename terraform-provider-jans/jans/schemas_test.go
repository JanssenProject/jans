package jans

import (
	"context"
	"testing"
)

func TestSchemas(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	schemas, err := client.GetSchemas(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(schemas) == 0 {
		t.Fatal("expected at least one schema, got none")
	}

	// Verify that each schema has required fields
	for _, schema := range schemas {
		if len(schema.Schemas) == 0 {
			t.Fatal("schema should have at least one schema identifier")
		}
		if schema.ID == "" {
			t.Fatal("schema ID should not be empty")
		}
		if schema.Name == "" {
			t.Fatal("schema name should not be empty")
		}
	}

	groupSchema, err := client.GetSchema(ctx, "urn:ietf:params:scim:schemas:core:2.0:Group")
	if err != nil {
		t.Fatal(err)
	}

	if groupSchema.Name != "Group" {
		t.Errorf("expected schema name Group, got %s", groupSchema.Name)
	}
}
