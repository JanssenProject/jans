package jans

import (
	"bytes"
	"context"
	_ "embed"
	"testing"

	"github.com/google/go-cmp/cmp"
)

//go:embed testdata/metadata.xml
var metadata []byte

func TestQueryTRs(t *testing.T) {
	c, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	if trs, err := c.GetTRs(ctx); err != nil {
		t.Fatalf("could not get trs: %v", err)
	} else {
		for _, tr := range trs {
			if tr.Name == "My TR7" {
				if err = c.DeleteTR(ctx, tr.Inum); err != nil {
					t.Fatalf("could not delete tr: %v", err)
				}
			}
		}
	}
}

func TestCreateTR(t *testing.T) {
	c, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	tr := &TrustRelationship{
		Name:                 "My TR7",
		DisplayName:          "Some display name",
		Description:          "Some trust relationship",
		SPMetaDataSourceType: "file",
	}

	r := bytes.NewReader(metadata)

	tr, err = c.CreateTR(ctx, tr, r)
	if err != nil {
		t.Fatal(err)
	}
	defer func() {
		if err := c.DeleteTR(ctx, tr.Inum); err != nil {
			t.Fatal(err)
		}
	}()

	tr.Description = "Updated description"

	if _, err := r.Seek(0, 0); err != nil {
		t.Fatal(err)
	}

	tr, err = c.UpdateTR(ctx, tr, r)
	if err != nil {
		t.Fatal(err)
	}

	gotTr, err := c.GetTR(ctx, tr.Inum)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(tr, gotTr); diff != "" {
		t.Errorf("TR mismatch (-want +got):\n%s", diff)
	}
}
