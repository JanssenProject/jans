package com.example.fido2.utils

import shared.database.getAppDatabase
import com.example.fido2.database.AppDatabase
import org.koin.dsl.module

actual val platformModule = module {
    single<AppDatabase> {
        getAppDatabase()
    }
}