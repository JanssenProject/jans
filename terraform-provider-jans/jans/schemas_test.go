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

	if len(schemas) != 5 {
		t.Errorf("expected 5 schemas, got %d", len(schemas))
	}

	groupSchema, err := client.GetSchema(ctx, "urn:ietf:params:scim:schemas:core:2.0:Group")
	if err != nil {
		t.Fatal(err)
	}

	if groupSchema.Name != "Group" {
		t.Errorf("expected schema name Group, got %s", groupSchema.Name)
	}
}
