package com.example.fido2.modules

import org.koin.dsl.module
import com.example.fido2.viewmodel.MainViewModel

val viewModelModule = module {
    factory {
        MainViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
}