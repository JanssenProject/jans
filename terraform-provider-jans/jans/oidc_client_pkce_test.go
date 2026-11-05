package jans

import "testing"

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
