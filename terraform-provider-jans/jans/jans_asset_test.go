package jans

import (
	"bytes"
	"context"
	"io"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestCreateJansAsset(t *testing.T) {
	t.Skip("Service not implemented")
	c, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	// Generate metadata dynamically
	metadataReader, err := GenerateMetadataReader(GetCurrentHost())
	if err != nil {
		t.Fatal(err)
	}

	metadataBytes, err := io.ReadAll(metadataReader)
	if err != nil {
		t.Fatal(err)
	}
	file := bytes.NewReader(metadataBytes)

	doc, err := c.CreateJansAsset(ctx, Document{
		FileName:    "metadata.xml",
		Description: "A document made for testing purposes",
		Document:    "Doc",
		BaseDn:      "RandomBaseDN",
		Level:       "1",
	}, file)
	if err != nil {
		t.Fatal(err)
	}

	gotDoc, err := c.GetJansAsset(ctx, doc.Inum)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(doc, gotDoc); diff != "" {
		t.Fatalf("mismatch: %s", diff)
	}

	// Generate fresh metadata for update
	metadataReader, err = GenerateMetadataReader(GetCurrentHost())
	if err != nil {
		t.Fatal(err)
	}

	metadataBytes, err = io.ReadAll(metadataReader)
	if err != nil {
		t.Fatal(err)
	}
	file1 := bytes.NewReader(metadataBytes)

	doc.Description = "Updated description"
	if doc, err = c.UpdateJansAsset(ctx, *doc, file1); err != nil {
		t.Fatal(err)
	}

	gotDoc, err = c.GetJansAsset(ctx, doc.Inum)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(doc, gotDoc); diff != "" {
		t.Fatalf("mismatch: %s", diff)
	}

	t.Cleanup(func() {
		if err := c.DeleteJansAsset(ctx, doc.Inum); err != nil {
			t.Fatal(err)
		}
	})
}
