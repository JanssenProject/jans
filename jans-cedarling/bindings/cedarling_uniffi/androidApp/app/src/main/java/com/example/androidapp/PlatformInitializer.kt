// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package com.example.androidapp

import android.content.Context

/**
 * Initializes platform-specific services required by the Cedarling native library
 * before any Cedarling instances are created.
 */
object PlatformInitializer {
    init {
        System.loadLibrary("cedarling_uniffi")
    }

    /**
     * Initializes the TLS platform verifier for Android.
     *
     * Must be called before creating any [uniffi.cedarling_uniffi.Cedarling] instance
     * when JWT signature or status validation is enabled.
     *
     * @param context The Android application context (use `applicationContext`).
     */
    @JvmStatic
    external fun initPlatformVerifier(context: Context)
}
