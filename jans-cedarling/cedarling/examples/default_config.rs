// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// # export CEDARLING_APPLICATION_NAME=ABC123
// # export CEDARLING_POLICY_STORE_URI=https://test.com/asdfasdf

use cedarling::config::Config;

fn main() {
    let config = Config::load_with_defaults().unwrap();
    println!("app_name: {}", config.application_name);
    println!("source: {}", config.policy_store.source);
}
