package com.example.fido2.utils

import com.example.fido2.database.AppDatabase
import com.shared.database.getAppDatabase
import org.koin.dsl.module

actual val platformModule = module {
    single<AppDatabase> {
        getAppDatabase(get())
    }
}