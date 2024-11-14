//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

extension AuthenticatorType {
    var simpleString: String {
        switch self {
        case .biometric:
            return "Biometric"
        case .deviceCredential:
            return "Device Credential"
        }
    }
}
