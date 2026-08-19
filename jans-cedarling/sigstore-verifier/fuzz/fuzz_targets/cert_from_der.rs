#![no_main]

use libfuzzer_sys::fuzz_target;
use sigstore_verifier::fuzz_api::cert_from_der;

fuzz_target!(|data: &[u8]| {
    cert_from_der(data);
});
