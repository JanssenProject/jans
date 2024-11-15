package com.example.fido2.modules

import com.example.fido2.database.local.*
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.example.fido2.repository.dcr.*
import com.example.fido2.repository.fidoAssertion.*
import com.example.fido2.repository.fidoAttestation.*
import com.example.fido2.repository.login.*
import com.example.fido2.repository.logout.*
import com.example.fido2.repository.main.*
import com.example.fido2.repository.settings.*
import com.example.fido2.repository.token.*
import com.example.fido2.repository.userinfo.*
import com.example.fido2.retrofit.ApiClient

val repositoryModule = module {
    single<MainRepository> { MainRepositoryImpl(get(), get()) }
    single<LoginRepository> { LoginRepositoryImpl(get(), get()) }
    single<TokenRepository> { TokenRepositoryImpl(get(), get()) }
    single<LogoutRepository> { LogoutRepositoryImpl(get(), get()) }
    single<UserInfoRepository> { UserInfoRepositoryImpl(get(), get()) }
    single<FidoAssertionRepository> { FidoAssertionRepositoryImpl(get(), get()) }
    single<FidoAttestationRepository> { FidoAttestationRepositoryImpl(get(), get()) }
    single<DCRRepository> { DCRRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<AllDataRepository> { AllDataRepositoryImpl(get()) }
    single<ApiClient> { ApiClient() }
    single<LocalDataSource> { LocalDataSourceImpl(get(), get(named(Dispatcher.IO))) }
}