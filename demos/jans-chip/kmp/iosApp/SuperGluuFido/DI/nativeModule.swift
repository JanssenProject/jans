import Foundation
import shared

var nativeModule: Koin_coreModule = MakeNativeModuleKt.makeNativeModule(
    analytic: { scope in
        return AnalyticImpl(logger: scope.get())
    },
    authentication: { _ in
        return AuthenticationAdaptor()
    },
    dPoPProofProvider: { _ in
        return DPoPProofFactoryAdaptor()
    }
)
