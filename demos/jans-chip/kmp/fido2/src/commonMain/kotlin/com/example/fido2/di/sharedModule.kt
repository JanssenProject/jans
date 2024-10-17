package com.example.fido2.di

import com.example.fido2.services.Logger
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * The Koin module that contains all DI services, ViewModules, etc.,
 * that provided in Shared Module.
 */
val sharedModule: Module = module {
    singleOf(::Logger)
}
