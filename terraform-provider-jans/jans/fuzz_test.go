package jans

import (
	"encoding/json"
	"reflect"
	"strings"
	"testing"
)

// FuzzPatchRequestJSON fuzzes JSON deserialization of PatchRequest to catch
// panics or crashes on malformed input.
func FuzzPatchRequestJSON(f *testing.F) {
	f.Add([]byte(`{"op":"replace","path":"/name","value":"test"}`))
	f.Add([]byte(`{"op":"add","path":"/","value":null}`))
	f.Add([]byte(`{}`))

	f.Fuzz(func(t *testing.T, data []byte) {
		var pr PatchRequest
		_ = json.Unmarshal(data, &pr)
	})
}

// FuzzCreatePatches fuzzes createPatches using JsonWebKey so recursivePatchFromEntity
// sees typed inputs including strings and ints across multiple fields.
// Seed corpus covers: single-field change, multi-field change, identical orig/updated (0 patches),
// and empty strings.
func FuzzCreatePatches(f *testing.F) {
	// single-field change
	f.Add("key1", "RS256", "sig", "kid1", 0, "key1_updated", "RS256", "sig", "kid1", 0)
	// multi-field change
	f.Add("key1", "RS256", "sig", "kid1", 1000, "key2", "ES256", "enc", "kid2", 2000)
	// identical orig/updated — expect 0 patches
	f.Add("key1", "RS256", "sig", "kid1", 42, "key1", "RS256", "sig", "kid1", 42)
	// empty strings
	f.Add("", "", "", "", 0, "", "", "", "", 0)

	f.Fuzz(func(t *testing.T,
		origName, origAlg, origUse, origKid string, origExp int,
		updName, updAlg, updUse, updKid string, updExp int,
	) {
		orig := JsonWebKey{
			Name: origName, Alg: origAlg, Use: origUse, Kid: origKid, Exp: origExp,
		}
		updated := JsonWebKey{
			Name: updName, Alg: updAlg, Use: updUse, Kid: updKid, Exp: updExp,
		}

		patches, err := createPatches(updated, orig)
		if err != nil {
			return
		}

		// Identical structs must produce no patches.
		if reflect.DeepEqual(orig, updated) {
			if len(patches) != 0 {
				t.Errorf("createPatches returned %d patches for identical inputs, want 0", len(patches))
			}
			return
		}

		for _, p := range patches {
			if p.Path == "" {
				t.Errorf("patch has empty path: op=%q value=%v", p.Op, p.Value)
			}
			if !strings.HasPrefix(p.Path, "/") {
				t.Errorf("patch path %q does not start with '/'", p.Path)
			}
			if p.Op != "replace" {
				t.Errorf("unexpected patch op %q at path %q value=%v; createPatches should only produce 'replace'", p.Op, p.Path, p.Value)
			}
		}
	})
}
