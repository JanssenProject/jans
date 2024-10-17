package com.example.fido2.modules

import com.example.fido2.database.local.LocalDataSource
import com.example.fido2.database.local.LocalDataSourceImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.example.fido2.repository.dcr.DCRRepository
import com.example.fido2.repository.dcr.DCRRepositoryImpl
import com.example.fido2.repository.fidoAssertion.FidoAssertionRepository
import com.example.fido2.repository.fidoAssertion.FidoAssertionRepositoryImpl
import com.example.fido2.repository.fidoAttestation.FidoAttestationRepository
import com.example.fido2.repository.fidoAttestation.FidoAttestationRepositoryImpl
import com.example.fido2.repository.login.LoginRepository
import com.example.fido2.repository.login.LoginRepositoryImpl
import com.example.fido2.repository.logout.LogoutRepository
import com.example.fido2.repository.logout.LogoutRepositoryImpl
import com.example.fido2.repository.main.MainRepository
import com.example.fido2.repository.main.MainRepositoryImpl
import com.example.fido2.repository.token.TokenRepository
import com.example.fido2.repository.token.TokenRepositoryImpl
import com.example.fido2.repository.userinfo.UserInfoRepository
import com.example.fido2.repository.userinfo.UserInfoRepositoryImpl
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
    single<ApiClient> { ApiClient() }
    single<LocalDataSource> { LocalDataSourceImpl(get(), get(named(Dispatcher.IO))) }
}