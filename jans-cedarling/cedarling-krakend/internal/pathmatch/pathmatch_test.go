// SPDX-License-Identifier: Apache-2.0

package pathmatch

import "testing"

func TestMatches(t *testing.T) {
	tests := []struct {
		name    string
		pattern string
		reqPath string
		want    bool
	}{
		{"exact path", "/protected", "/protected", true},
		{"trailing slash on request", "/protected", "/protected/", true},
		{"trailing slash on pattern", "/protected/", "/protected", true},
		{"other path", "/protected", "/public", false},
		{"prefix of protected path", "/protected", "/prot", false},
		{"longer request", "/protected", "/protected/extra", false},
		{"shorter request", "/api/users", "/api", false},
		{"case differs", "/protected", "/Protected", false},

		{"brace parameter", "/api/{id}", "/api/42", true},
		{"colon parameter", "/api/:id", "/api/42", true},
		{"parameter with empty segment", "/api/{id}", "/api//", false},
		{"parameter missing", "/api/{id}", "/api", false},
		{"parameter then literal", "/api/{id}/items", "/api/42/items", true},
		{"parameter then other literal", "/api/{id}/items", "/api/42/orders", false},

		{"catch-all matches deeper path", "/api/*", "/api/a/b/c", true},
		{"named catch-all", "/api/*rest", "/api/a/b", true},
		{"catch-all matches bare prefix", "/api/*", "/api", true},
		{"catch-all other prefix", "/api/*", "/other/a", false},
		{"catch-all only in last segment", "/api/*/items", "/api/x/orders", false},

		{"double slash in request", "/api/users", "/api//users", true},
		{"leading double slash", "/protected", "//protected", true},
		{"dot segment", "/api/users", "/api/./users", true},
		{"dot-dot into protected path", "/protected", "/public/../protected", true},
		{"dot-dot out of protected path", "/protected", "/protected/../public", false},
		{"dot-dot above root", "/protected", "/../protected", true},

		{"root pattern matches root", "/", "/", true},
		{"root pattern only matches root", "/", "/protected", false},
		{"empty pattern only matches root", "", "/protected", false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := Matches(tt.pattern, tt.reqPath); got != tt.want {
				t.Errorf("Matches(%q, %q) = %v, want %v", tt.pattern, tt.reqPath, got, tt.want)
			}
		})
	}
}
