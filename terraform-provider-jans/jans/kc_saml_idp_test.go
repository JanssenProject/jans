package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestCreateIDP(t *testing.T) {
	c, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	idp := &IdentityProvider{
		CreatorId:              "admin",
		Description:            "Test IDP",
		DisplayName:            "Test IDP",
		Name:                   "test-idp",
		Realm:                  "jans",
		NameIDPolicyFormat:     "urn:mace:shibboleth:1.0:nameIdentifier",
		IdpEntityId:            "https://moabu-promoted-loon.gluu.info/idp/shibboleth",
		SingleSignOnServiceUrl: "https://moabu-promoted-loon.gluu.info/idp/profile/SAML2/POST/SSO",
	}

	idp, err = c.CreateIDP(ctx, idp, nil)
	if err != nil {
		t.Fatal(err)
	}
	defer func() {
		if err := c.DeleteIDP(ctx, idp.Inum); err != nil {
			t.Fatal(err)
		}
	}()

	idp.Description = "Updated description"

	idp, err = c.UpdateIDP(ctx, idp, nil)
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
