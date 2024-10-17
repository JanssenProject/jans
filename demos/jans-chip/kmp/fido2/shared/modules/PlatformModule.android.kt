package com.example.fido2.shared.modules

import com.example.fido2.database.AppDatabase
import org.koin.dsl.module
import com.example.fido2.shared.database.getAppDatabase

actual val platformModule = module {
    single<AppDatabase> {
        getAppDatabase(get())
    }
}