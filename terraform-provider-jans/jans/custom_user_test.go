package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestCustomUsers(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetCustomUsers(ctx)
	if err != nil {
		t.Error(err)
	}

	usr := CustomUser{
		UserID: "exampleUsr1",
		CustomAttributes: []CustomAttribute{
			{
				Name:         "birthdate",
				MultiValued:  false,
				Values:       []string{`"2000-12-31T00:00:00"`},
				Value:        `"2000-12-31T00:00:00"`,
				DisplayValue: "Sun Dec 31 00:00:00 UTC 2000",
			},
			{
				Name:         "emailVerified",
				MultiValued:  false,
				Values:       []string{"true"},
				Value:        "true",
				DisplayValue: "true",
			},
			{
				Name:         "jansAdminUIRole",
				MultiValued:  true,
				Values:       []string{`"api-admin"`},
				Value:        `"api-admin"`,
				DisplayValue: "api-admin",
			},
			{
				Name:         "memberOf",
				MultiValued:  true,
				Values:       []string{`"inum=60B7,ou=groups,o=jans"`},
				Value:        `"inum=60B7,ou=groups,o=jans"`,
				DisplayValue: "inum=60B7,ou=groups,o=jans",
			},
			{
				Name:         "middleName",
				MultiValued:  false,
				Values:       []string{`"Test USer 1"`},
				Value:        `"Test USer 1"`,
				DisplayValue: "Test USer 1",
			},
			{
				Name:         "nickname",
				MultiValued:  false,
				Values:       []string{`"Test USer 1"`},
				Value:        `"Test USer 1"`,
				DisplayValue: "Test USer 1",
			},
			{
				Name:         "sn",
				MultiValued:  false,
				Values:       []string{`"exampleUsr1"`},
				Value:        `"exampleUsr1"`,
				DisplayValue: "exampleUsr1",
			},
		},
		CustomObjectClasses: []string{
			"top",
			"jansCustomPerson",
		},
		Mail:                "exampleUsr1@jans.io",
		OxAuthPersistentJwt: []string{"jwt1", "jwt2"},
		DisplayName:         "Default Test User",
		JansStatus:          "active",
		UserPassword:        "pwd123",
		GivenName:           "exampleUsr1",
	}

	createdUser, err := client.CreateCustomUser(ctx, &usr)
	if err != nil {
		t.Fatal(err)
	}

	// password is hashed, so we can't compare it
	filter := cmp.FilterPath(func(p cmp.Path) bool {
		attr := p.String()
		return attr == "CreatedAt" || attr == "BaseDn" || attr == "Dn" ||
			attr == "UserPassword" || attr == "Inum" || attr == "UpdatedAt" ||
			attr == "DisplayValue"
	}, cmp.Ignore())

	if diff := cmp.Diff(&usr, createdUser, filter); diff != "" {
		t.Errorf("Got different user after create: %s", diff)
	}

	loadedUser, err := client.GetCustomUser(ctx, createdUser.Inum)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(createdUser, loadedUser); diff != "" {
		t.Errorf("Got different user after load: %s", diff)
	}

	loadedUser.DisplayName = "Updated Test User"
	loadedUser.Mail = "exampleUser1@jans.io"
	updatedUser, err := client.UpdateCustomUser(ctx, loadedUser)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(loadedUser, updatedUser, filter); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	err = client.DeleteCustomUser(ctx, loadedUser.Inum)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteCustomUser(ctx, loadedUser.Inum)
	})

}
