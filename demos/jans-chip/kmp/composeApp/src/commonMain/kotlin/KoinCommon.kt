
import com.example.fido2.di.sharedModule
import com.example.fido2.modules.*
import com.example.fido2.utils.platformModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(nativeModule: Module, appDeclaration: KoinAppDeclaration = {}) =
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

//using in iOS
//fun initKoin() = initKoin {}