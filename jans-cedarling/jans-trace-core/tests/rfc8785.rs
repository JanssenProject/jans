// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! RFC 8785 conformance against the reference data of the JCS authors.

use std::fs;
use std::path::Path;

use jans_trace_core::{ErrorCode, Limits};

const CASES: [&str; 6] = [
    "arrays",
    "french",
    "structures",
    "unicode",
    "values",
    "weird",
];

/// `values` holds `1E30`, outside the +/-(2^53-1) every TRACE number must fit.
const OUT_OF_PROFILE: &str = "values";

fn read(dir: &str, case: &str) -> String {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("tests/rfc8785")
        .join(dir)
        .join(format!("{case}.json"));
    fs::read_to_string(path).expect("reference file is readable UTF-8")
}

#[test]
fn canonicalizer_matches_the_reference_byte_for_byte() {
    for case in CASES {
        let value: serde_json::Value =
            serde_json::from_str(&read("input", case)).expect("reference input is JSON");
        assert_eq!(
            serde_json_canonicalizer::to_string(&value).expect("reference input canonicalizes"),
            read("output", case).trim_end(),
            "canonical text for `{case}` differs from the RFC 8785 reference"
        );
    }
}

#[test]
fn strict_canonicalization_matches_the_reference_inside_the_profile() {
    for case in CASES {
        let actual =
            jans_trace_core::json::canonicalize_json(&read("input", case), &Limits::default());
        if case == OUT_OF_PROFILE {
            let err = actual.expect_err("1E30 is outside +/-(2^53-1)");
            assert_eq!(
                (err.code(), err.to_string().as_str()),
                (
                    ErrorCode::NonCanonicalizableJson,
                    "JSON value cannot be canonicalized without changing it: a number is \
                     outside +/-(2^53-1), the range every JSON reader keeps exact"
                ),
                "`{case}` is refused for its out-of-range number"
            );
        } else {
            assert_eq!(
                actual.expect("reference input canonicalizes"),
                read("output", case).trim_end(),
                "canonical text for `{case}` differs from the RFC 8785 reference"
            );
        }
    }
}
