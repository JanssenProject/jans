// SPDX-License-Identifier: Apache-2.0

package pathmatch

import (
	"errors"
	"testing"
)

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
			pattern, err := Parse(tt.pattern)
			if err != nil {
				t.Fatalf("Parse(%q) failed: %v", tt.pattern, err)
			}
			if got := pattern.Matches(tt.reqPath); got != tt.want {
				t.Errorf("Matches(%q, %q) = %v, want %v", tt.pattern, tt.reqPath, got, tt.want)
			}
		})
	}
}

// A segment the matcher would compare as a literal, while the router reads a parameter
// in it, never matches, so the plugin would pass such requests on unauthorized.
func TestParseRejectsSegmentsTheRouterReadsDifferently(t *testing.T) {
	tests := []struct {
		name    string
		pattern string
	}{
		{"brace parameter inside a segment", "/file.{ext}"},
		{"brace parameter after a prefix", "/api/v{version}/users"},
		{"two brace parameters in a segment", "/api/{a}{b}"},
		{"unclosed brace", "/api/{id"},
		{"stray closing brace", "/api/id}"},
		{"colon parameter inside a segment", "/api/items:batch"},
		{"second colon in a colon parameter", "/api/:id:x"},
		{"catch-all inside a segment", "/api/file*"},
		{"catch-all before the last segment", "/api/*/items"},
		{"brace parameter without a name", "/api/{}"},
		{"colon parameter without a name", "/api/:"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := Parse(tt.pattern)
			if !errors.Is(err, ErrUnsupportedSegment) {
				t.Errorf("Parse(%q) error = %v, want ErrUnsupportedSegment", tt.pattern, err)
			}
		})
	}
}
