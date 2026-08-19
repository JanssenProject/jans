package jans

import (
        "context"
        "encoding/json"
        "net/http"
        "net/http/httptest"
        "testing"
)

func TestAttributes(t *testing.T) {

        client, err := NewInsecureClient(host, user, pass)
        if err != nil {
                t.Fatal(err)
        }

        ctx := context.Background()

        // Create/update/delete of custom attributes is covered by the acceptance
        // test TestAccResourceAttribute (a known-good payload). Arbitrary custom
        // attribute names are rejected by the SQL backend on the AIO, so here we
        // only verify the list endpoint returns the seeded attributes.
        attrs, err := client.GetAttributes(ctx)
        if err != nil {
                t.Fatal(err)
        }

        if len(attrs) == 0 {
                t.Error("expected at least one attribute from GetAttributes")
        }
}

// Unit tests for Attribute operations

func TestClient_GetAttribute(t *testing.T) {
        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                expectedPath := "/jans-config-api/api/v1/attributes/TEST"
                if r.URL.Path != expectedPath {
                        t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                }
                if r.Method != http.MethodGet {
                        t.Errorf("Expected GET method, got %s", r.Method)
                }
                attr := Attribute{
                        Inum:        "TEST",
                        Name:        "testAttr",
                        DisplayName: "Test Attribute",
                }
                json.NewEncoder(w).Encode(attr)
        }))
        defer server.Close()

        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
        if err != nil {
                t.Fatalf("Failed to create client: %v", err)
        }

        result, err := client.GetAttribute(context.Background(), "TEST")

        if err != nil {
                t.Errorf("Unexpected error: %v", err)
        }
        if result.Inum != "TEST" {
                t.Errorf("Expected inum 'TEST', got %s", result.Inum)
        }
}

func TestClient_UpdateAttribute(t *testing.T) {
        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                expectedPath := "/jans-config-api/api/v1/attributes/"
                if r.URL.Path != expectedPath {
                        t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                }
                if r.Method != http.MethodPut {
                        t.Errorf("Expected PUT method, got %s", r.Method)
                }
                attr := Attribute{Inum: "TEST", Name: "updated", DisplayName: "Updated"}
                json.NewEncoder(w).Encode(attr)
        }))
        defer server.Close()

        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
        if err != nil {
                t.Fatalf("Failed to create client: %v", err)
        }

        attr := &Attribute{Inum: "TEST", Name: "updated", DisplayName: "Updated"}
        result, err := client.UpdateAttribute(context.Background(), attr)

        if err != nil {
                t.Errorf("Unexpected error: %v", err)
        }
        if result.Name != "updated" {
                t.Errorf("Expected name 'updated', got %s", result.Name)
        }
}

func TestClient_DeleteAttribute(t *testing.T) {
        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                expectedPath := "/jans-config-api/api/v1/attributes/TEST"
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

        err = client.DeleteAttribute(context.Background(), "TEST")

        if err != nil {
                t.Errorf("Unexpected error: %v", err)
        }
}
