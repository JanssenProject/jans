use std::env;

fn main() {
    env::set_var("RUST_MIN_STACK", "67108864"); // Example: 64 MB stack
    uniffi::uniffi_bindgen_main()
}