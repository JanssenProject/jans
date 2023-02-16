package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestUser(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetUsers(ctx)
	if err != nil {
		t.Error(err)
	}

	newUser := &User{
		DisplayName:       "test-user",
		Schemas:           []string{"urn:ietf:params:scim:schemas:core:2.0:User"},
		ExternalId:        "ext1234",
		UserName:          "test-user",
		NickName:          "test-user",
		ProfileUrl:        "https://localhost:9443/scim/v2/Users/1234",
		Title:             "Mr",
		UserType:          "Employee",
		PreferredLanguage: "en",
		Locale:            "en_US",
		Timezone:          "UTC",
		Active:            true,
		Password:          "password",
		Name: Name{
			FamilyName:      "Doe",
			GivenName:       "John",
			MiddleName:      "M",
			HonorificPrefix: "Dr",
			HonorificSuffix: "Jr",
		},
		Emails: []Email{
			{
				Value:   "john.doe@jans.io",
				Display: "Work",
				Type:    "work",
				Primary: true,
			},
		},
		PhoneNumbers: []PhoneNumber{
			{
				Value:   "1234567890",
				Display: "Mobile",
				Type:    "work",
				Primary: true,
			},
		},
		Ims: []InstantMessagingAddress{
			{
				Value:   "@john.doe",
				Display: "Messenger",
				Type:    "Messenger",
				Primary: true,
			},
		},
		Photos: []Photo{
			{
				Value:   "https://localhost:9443/scim/v2/Users/1234/photo",
				Display: "Photo",
				Type:    "photo",
				Primary: true,
			},
		},
		Addresses: []Address{
			{
				Formatted:     "123 Main St",
				StreetAddress: "123 Main St",
				Locality:      "New York",
				Region:        "NY",
				PostalCode:    "12345",
				Country:       "US",
				Type:          "work",
				Primary:       true,
			},
		},
		// Groups: []GroupReference{
		// {
		// 	Value:   "1234",
		// 	Display: "test-group",
		// 	Type:    "Group",
		// 	Ref:     "https://localhost:9443/scim/v2/Groups/1234",
		// },
		// },
		Entitlements: []Entitlement{
			{
				Value:   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:entitlement",
				Display: "Entitlement",
				Type:    "entitlement",
				Primary: true,
			},
		},
		Roles: []Role{
			{
				Value:   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:role",
				Display: "Role",
				Type:    "role",
				Primary: true,
			},
		},
		X509Certificates: []X509Certificate{
			{
				Value:   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:x509Certificates",
				Display: "X509Certificates",
				Type:    "PEM",
				Primary: true,
			},
		},
	}

	createdUser, err := client.CreateUser(ctx, newUser)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteUser(ctx, createdUser.ID)
	})

	// some fields are not returned, or are updated by the server
	// update them so we can compare the rest
	filter := cmp.FilterPath(func(p cmp.Path) bool {
		ps := p.String()
		return ps == "ID" || ps == "Meta" || ps == "Password" || ps == "Name.Formatted"
	}, cmp.Ignore())

	if diff := cmp.Diff(createdUser, newUser, filter); diff != "" {
		t.Errorf("created user does not match expected user: %s", diff)
	}

	loadedUser, err := client.GetUser(ctx, createdUser.ID)
	if err != nil {
		t.Fatal(err)
	}

	// TODO: this is broken in the backend
	filter = cmp.FilterPath(func(p cmp.Path) bool {
		ps := p.String()
		return ps == "ID" || ps == "Meta" || ps == "Name.Formatted" ||
			ps == "Password" || ps == "X509Certificates"
	}, cmp.Ignore())
	if diff := cmp.Diff(loadedUser, createdUser, filter); diff != "" {
		t.Errorf("loaded user does not match created user: %s", diff)
	}

	createdUser.DisplayName = "test-user-updated"
	updatedUser, err := client.UpdateUser(ctx, createdUser)
	if err != nil {
		t.Fatal(err)
	}

	if updatedUser.DisplayName != createdUser.DisplayName {
		t.Errorf("expected user display name %s, got %s", createdUser.DisplayName, updatedUser.DisplayName)
	}

	if err := client.DeleteUser(ctx, createdUser.ID); err != nil {
		t.Fatal(err)
	}

}

func TestGroupAssignment(t *testing.T) {

	if skipKnownFailures {
		t.SkipNow()
	}

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	newGroup := Group{
		Schemas:     []string{"urn:ietf:params:scim:schemas:core:2.0:Group"},
		DisplayName: "test-group",
	}

	createdGroup, err := client.CreateGroup(ctx, &newGroup)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteGroup(ctx, createdGroup.ID)
	})

	newUser := &User{
		DisplayName:       "test-user",
		Schemas:           []string{"urn:ietf:params:scim:schemas:core:2.0:User"},
		ExternalId:        "ext1234",
		UserName:          "test-user",
		NickName:          "test-user",
		ProfileUrl:        "https://localhost:9443/scim/v2/Users/1234",
		Title:             "Mr",
		UserType:          "Employee",
		PreferredLanguage: "en",
		Locale:            "en_US",
		Timezone:          "UTC",
		Active:            true,
		Password:          "password",
	}

	createdUser, err := client.CreateUser(ctx, newUser)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteUser(ctx, createdUser.ID)
	})

	createdGroup.Members = []Member{
		{
			Value:   createdUser.ID,
			Display: createdUser.DisplayName,
			Type:    "User",
			Ref:     createdUser.Meta.Location,
		},
	}

	updatedGroup, err := client.UpdateGroup(ctx, createdGroup)
	if err != nil {
		t.Fatal(err)
	}

	if len(updatedGroup.Members) != 1 {
		t.Errorf("expected 1 member, got %d", len(updatedGroup.Members))
	}

	loadedGroup, err := client.GetGroup(ctx, createdGroup.ID)
	if err != nil {
		t.Fatal(err)
	}

	if len(loadedGroup.Members) != 1 {
		t.Errorf("expected 1 member, got %d", len(loadedGroup.Members))
	}

	loadedUser, err := client.GetUser(ctx, createdUser.ID)
	if err != nil {
		t.Fatal(err)
	}

	if len(loadedUser.Groups) != 1 {
		t.Errorf("expected 1 group, got %d", len(loadedUser.Groups))
	}

}
