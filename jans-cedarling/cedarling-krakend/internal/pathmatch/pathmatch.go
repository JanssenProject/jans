// SPDX-License-Identifier: Apache-2.0

// Package pathmatch decides which requests the plugin authorizes. It has no
// cgo dependencies, so it can be tested without the Cedarling library.
package pathmatch

import (
	"path"
	"strings"
)

// Matches reports whether reqPath matches the configured endpoint, which may contain
// KrakenD-style parameters ({id} or :id) and a trailing catch-all (* or *name).
func Matches(pattern, reqPath string) bool {
	p := strings.Split(strings.Trim(pattern, "/"), "/")
	// The router may route either the raw or the cleaned path, so match both to fail closed.
	return segmentsMatch(p, strings.Split(strings.Trim(reqPath, "/"), "/")) ||
		segmentsMatch(p, strings.Split(strings.Trim(path.Clean("/"+reqPath), "/"), "/"))
}

func segmentsMatch(p, r []string) bool {
	for i, seg := range p {
		if i == len(p)-1 && strings.HasPrefix(seg, "*") {
			return true
		}
		if i >= len(r) {
			return false
		}
		if (strings.HasPrefix(seg, "{") && strings.HasSuffix(seg, "}")) || strings.HasPrefix(seg, ":") {
			if r[i] == "" {
				return false
			}
			continue
		}
		if seg != r[i] {
			return false
		}
	}
	return len(p) == len(r)
}
