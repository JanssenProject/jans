package jans

import (
        "bytes"
        "context"
        "fmt"
        "io"
        "net/url"
        "strings"
        "testing"

        "github.com/google/go-cmp/cmp"
)

func TestCreateIDP(t *testing.T) {
        c, err := NewInsecureClient(host, user, pass)
        if err != nil {
                t.Fatal(err)
        }

        ctx := context.Background()

        // Get current host for dynamic metadata generation
        currentHost := GetCurrentHost()
        parsedURL, err := url.Parse(currentHost)
        if err != nil {
                t.Fatal(err)
        }
        hostDomain := parsedURL.Host

        idp := &IdentityProvider{
                CreatorId:              "admin",
                Description:            "Test IDP",
                DisplayName:            "Test IDP",
                Name:                   "test-idp",
                Realm:                  "jans",
                NameIDPolicyFormat:     "urn:mace:shibboleth:1.0:nameIdentifier",
                IdpEntityId:            fmt.Sprintf("https://%s/idp/shibboleth", hostDomain),
                SingleSignOnServiceUrl: fmt.Sprintf("https://%s/idp/profile/SAML2/POST/SSO", hostDomain),
        }

        // Generate metadata dynamically with current host
        metadataReader, err := GenerateMetadataReader(currentHost)
        if err != nil {
                t.Fatalf("could not generate metadata: %v", err)
        }

        // Convert reader to ReadSeeker for the API
        metadataBytes, err := io.ReadAll(metadataReader)
        if err != nil {
                t.Fatal(err)
        }
        file := bytes.NewReader(metadataBytes)

        idp, err = c.CreateIDP(ctx, idp, file)
        if err != nil {
                if strings.Contains(err.Error(), "scope not granted") || strings.Contains(err.Error(), "not found") {
                        t.Skipf("Keycloak SAML feature not available or scope not granted: %v", err)
                }
                t.Fatal(err)
        }
        defer func() {
                if err := c.DeleteIDP(ctx, idp.Inum); err != nil {
                        t.Fatal(err)
                }
        }()

        idp.Description = "Updated description"

        // Generate fresh metadata for update
        metadataReader, err = GenerateMetadataReader(currentHost)
        if err != nil {
                t.Fatalf("could not generate metadata for update: %v", err)
        }

        metadataBytes, err = io.ReadAll(metadataReader)
        if err != nil {
                t.Fatal(err)
        }
        file = bytes.NewReader(metadataBytes)

        idp, err = c.UpdateIDP(ctx, idp, file)
        if err != nil {
                t.Fatal(err)
        }

        gotIdp, err := c.GetIDP(ctx, idp.Inum)
        if err != nil {
                t.Fatal(err)
        }

        if diff := cmp.Diff(idp, gotIdp); diff != "" {
                t.Errorf("IDP mismatch (-want +got):\n%s", diff)
        }
}
