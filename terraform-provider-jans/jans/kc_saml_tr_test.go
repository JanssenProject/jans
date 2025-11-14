package jans

import (
        "bytes"
        "context"
        _ "embed"
        "io"
        "strings"
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
                if strings.Contains(err.Error(), "scope not granted") || strings.Contains(err.Error(), "not found") {
                        t.Skipf("Keycloak SAML feature not available or scope not granted: %v", err)
                }
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
                Name:                  "My TR7",
                DisplayName:           "Some display name",
                Description:           "Some trust relationship",
                SPMetaDataSourceType:  "file",
                Status:                "",
                ValidationStatus:      "",
                ProfileConfigurations: ProfileConfigurations{},
        }

        // Generate metadata dynamically with current host
        metadataReader, err := GenerateMetadataReader(GetCurrentHost())
        if err != nil {
                t.Fatalf("could not generate metadata: %v", err)
        }

        // Convert reader to ReadSeeker for the API
        metadataBytes, err := io.ReadAll(metadataReader)
        if err != nil {
                t.Fatal(err)
        }
        file := bytes.NewReader(metadataBytes)

        tr, err = c.CreateTR(ctx, tr, file)
        if err != nil {
                if strings.Contains(err.Error(), "scope not granted") || strings.Contains(err.Error(), "not found") {
                        t.Skipf("Keycloak SAML feature not available or scope not granted: %v", err)
                }
                t.Fatal(err)
        }
        defer func() {
                if err := c.DeleteTR(ctx, tr.Inum); err != nil {
                        t.Fatal(err)
                }
        }()

        tr.Description = "Updated description"

        // Generate fresh metadata for update
        metadataReader, err = GenerateMetadataReader(GetCurrentHost())
        if err != nil {
                t.Fatalf("could not generate metadata for update: %v", err)
        }

        metadataBytes, err = io.ReadAll(metadataReader)
        if err != nil {
                t.Fatal(err)
        }
        file1 := bytes.NewReader(metadataBytes)

        if _, err := file1.Seek(0, 0); err != nil {
                t.Fatal(err)
        }

        tr, err = c.UpdateTR(ctx, tr, file1)
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
