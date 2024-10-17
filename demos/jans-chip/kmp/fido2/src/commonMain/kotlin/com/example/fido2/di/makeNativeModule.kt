package com.example.fido2.di

import com.example.fido2.authAdaptor.AuthenticationProvider
import com.example.fido2.authAdaptor.DPoPProofFactoryProvider
import com.example.fido2.services.Analytic
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

typealias NativeInjectionFactory<T> = Scope.() -> T

fun makeNativeModule(
    analytic: NativeInjectionFactory<Analytic>,
    authentication: NativeInjectionFactory<AuthenticationProvider>,
    dPoPProofProvider: NativeInjectionFactory<DPoPProofFactoryProvider>
): Module {
    return module {
        single { analytic() }
        single { authentication() }
        single { dPoPProofProvider() }
    }
}
