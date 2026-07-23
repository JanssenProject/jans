package org.jans.cedarling

import android.content.Context

/**
 * Android-specific initialization for Cedarling.
 *
 * Cedarling's Rust core verifies TLS certificates (JWKS fetches, token status
 * lists, remote policy stores) through rustls-platform-verifier, which on
 * Android must be handed an application [Context] via JNI before any network
 * call is made. Without this it panics with
 * "Expect rustls-platform-verifier to be initialized".
 *
 * Call [initTls] once, before constructing any Cedarling instance —
 * e.g. from Application.onCreate() or Activity.onCreate().
 *
 * The JNI entry point lives in cedarling_uniffi's src/android.rs and its name
 * is derived from this package/class/method — keep them in sync.
 */
object CedarlingAndroid {
    @Volatile
    private var initialized = false

    init {
        System.loadLibrary("cedarling_uniffi")
    }

    @JvmStatic
    external fun initTls(context: Context)

    /** Idempotent convenience wrapper around [initTls]. */
    @JvmStatic
    fun ensureInitialized(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    initTls(context.applicationContext)
                    initialized = true
                }
            }
        }
    }
}
