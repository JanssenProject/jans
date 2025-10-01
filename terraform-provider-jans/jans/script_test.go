// Removing hardcoded Dn and Inum from the test script creation.
package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestScripts(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	scripts, err := client.GetScripts(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(scripts) == 0 {
		t.Fatal("expected scripts, got none")
	}

	newScript := &Script{
		Name:                "test_script",
		Description:         "Test description",
		Script:              "# Test script\nprint('Hello World')",
		ScriptType:          "introspection",
		ProgrammingLanguage: "python",
		Level:               1,
		Revision:            1,
		Enabled:             false,
		Modified:            false,
		Internal:            false,
		LocationType:        "db",
		ModuleProperties: []SimpleCustomProperty{
			{
				Value1: "location_type",
				Value2: "db",
			},
		},
	}

	createdScript, err := client.CreateScript(ctx, newScript)
	if err != nil {
		t.Fatal(err)
	}
	newScript.Inum = createdScript.Inum //Assign the inum after creation to compare.
	newScript.Dn = createdScript.Dn     //Assign the dn after creation to compare.

	loadScript, err := client.GetScript(ctx, newScript.Inum)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		if err := client.DeleteScript(ctx, newScript.Inum); err != nil {
			t.Fatal(err)
		}
	})

	// for new script a default source code is set, and BaseDN is server-generated, so we need to ignore them
	filter := cmp.FilterPath(func(p cmp.Path) bool {
		return p.String() == "Script" || p.String() == "BaseDN"
	}, cmp.Ignore())

	if diff := cmp.Diff(newScript, loadScript, filter); diff != "" {
		t.Errorf("Got different script after mapping: %s", diff)
	}

	loadScript.Name = "test_script"
	loadScript.LocationType = "file"

	updatedScript, err := client.UpdateScript(ctx, loadScript)
	if err != nil {
		t.Fatal(err)
	}

	if updatedScript.Name != loadScript.Name {
		t.Errorf("script name not updated")
	}

	if updatedScript.LocationType != loadScript.LocationType {
		t.Errorf("script location type not updated")
	}
}

func TestScriptTypes(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	types, err := client.GetScriptTypes(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(types) == 0 {
		t.Error("expected script types, got none")
	}
}
