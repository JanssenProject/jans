// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use jni::objects::{JClass, JObject};
use jni::JavaVM;
use std::sync::OnceLock;

static JAVA_VM: OnceLock<JavaVM> = OnceLock::new();

/// Stores the JavaVM reference when the native library is loaded.
/// Called automatically by the JVM when the library is loaded via `System.loadLibrary`.
#[no_mangle]
pub extern "system" fn JNI_OnLoad(
    vm: JavaVM,
    _reserved: *mut std::ffi::c_void,
) -> jni::sys::jint {
    let _ = JAVA_VM.set(vm);
    jni::sys::JNI_VERSION_1_6
}

/// Initializes `rustls-platform-verifier` on Android.
/// Called from Kotlin via JNI with the Application Context.
///
/// This must be called before any HTTPS requests are made (i.e., before creating
/// a Cedarling instance when JWT verification is enabled).
#[no_mangle]
pub extern "system" fn Java_com_example_androidapp_PlatformInitializer_initPlatformVerifier<
    'local,
>(
    mut env: jni::JNIEnv<'local>,
    _class: JClass<'local>,
    context: JObject<'local>,
) {
    rustls_platform_verifier::android::init_with_env(&mut env, context)
        .expect("Failed to initialize rustls-platform-verifier on Android");
}
