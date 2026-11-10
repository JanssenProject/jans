package jans

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"slices"
	"testing"
)

func TestPushRequirePkce(t *testing.T) {
	fetched := func(v bool) *OidcClient {
		o := &OidcClient{RequirePkce: v, Attributes: &OidcClientAttribute{RequirePkce: v}}
		o.pullRequirePkce()
		return o
	}
	tests := []struct {
		name string
		o    *OidcClient
		want bool
	}{
		{"fetched, top-level disabled", func() *OidcClient { o := fetched(true); o.RequirePkce = false; return o }(), false},
		{"fetched, nested disabled", func() *OidcClient { o := fetched(true); o.Attributes.RequirePkce = false; return o }(), false},
		{"fetched, top-level enabled", func() *OidcClient { o := fetched(false); o.RequirePkce = true; return o }(), true},
		{"fetched, nested enabled", func() *OidcClient { o := fetched(false); o.Attributes.RequirePkce = true; return o }(), true},
		{"fresh, top-level enabled", &OidcClient{RequirePkce: true}, true},
		{"fresh, nested enabled", &OidcClient{Attributes: &OidcClientAttribute{RequirePkce: true}}, true},
		{"fresh, unset", &OidcClient{}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tt.o.pushRequirePkce()
			nested := tt.o.Attributes != nil && tt.o.Attributes.RequirePkce
			if nested != tt.want {
				t.Errorf("attributes.requirePkce = %v, want %v", nested, tt.want)
			}
		})
	}
}

func TestUpdateOidcClientRefreshesPkceBaseline(t *testing.T) {
	var sent []bool
	server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
		body, _ := io.ReadAll(r.Body)
		var got OidcClient
		if err := json.Unmarshal(body, &got); err != nil {
			t.Fatalf("bad request body: %v", err)
		}
		sent = append(sent, got.Attributes != nil && got.Attributes.RequirePkce)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write(body)
	}))
	defer server.Close()

	c, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
	if err != nil {
		t.Fatal(err)
	}
	ctx := context.Background()

	o := &OidcClient{Inum: "1", Attributes: &OidcClientAttribute{}}
	o.pullRequirePkce()

	o.RequirePkce = true
	if _, err := c.UpdateOidcClient(ctx, o); err != nil {
		t.Fatal(err)
	}
	o.Attributes.RequirePkce = false
	if _, err := c.UpdateOidcClient(ctx, o); err != nil {
		t.Fatal(err)
	}

	if want := []bool{true, false}; !slices.Equal(sent, want) {
		t.Errorf("sent attributes.requirePkce = %v, want %v", sent, want)
	}
}
