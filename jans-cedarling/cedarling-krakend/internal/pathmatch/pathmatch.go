// SPDX-License-Identifier: Apache-2.0

// Package pathmatch decides which requests the plugin authorizes. It has no
// cgo dependencies, so it can be tested without the Cedarling library.
package pathmatch

import (
	"errors"
	"fmt"
	"path"
	"strings"
)

// ErrUnsupportedSegment is returned for an endpoint segment the matcher cannot
// compare the way the router does, such as /file.{ext}. Comparing it as a literal
// would never match, and the plugin would pass those requests on unauthorized.
var ErrUnsupportedSegment = errors.New(
	"unsupported path segment: a parameter must be a whole segment and a catch-all the last one")

// Pattern is a protected endpoint whose segments Matches compares the same way
// as the router.
type Pattern struct {
	raw      string
	segments []string
}

// Parse accepts KrakenD-style parameters ({id} or :id) that fill a whole segment
// and a trailing catch-all (* or *name).
func Parse(pattern string) (Pattern, error) {
	segments := strings.Split(strings.Trim(pattern, "/"), "/")
	for i, seg := range segments {
		if !isSupported(seg, i == len(segments)-1) {
			return Pattern{}, fmt.Errorf("%w: %q in %q", ErrUnsupportedSegment, seg, pattern)
		}
	}
	return Pattern{raw: pattern, segments: segments}, nil
}

func isSupported(seg string, last bool) bool {
	switch {
	case isBraceParam(seg):
		return !strings.ContainsAny(seg[1:len(seg)-1], "{}")
	case strings.HasPrefix(seg, "*"):
		return last
	default:
		// The router reads ':' or '*' anywhere in a segment as the start of a parameter.
		return !strings.ContainsAny(seg, "{}*") && !strings.Contains(strings.TrimPrefix(seg, ":"), ":")
	}
}

func isBraceParam(seg string) bool {
	return strings.HasPrefix(seg, "{") && strings.HasSuffix(seg, "}") && len(seg) >= 2
}

// String returns the endpoint as configured.
func (p Pattern) String() string {
	return p.raw
}

// Matches reports whether reqPath is a request to the endpoint.
func (p Pattern) Matches(reqPath string) bool {
	// The router may route either the raw or the cleaned path, so match both to fail closed.
	return segmentsMatch(p.segments, strings.Split(strings.Trim(reqPath, "/"), "/")) ||
		segmentsMatch(p.segments, strings.Split(strings.Trim(path.Clean("/"+reqPath), "/"), "/"))
}

func segmentsMatch(p, r []string) bool {
	for i, seg := range p {
		if i == len(p)-1 && strings.HasPrefix(seg, "*") {
			return true
		}
		if i >= len(r) {
			return false
		}
		if isBraceParam(seg) || strings.HasPrefix(seg, ":") {
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
