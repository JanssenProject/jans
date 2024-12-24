package jans

import (
	"context"
	"embed"
	_ "embed"
	"testing"

	"github.com/google/go-cmp/cmp"
)

//go:embed testdata/metadata.xml
var testFile embed.FS

func TestCreateJansAsset(t *testing.T) {
	t.Skip("Service not implemented")
	c, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	file, err := testFile.Open("testdata/metadata.xml")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		file.Close()
	})

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

	file1, err := testFile.Open("testdata/metadata.xml")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		file1.Close()
	})

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
