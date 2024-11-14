//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import LocalAuthentication

public protocol LocalAuthenticationProtocol {
    var localizedReason: String { get set }

    func execute() async -> Result<Bool, WebAuthnError>
}

final class LocalAuthentication: LocalAuthenticationProtocol {
    var context = LAContext()

    let policy: LAPolicy
    var localizedReason = "Register your account"

    init(_ policy: LAPolicy) {
        self.policy = policy
    }

    func execute() async -> Result<Bool, WebAuthnError> {
        var error: NSError?
        guard context.canEvaluatePolicy(policy, error: &error) else {
            let msg = error?.localizedDescription ?? "Can't evaluate policy"
            return .failure(.coreError(.constraintError, cause: msg))
        }
        do {
            let result = try await context.evaluatePolicy(policy, localizedReason: localizedReason)
            return .success(result)
        } catch {
            return .failure(.laError(error))
        }
    }
}

extension LAContext {
    enum BiometricType: String {
        case none
        case touchID
        case faceID
        case opticID
    }

    var biometricType: BiometricType {
        var error: NSError?

        guard self.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return .none
        }

        if #available(iOS 11.0, *) {
            switch self.biometryType {
            case .none:
                return .none
            case .touchID:
                return .touchID
            case .faceID:
                return .faceID
            case .opticID:
                return .opticID
            @unknown default:
                #warning("Handle new Biometric type")
            }
        }
        
        return  self.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil) ? .touchID : .none
    }
}
