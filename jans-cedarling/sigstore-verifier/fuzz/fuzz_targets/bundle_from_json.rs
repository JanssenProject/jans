#![no_main]

use libfuzzer_sys::fuzz_target;
use sigstore_verifier::fuzz_api::bundle_from_json;

fuzz_target!(|data: &[u8]| {
    bundle_from_json(data);
});
