package jans

import (
        "context"
        "strings"
        "testing"

        "github.com/google/go-cmp/cmp"
)

func TestCreateConfig(t *testing.T) {
        c, err := NewInsecureClient(host, user, pass)
        if err != nil {
                t.Fatal(err)
        }

        ctx := context.Background()

        config := &KCSAMLConfiguration{
                ApplicationName:                "SomeAPP",
                Enabled:                        true,
                IdpMetadataMandatoryAttributes: []string{"name"},
        }

        _, err = c.CreateKCSAMLConfiguration(ctx, config)
        if err != nil {
                if strings.Contains(err.Error(), "scope not granted") || strings.Contains(err.Error(), "not found") || strings.Contains(err.Error(), "Method Not Allowed") {
                        t.Skipf("Keycloak SAML feature not available or scope not granted: %v", err)
                }
                t.Fatal(err)
        }

        pr := []PatchRequest{
                {
                        Op:    "replace",
                        Path:  "/applicationName",
                        Value: "UpdatedAPP",
                },
        }
        config, err = c.PatchKCSAMLConfiguration(ctx, pr)
        if err != nil {
                t.Fatal(err)
        }

        gotConfig, err := c.GetKCSAMLConfiguration(ctx)
        if err != nil {
                t.Fatal(err)
        }

        if diff := cmp.Diff(config, gotConfig); diff != "" {
                t.Errorf("Config mismatch (-want +got):\n%s", diff)
        }
}
