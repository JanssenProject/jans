package jans

import (
	"context"
	"io/ioutil"
	"os"
	"path/filepath"
	"runtime"
	"testing"
)

func TestAgamaDeployment(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	deployments, err := client.GetAgamaDeployments(ctx)
	if err != nil {
		t.Error(err)
	}

	if len(deployments) != 0 {
		t.Errorf("expected 0 deployments, got %d", len(deployments))
	}
	_ = client.DeleteAgamaDeployment(ctx, "test-deployment")

	_, filename, _, _ := runtime.Caller(0)
	dir := filepath.Dir(filepath.Dir(filename))

	testFile := dir + "/testdata/agama_project.gama"

	// read test file
	zipFile, err := os.Open(testFile)
	if err != nil {
		t.Fatalf("failed to open test file: %v", err)
	}

	// read file into byte array
	contents, err := ioutil.ReadAll(zipFile)
	if err != nil {
		t.Fatalf("failed to read test file: %v", err)
	}

	t.Cleanup(func() {
		// delete test deployment
		_ = client.DeleteAgamaDeployment(ctx, "testDeployment")
	})

	// upload test file
	if err = client.CreateAgamaDeployment(ctx, "testDeployment", contents); err != nil {
		t.Fatalf("failed to create test deployment: %v", err)
	}

	// get test deployment
	deployment, err := client.GetAgamaDeployment(ctx, "testDeployment")
	if err != nil {
		t.Fatalf("failed to get test deployment: %v", err)
	}

	if deployment.Name != "testDeployment" {
		t.Errorf("expected deployment name to be 'testDeployment', got '%s'", deployment.Name)
	}

	// delete test deployment
	err = client.DeleteAgamaDeployment(ctx, "testDeployment")
	if err != nil {
		t.Fatalf("failed to delete test deployment: %v", err)
	}

}
