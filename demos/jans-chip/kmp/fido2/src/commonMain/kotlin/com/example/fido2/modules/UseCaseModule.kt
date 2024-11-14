package com.example.fido2.modules

import com.example.fido2.usecase.DCRClientUseCase
import com.example.fido2.usecase.FidoAssertionUseCase
import com.example.fido2.usecase.FidoAttestationUseCase
import com.example.fido2.usecase.GetFidoConfigurationUseCase
import com.example.fido2.usecase.GetOPConfigurationUseCase
import com.example.fido2.usecase.GetTokenUseCase
import com.example.fido2.usecase.GetUserInfoUseCase
import com.example.fido2.usecase.LoginUseCase
import com.example.fido2.usecase.LogoutUseCase
import com.example.fido2.usecase.SettingsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val useCasesModule: Module = module {
    factory { GetOPConfigurationUseCase(get(), get()) }
    factory { GetFidoConfigurationUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { GetTokenUseCase(get(), get()) }
    factory { GetUserInfoUseCase(get()) }
    factory { FidoAssertionUseCase(get()) }
    factory { FidoAttestationUseCase(get()) }
    factory { DCRClientUseCase(get(), get()) }
    factory { SettingsUseCase(get()) }
}