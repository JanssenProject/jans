package jans

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_GetScriptByName(t *testing.T) {
	name := "discovery_java_params"
	responseBody := Script{
		Inum:       "0300-BA90",
		Name:       name,
		ScriptType: "discovery",
		Enabled:    true,
	}

	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		expectedPath := "/jans-config-api/api/v1/config/scripts/name/" + name
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

	result, err := client.GetScriptByName(context.Background(), name)
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if result.Name != responseBody.Name {
		t.Errorf("Expected name %s, got %s", responseBody.Name, result.Name)
	}
	if result.Inum != responseBody.Inum {
		t.Errorf("Expected inum %s, got %s", responseBody.Inum, result.Inum)
	}
}

func TestClient_GetScriptsByType(t *testing.T) {
	scriptType := "discovery"
	responseBody := struct {
		Entries []Script `json:"entries"`
	}{
		Entries: []Script{
			{Inum: "0300-BA90", Name: "discovery_java_params", ScriptType: scriptType},
		},
	}

	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		expectedPath := "/jans-config-api/api/v1/config/scripts/type/" + scriptType
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

	result, err := client.GetScriptsByType(context.Background(), scriptType)
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if len(result) != 1 {
		t.Fatalf("Expected 1 script, got %d", len(result))
	}
	if result[0].ScriptType != scriptType {
		t.Errorf("Expected scriptType %s, got %s", scriptType, result[0].ScriptType)
	}
}
