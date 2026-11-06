// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Printing text the CLI did not write, such as policy store content and
//! errors built from it.

use std::fmt::{self, Display, Formatter, Write};

/// Text from a policy store or a test file, shown so that it cannot move the
/// cursor, recolor the terminal, break a report line or reorder what is shown.
/// Such characters print as `\u{..}` escapes; everything else prints as is.
pub struct Untrusted<'a>(pub &'a str);

impl Display for Untrusted<'_> {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        for c in self.0.chars() {
            if is_unsafe_to_show(c) {
                write!(f, "{}", c.escape_unicode())?;
            } else {
                f.write_char(c)?;
            }
        }
        Ok(())
    }
}

/// Control characters, plus the invisible format characters that change line
/// breaks or text direction without being control characters.
fn is_unsafe_to_show(c: char) -> bool {
    c.is_control()
        || matches!(
            c,
            '\u{061C}'
                | '\u{200B}'..='\u{200F}'
                | '\u{2028}'..='\u{202E}'
                | '\u{2066}'..='\u{2069}'
                | '\u{FEFF}'
        )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn escapes_terminal_sequences_and_line_breaks() {
        assert_eq!(
            Untrusted("a\u{1b}[31mb\nc\td\u{7f}e\u{9b}").to_string(),
            r"a\u{1b}[31mb\u{a}c\u{9}d\u{7f}e\u{9b}",
            "C0, DEL and C1 control characters must print as escapes"
        );
    }

    #[test]
    fn escapes_direction_and_invisible_format_characters() {
        assert_eq!(
            Untrusted("\u{202e}x\u{2066}y\u{2028}z\u{200b}w\u{061c}v\u{feff}").to_string(),
            r"\u{202e}x\u{2066}y\u{2028}z\u{200b}w\u{61c}v\u{feff}",
            "bidi overrides, isolates, line separators and zero-width characters must print as \
             escapes"
        );
    }

    #[test]
    fn keeps_printable_text() {
        let text = "Acme::Access_token.x_vendor \"ідентифікатор\" 😀 `ok`";
        assert_eq!(
            Untrusted(text).to_string(),
            text,
            "printable text, including non-ASCII letters and emoji, must print unchanged"
        );
    }
}
