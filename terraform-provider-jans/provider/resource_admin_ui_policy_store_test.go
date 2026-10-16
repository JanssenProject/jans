package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceAdminUIPolicyStore_Mapping(t *testing.T) {

	schema := resourceAdminUIPolicyStore()

	data := schema.Data(nil)

	store := jans.AdminUIPolicyStore{
		Dn:           "inum=1234,ou=policystore,o=jans",
		Inum:         "1234",
		Displayname:  "example store",
		Description:  "example policy store",
		PolicyStore:  "UEsDBBQACAgIAA==",
		JansUsrDN:    "inum=user,ou=people,o=jans",
		JansStatus:   "active",
		CreationDate: "2026-01-01T00:00:00Z",
		JansLastUpd:  "2026-01-02T00:00:00Z",
	}

	if err := toSchemaResource(data, store); err != nil {
		t.Fatal(err)
	}

	newStore := jans.AdminUIPolicyStore{}

	if err := fromSchemaResource(data, &newStore); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(store, newStore); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}
