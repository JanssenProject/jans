package jans

import (
	"bytes"
	"context"
	"testing"
)

func TestCreateJansAsset(t *testing.T) {
	c, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	// jans-auth allows the .properties extension (i18n asset dir mapping).
	doc, err := c.CreateJansAsset(ctx, Document{
		FileName:    "test.properties",
		Description: "A document made for testing purposes",
		Service:     "jans-auth",
		Level:       1,
	}, bytes.NewReader([]byte("test.key=test value\n")))
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = c.DeleteJansAsset(ctx, doc.Inum)
	})

	if doc.Inum == "" {
		t.Fatal("expected inum to be set after create")
	}

	gotDoc, err := c.GetJansAsset(ctx, doc.Inum)
	if err != nil {
		t.Fatal(err)
	}

	if gotDoc.FileName != "test.properties" {
		t.Errorf("expected fileName test.properties, got %q", gotDoc.FileName)
	}
}
