#![no_main]

use libfuzzer_sys::fuzz_target;
use sigstore_verifier::fuzz_api::sct_parse_list;

fuzz_target!(|data: &[u8]| {
    sct_parse_list(data);
});
