package jans

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_DeleteClientAuthorization(t *testing.T) {
	userId := "122ff2df-911d-424b-bbfe-891a43a70e95"
	clientId := "2000.cc8b29ae-cb4a-49ea-b176-8695e53919d9"
	username := "admin"

	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		expectedPath := "/jans-config-api/api/v1/clients/authorizations/" + userId + "/" + clientId + "/" + username
		if r.URL.Path != expectedPath {
			t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
		}
		if r.Method != http.MethodDelete {
			t.Errorf("Expected DELETE method, got %s", r.Method)
		}
		w.WriteHeader(http.StatusNoContent)
	}))
	defer server.Close()

	client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}

	if err := client.DeleteClientAuthorization(context.Background(), userId, clientId, username); err != nil {
		t.Errorf("Unexpected error: %v", err)
	}
}
