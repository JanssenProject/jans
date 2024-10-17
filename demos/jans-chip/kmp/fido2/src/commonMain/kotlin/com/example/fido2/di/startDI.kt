package com.example.fido2.di

import com.example.fido2.modules.dispatcherModule
import com.example.fido2.modules.repositoryModule
import com.example.fido2.modules.useCasesModule
import com.example.fido2.modules.viewModelModule
import com.example.fido2.utils.platformModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** Start Koin DI with given native module and app declaration. for iOS - do NOT REMOVE*/
fun startDI(nativeModule: Module, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()

        modules(
            nativeModule,
            sharedModule,
            viewModelModule,
            dispatcherModule,
            useCasesModule,
            repositoryModule,
            platformModule
        )
    }
}