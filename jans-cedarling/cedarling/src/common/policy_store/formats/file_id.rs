// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Issuer id derived from a file name, shared by every format version.
//!
//! A stateless string helper rather than part of any version's shape, so
//! versions share it instead of each freezing a copy.

/// Strips a `.json` suffix in any case, matching the loader's extension check.
///
/// Used for issuer files that carry no explicit `id` field.
pub(super) fn id_from_filename(filename: &str) -> &str {
    filename
        .rfind('.')
        .filter(|&dot| filename[dot..].eq_ignore_ascii_case(".json"))
        .map_or(filename, |dot| &filename[..dot])
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn strips_a_json_suffix_in_any_case() {
        for name in ["issuer.json", "issuer.JSON", "issuer.JsOn"] {
            assert_eq!(
                id_from_filename(name),
                "issuer",
                "the loader accepts '{name}', so its id should drop the extension"
            );
        }
    }

    #[test]
    fn keeps_names_that_are_not_json() {
        assert_eq!(
            id_from_filename("issuer"),
            "issuer",
            "a name without an extension is already the id"
        );
        assert_eq!(
            id_from_filename("issuer.yaml"),
            "issuer.yaml",
            "only a .json suffix should be stripped"
        );
        assert_eq!(
            id_from_filename("my.issuer.json"),
            "my.issuer",
            "only the final suffix should be stripped"
        );
    }
}
