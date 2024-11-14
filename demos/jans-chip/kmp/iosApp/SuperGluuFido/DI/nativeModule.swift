//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

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
