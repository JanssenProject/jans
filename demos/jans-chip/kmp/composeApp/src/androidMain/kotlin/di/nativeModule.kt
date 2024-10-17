package di

import com.example.fido2.di.makeNativeModule
import services.AnalyticImpl
import services.AuthAdaptor
import services.DPoPProofFactoryAdaptor

val nativeModule = makeNativeModule(
    analytic = { AnalyticImpl( get() ) },
    authentication = { AuthAdaptor(get()) },
    dPoPProofProvider = { DPoPProofFactoryAdaptor(get()) }
)