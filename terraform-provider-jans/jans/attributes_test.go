package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestAttributes(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	attrs, err := client.GetAttributes(ctx)
	if err != nil {
		t.Fatal(err)
	}

	for _, attr := range attrs {
		if attr.Name == "l" {
			if err = client.DeleteAttribute(ctx, attr.Inum); err != nil {
				t.Fatal(err)
			}
		}
	}

	newAttribute := &Attribute{
		Inum:           "7AC6",
		Name:           "testCustomAttribute",
		DisplayName:    "Test Custom Attribute",
		Description:    "Test custom attribute for unit testing",
		Origin:         "jansCustomPerson",
		DataType:       "string",
		EditType:       []string{"user", "admin"},
		ViewType:       []string{"user", "admin"},
		ClaimName:      "test_custom_attribute",
		Status:         "inactive",
		Saml1Uri:       "urn:mace:dir:attribute-def:testCustomAttribute",
		Saml2Uri:       "urn:oid:2.5.4.999",
		Urn:            "urn:mace:dir:attribute-def:testCustomAttribute",
		Required:       true,
		AdminCanAccess: true,
		AdminCanView:   true,
		AdminCanEdit:   true,
		UserCanAccess:  true,
		UserCanView:    true,
		UserCanEdit:    true,
	}

	createdAttribute, err := client.CreateAttribute(ctx, newAttribute)
	if err != nil {
		// Check if it's a schema validation error - this may be expected in some environments
		if err.Error() == "post request failed: did not get correct response code: 406 Not Acceptable" {
			t.Skipf("Cannot create custom attributes in this environment - schema validation failed: %v", err)
		}
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteAttribute(ctx, createdAttribute.Inum)
	})

	// have to set the generated IDs before comparing
	newAttribute.Inum = createdAttribute.Inum
	newAttribute.Dn = createdAttribute.Dn
	newAttribute.BaseDn = createdAttribute.BaseDn

	if diff := cmp.Diff(newAttribute, createdAttribute); diff != "" {
		t.Errorf("Got different attribute after creating: %v", diff)
	}

	createdAttribute.Description = "test2"
	updatedAttribute, err := client.UpdateAttribute(ctx, createdAttribute)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(createdAttribute, updatedAttribute); diff != "" {
		t.Errorf("Got different attribute after updating: %v", diff)
	}

	// delete attribute
	err = client.DeleteAttribute(ctx, createdAttribute.Inum)
	if err != nil {
		t.Fatal(err)
	}

}
