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
		Dn:                  "inum=4A4E-4F3D,ou=scripts,o=jans",
		Inum:                "4A4E-4F3D",
		Name:                "test_script",
		Description:         "Test description",
		Script:              "",
		ScriptType:          "introspection",
		ProgrammingLanguage: "python",
		Level:               1,
		Revision:            1,
		Enabled:             true,
		Modified:            false,
		Internal:            false,
		LocationType:        "db",
		LocationPath:        "string",
		BaseDN:              "inum=4A4E-4F3D,ou=scripts,o=jans",
		ModuleProperties: []SimpleCustomProperty{
			{
				Value1: "location_type",
				Value2: "db",
			},
			{
				Value1: "location_option",
				Value2: "foo",
			},
			{
				Value1: "location_path",
				Value2: "string",
			},
		},
	}

	_, err = client.CreateScript(ctx, newScript)
	if err != nil {
		t.Fatal(err)
	}

	loadScript, err := client.GetScript(ctx, newScript.Inum)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		if err := client.DeleteScript(ctx, newScript.Inum); err != nil {
			t.Fatal(err)
		}
	})

	// for new script a default source code is set, so we need to ignore it
	filter := cmp.FilterPath(func(p cmp.Path) bool {
		return p.String() == "Script"
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
