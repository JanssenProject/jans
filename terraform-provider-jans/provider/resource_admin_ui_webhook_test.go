package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceAdminUIWebhook_Mapping(t *testing.T) {

	schema := resourceAdminUIWebhook()

	data := schema.Data(nil)

	webhook := jans.WebhookEntry{
		Dn:                    "inum=abcd,ou=webhook,o=jans",
		Inum:                  "abcd",
		DisplayName:           "example webhook",
		Description:           "example webhook description",
		Url:                   "https://example.jans.io/hook",
		HttpRequestBodyString: `{"event":"created"}`,
		HttpMethod:            "POST",
		JansEnabled:           true,
		HttpHeaders: []jans.KeyValuePair{
			{Key: "Content-Type", Value: "application/json"},
		},
		AuiFeatureIds:   []string{"user_management"},
		HttpRequestBody: map[string]string{"event": "created"},
		BaseDn:          "ou=webhook,o=jans",
	}

	if err := toSchemaResource(data, webhook); err != nil {
		t.Fatal(err)
	}

	newWebhook := jans.WebhookEntry{}

	if err := fromSchemaResource(data, &newWebhook); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(webhook, newWebhook); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}
