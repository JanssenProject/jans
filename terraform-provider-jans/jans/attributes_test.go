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

	_, err = client.GetAttributes(ctx)
	if err != nil {
		t.Fatal(err)
	}

	newAttribute := &Attribute{
		AdminCanAccess: true,
		AdminCanView:   true,
		AdminCanEdit:   true,
		ClaimName:      "test",
		DataType:       "string",
		Description:    "test",
		DisplayName:    "test",
		EditType:       []string{"user", "admin"},
		Inum:           "7AC6",
		Name:           "t",
		Origin:         "jansCustomPerson",
		Saml1Uri:       "urn:mace:dir:attribute-def:t",
		Saml2Uri:       "urn:oid:2.5.4.7",
		Urn:            "urn:mace:dir:attribute-def:t",
		UserCanAccess:  true,
		UserCanEdit:    true,
		UserCanView:    true,
		ViewType:       []string{"user", "admin"},
		Status:         "inactive",
	}

	createdAttribute, err := client.CreateAttribute(ctx, newAttribute)
	if err != nil {
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
