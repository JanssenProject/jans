// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Android-specific initialization.
//!
//! On Android, `rustls-platform-verifier` (used by Cedarling's HTTP client for
//! TLS certificate verification) must be initialized with an Android `Context`
//! before any network call is made, otherwise it panics with
//! "Expect rustls-platform-verifier to be initialized".
//!
//! UniFFI cannot pass JNI objects, so this is exposed as a plain JNI export.
//! Kotlin side (must match the JNI name below):
//!
//! ```kotlin
//! package org.jans.cedarling
//!
//! object CedarlingAndroid {
//!     init { System.loadLibrary("cedarling_uniffi") }
//!     @JvmStatic external fun initTls(context: android.content.Context)
//! }
//! ```
//!
//! Call `CedarlingAndroid.initTls(applicationContext)` once (e.g. in
//! `Application.onCreate`) before constructing a `Cedarling` instance.
//!
//! Note: the app must also bundle the rustls-platform-verifier Kotlin
//! component (`org.rustls.platformverifier`), see
//! <https://github.com/rustls/rustls-platform-verifier#android>.

use jni::objects::{JClass, JObject};
use jni::EnvUnowned;

/// JNI entry point: `org.jans.cedarling.CedarlingAndroid.initTls(Context)`.
///
/// Initializes rustls-platform-verifier with the given Android Context.
/// Safe to call multiple times; subsequent calls are no-ops.
/// Throws a Java `RuntimeException` on failure.
#[no_mangle]
pub extern "system" fn Java_org_jans_cedarling_CedarlingAndroid_initTls<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _class: JClass<'local>,
    context: JObject<'local>,
) {
    unowned_env
        .with_env(|env| rustls_platform_verifier::android::init_with_env(env, context))
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
