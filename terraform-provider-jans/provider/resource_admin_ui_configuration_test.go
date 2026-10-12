package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceAdminUIConfiguration_Mapping(t *testing.T) {

	schema := resourceAdminUIConfiguration()

	data := schema.Data(nil)

	cfg := jans.AdminUIConfiguration{
		AuthServerHost:        "https://example.jans.io",
		AuthzBaseUrl:          "https://example.jans.io/jans-auth/authorize",
		ClientId:              "admin-ui-client",
		ResponseType:          "code",
		Scope:                 "openid profile",
		RedirectUrl:           "https://example.jans.io/admin",
		AcrValues:             "basic",
		FrontChannelLogoutUrl: "https://example.jans.io/admin/logout",
		PostLogoutRedirectUri: "https://example.jans.io/admin",
		EndSessionEndpoint:    "https://example.jans.io/jans-auth/end_session",
		SessionTimeoutInMins:  30,
		AllowSmtpKeystoreEdit: true,
		AdditionalParameters: []jans.KeyValuePair{
			{Key: "ui_locales", Value: "en"},
		},
		CedarlingLogType: "std_out",
	}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.AdminUIConfiguration{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}
}
