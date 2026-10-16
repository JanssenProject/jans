package jans

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_GetPlugin(t *testing.T) {
	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		expectedPath := "/jans-config-api/api/v1/plugin/fido2"
		if r.URL.Path != expectedPath {
			t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
		}
		if r.Method != http.MethodGet {
			t.Errorf("Expected GET method, got %s", r.Method)
		}
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte("true"))
	}))
	defer server.Close()

	client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}

	deployed, err := client.GetPlugin(context.Background(), "fido2")
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if !deployed {
		t.Errorf("Expected deployed=true, got false")
	}
}
