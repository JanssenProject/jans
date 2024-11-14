//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

public enum AuthenticatorType: CaseIterable {
    private static let biometricAAGUID = UUID(uuidString: "4e3db665-c12d-5d2d-6a09-a15b78972bc9")!
    private static let deviceCredentialAAGUID = UUID.init(uuidString: "5c7b7e9a-2b85-464e-9ea3-529582bb7e34")!

    case biometric
    case deviceCredential

    init?(aaguid: UUID) {
        switch aaguid {
        case AuthenticatorType.biometricAAGUID:
            self = .biometric
        case AuthenticatorType.deviceCredentialAAGUID:
            self = .deviceCredential
        default:
            return nil
        }
    }

    var aaguid: UUID {
        switch self {
        case .biometric:
            return AuthenticatorType.biometricAAGUID
        case .deviceCredential:
            return AuthenticatorType.deviceCredentialAAGUID
        }
    }

    var uuidString: String {
        switch self {
        case .biometric:
            return self.aaguid.uuidString
        case .deviceCredential:
            return self.aaguid.uuidString
        }
    }
}
