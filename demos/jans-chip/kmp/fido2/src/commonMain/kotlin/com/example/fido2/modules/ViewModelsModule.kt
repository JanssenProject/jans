package com.example.fido2.modules

import com.example.fido2.ui.screens.settings.SettingsScreenViewModel
import org.koin.dsl.module
import com.example.fido2.viewmodel.MainViewModel

val viewModelModule = module {
    factory {
        MainViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    factory { SettingsScreenViewModel(get()) }
}