package jans

import (
	"context"
	"strings"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestSimpleCreate(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	newClient := OidcClient{
		RedirectUris: []string{"https://moabu-diverse-tiger.gluu.info"},
	}

	createdClient, err := client.CreateOidcClient(ctx, &newClient)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteOidcClient(ctx, createdClient.Inum)
	})

	if diff := cmp.Diff(&newClient, createdClient); diff != "" {
		t.Errorf("Got different configuration after create: %s", diff)
	}

}

func TestGetOIDCClients(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	clients, err := client.GetOidcClients(ctx)
	if err != nil {
		t.Fatal(err)
	}

	id := ""
	for _, c := range clients {
		if strings.HasPrefix(c.Inum, "1201") {
			id = c.Inum
			break
		}
	}

	oidcClient, err := client.GetOidcClient(ctx, id)
	if err != nil {
		t.Fatal(err)
	}

	expectedDn := "inum=" + id + ",ou=clients,o=jans"
	if oidcClient.Dn != expectedDn {
		t.Fatalf("expected '%s', got '%s'", expectedDn, oidcClient.Dn)
	}

}

func TestOIDCClient(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	newScope := &Scope{
		Dn:          "inum=3200.004CD5,ou=scopes,o=jans",
		Inum:        "3200.004BA5",
		DisplayName: "test groups.read",
		Id:          "https://jans.io/test/groups.read",
		Description: "Query test group resources",
		ScopeType:   "oauth",
		Attributes: ScopeAttribute{
			ShowInConfigurationEndpoint: true,
		},
		CreationDate: "2022-09-01T13:42:58",
		UmaType:      false,
		BaseDn:       "inum=3200.004CD5,ou=scopes,o=jans",
	}

	createdScope, err := client.CreateScope(ctx, newScope)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteScope(ctx, createdScope.Inum)
	})

	newClient := OidcClient{
		Dn:                                "inum=1201.d52300ed-8193-510e-b31d-5829f4af346e,ou=clients,o=jans",
		ClientSecret:                      "SEw7VOX8m9ah",
		FrontChannelLogoutUri:             "null",
		FrontChannelLogoutSessionRequired: false,
		RedirectUris: []string{
			"https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/.well-known/scim-configuration",
		},
		ClaimRedirectUris:       []string{"https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/.well-known/scim-configuration"},
		ResponseTypes:           []string{"code"},
		GrantTypes:              []string{"client_credentials"},
		ApplicationType:         "web",
		Contacts:                []string{"jane.doe@acme.net"},
		ClientName:              "test-client",
		LogoUri:                 "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/assets/images/logo.png",
		ClientUri:               "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/client",
		PolicyUri:               "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/policy",
		TosUri:                  "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/tos",
		SubjectType:             "pairwise",
		TokenEndpointAuthMethod: "client_secret_basic",
		DefaultAcrValues:        []string{"value1"},
		PostLogoutRedirectUris:  []string{"https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/start"},
		RequestUris:             []string{"https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/prereq"},
		Scopes: []string{
			createdScope.Dn,
		},
		Claims:                      []string{"inum"},
		TrustedClient:               false,
		PersistClientAuthorizations: false,
		IncludeClaimsInIdToken:      false,
		// CustomAttributes: []CustomAttribute{
		// 	{
		// 		Name:         "alternativeName",
		// 		MultiValued:  false,
		// 		Values:       []string{"SCIM client"},
		// 		DisplayValue: "SCIM client",
		// 		Value:        "SCIM client",
		// 	},
		// },
		RptAsJwt:              false,
		AccessTokenAsJwt:      false,
		AccessTokenSigningAlg: "RS256",
		Disabled:              false,
		AuthorizedOrigins:     []string{"https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info"},
		Attributes: &OidcClientAttribute{
			RunIntrospectionScriptBeforeJwtCreation: false,
			KeepClientAuthorizationAfterExpiration:  false,
			AllowSpontaneousScopes:                  false,
			BackchannelLogoutSessionRequired:        false,
			ParLifetime:                             600,
			RequirePar:                              false,
			JansDefaultPromptLogin:                  false,
		},
		AuthenticationMethod:  "client_secret_basic",
		TokenBindingSupported: false,
		BaseDn:                "inum=1201.d52300ed-8193-510e-b31d-5829f4af346e,ou=clients,o=jans",
		Inum:                  "1201.d52300ed-8193-510e-b31d-5829f4af346e",
	}

	createdClient, err := client.CreateOidcClient(ctx, &newClient)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteOidcClient(ctx, "1201.d52300ed-8193-510e-b31d-5829f4af346e")
	})

	if diff := cmp.Diff(&newClient, createdClient); diff != "" {
		t.Errorf("Got different configuration after create: %s", diff)
	}

	createdClient.Description = "updated description"
	updatedClient, err := client.UpdateOidcClient(ctx, createdClient)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(updatedClient, createdClient); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	if err := client.DeleteOidcClient(ctx, createdClient.Inum); err != nil {
		t.Fatal(err)
	}
}
