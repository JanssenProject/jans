package jans

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_GetJansAssets(t *testing.T) {
	responseBody := PagedResult[Document]{
		TotalEntryCount: 1,
		EntriesCount:    1,
		Entries: []Document{
			{Inum: "asset-1", FileName: "mermaid-extra.css", Enabled: true},
		},
	}

	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/jans-config-api/api/v1/jans-assets" {
			t.Errorf("Expected path '/jans-config-api/api/v1/jans-assets', got %s", r.URL.Path)
		}
		if r.Method != http.MethodGet {
			t.Errorf("Expected GET method, got %s", r.Method)
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(responseBody)
	}))
	defer server.Close()

	client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}

	result, err := client.GetJansAssets(context.Background())
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if len(result) != 1 || result[0].Inum != "asset-1" {
		t.Errorf("Unexpected assets result: %+v", result)
	}
}

func TestClient_GetJansAssetByName(t *testing.T) {
	name := "mermaid-extra.css"
	responseBody := PagedResult[Document]{
		TotalEntryCount: 1,
		EntriesCount:    1,
		Entries: []Document{
			{Inum: "asset-1", FileName: name},
		},
	}

	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		expectedPath := "/jans-config-api/api/v1/jans-assets/name/" + name
		if r.URL.Path != expectedPath {
			t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
		}
		if r.Method != http.MethodGet {
			t.Errorf("Expected GET method, got %s", r.Method)
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(responseBody)
	}))
	defer server.Close()

	client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}

	result, err := client.GetJansAssetByName(context.Background(), name)
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if len(result) != 1 || result[0].FileName != name {
		t.Errorf("Unexpected asset-by-name result: %+v", result)
	}
}

func TestClient_GetJansAssetDirMapping(t *testing.T) {
	responseBody := []AssetDirMapping{
		{Directory: "/opt/jans/jetty", Type: []string{"css", "js"}, Description: "static assets"},
	}

	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/jans-config-api/api/v1/jans-assets/asset-dir-mapping" {
			t.Errorf("Expected asset-dir-mapping path, got %s", r.URL.Path)
		}
		if r.Method != http.MethodGet {
			t.Errorf("Expected GET method, got %s", r.Method)
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(responseBody)
	}))
	defer server.Close()

	client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}

	result, err := client.GetJansAssetDirMapping(context.Background())
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if len(result) != 1 || result[0].Directory != "/opt/jans/jetty" {
		t.Errorf("Unexpected asset-dir-mapping result: %+v", result)
	}
}

func TestClient_GetJansAssetServices(t *testing.T) {
	responseBody := []string{"jans-auth", "jans-config-api"}

	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/jans-config-api/api/v1/jans-assets/services" {
			t.Errorf("Expected services path, got %s", r.URL.Path)
		}
		if r.Method != http.MethodGet {
			t.Errorf("Expected GET method, got %s", r.Method)
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(responseBody)
	}))
	defer server.Close()

	client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}

	result, err := client.GetJansAssetServices(context.Background())
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if len(result) != 2 {
		t.Errorf("Expected 2 services, got %d", len(result))
	}
}

func TestClient_GetJansAssetTypes(t *testing.T) {
	responseBody := []string{"css", "js", "jar"}

	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/jans-config-api/api/v1/jans-assets/asset-type" {
			t.Errorf("Expected asset-type path, got %s", r.URL.Path)
		}
		if r.Method != http.MethodGet {
			t.Errorf("Expected GET method, got %s", r.Method)
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(responseBody)
	}))
	defer server.Close()

	client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}

	result, err := client.GetJansAssetTypes(context.Background())
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if len(result) != 3 {
		t.Errorf("Expected 3 asset types, got %d", len(result))
	}
}
