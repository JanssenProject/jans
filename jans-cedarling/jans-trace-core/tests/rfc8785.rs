// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! RFC 8785 conformance against the reference data of the JCS authors.

use std::fs;
use std::path::Path;

use jans_trace_core::Limits;

const CASES: [&str; 6] = [
    "arrays",
    "french",
    "structures",
    "unicode",
    "values",
    "weird",
];

#[test]
fn reference_vectors_match_byte_for_byte() {
    let dir = Path::new(env!("CARGO_MANIFEST_DIR")).join("tests/rfc8785");
    for case in CASES {
        let input = fs::read_to_string(dir.join("input").join(format!("{case}.json")))
            .expect("reference input is readable UTF-8");
        let expected = fs::read_to_string(dir.join("output").join(format!("{case}.json")))
            .expect("reference output is readable UTF-8");
        let actual = jans_trace_core::json::canonicalize_json(&input, &Limits::default())
            .expect("reference input canonicalizes");
        assert_eq!(
            actual,
            expected.trim_end(),
            "canonical text for `{case}` differ from the RFC 8785 reference"
        );
    }
}
