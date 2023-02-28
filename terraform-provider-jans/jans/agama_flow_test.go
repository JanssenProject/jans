package jans

import (
	"context"
	"errors"
	"testing"
)

func TestAgamaFlow(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetAgamaFlows(ctx)
	if err != nil {
		t.Error(err)
	}

	newFlow := AgamaFlow{
		Qname:   "test",
		Enabled: true,
		Source: `//This is a comment
Flow test
    Basepath "hello"

in = { name: "John" }
RRF "index.ftlh" in

Log "Done!"
Finish "john_doe"
`,
	}

	_, err = client.CreateAgamaFlow(ctx, &newFlow)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteAgamaFlow(ctx, newFlow.Qname)
	})

	loadedFlow, err := client.GetAgamaFlow(ctx, "test")
	if err != nil {
		t.Fatal(err)
	}

	if newFlow.Qname != loadedFlow.Qname {
		t.Errorf("expected qname %s, got %s", newFlow.Qname, loadedFlow.Qname)
	}

	if newFlow.Enabled != loadedFlow.Enabled {
		t.Errorf("expected enabled %t, got %t", newFlow.Enabled, loadedFlow.Enabled)
	}

	loadedFlow.Enabled = false
	loadedFlow.Source = newFlow.Source
	if err := client.UpdateAgamaFlow(ctx, loadedFlow); err != nil {
		t.Fatal(err)
	}

	updatedFlow, err := client.GetAgamaFlow(ctx, "test")
	if err != nil {
		t.Fatal(err)
	}

	if updatedFlow.Enabled != false {
		t.Errorf("expected enabled %t, got %t", false, updatedFlow.Enabled)
	}

	// delete
	if err = client.DeleteAgamaFlow(ctx, newFlow.Qname); err != nil {
		t.Fatal(err)
	}

	if _, err := client.GetAgamaFlow(ctx, "test"); !errors.Is(err, ErrorNotFound) {
		t.Errorf("expected 404 error, got %v", err)
	}

}
