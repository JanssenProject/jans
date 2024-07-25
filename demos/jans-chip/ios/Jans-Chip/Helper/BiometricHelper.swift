//
//  BiometricHelper.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.07.2024.
//

import UIKit
import LocalAuthentication

final class BiometricHelper {
    
    static let shared = BiometricHelper()
    
    // UserDefaults key for the switch state
    private let biometricSwitchKey = "biometricSwitchState"
    
    private init() {}
    
    // Function to set the state of the biometric switch
    func setBiometricSwitchState(isOn: Bool) {
        UserDefaults.standard.set(isOn, forKey: biometricSwitchKey)
    }
    
    // Function to get the state of the biometric switch
    func isBiometricSwitchOn() -> Bool {
        UserDefaults.standard.bool(forKey: biometricSwitchKey)
    }
    
    func canUseBiometricAuthentication() -> Bool {
        let context = LAContext()
        var error: NSError?
        return context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }
    
    func getBiometricType() -> LABiometryType {
        let context = LAContext()
        return context.biometryType
    }
    
    func authenticateWithBiometrics(completion: @escaping (Bool, Error?) -> Void) {
        if Platform.isSimulator {
            completion(true, nil)
            return
        }
        
        let context = LAContext()
        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Authenticate using Face ID or Touch ID") { success, error in
            DispatchQueue.main.async {
                completion(success, error)
            }
        }
    }
    func showBiometricsSettingsAlert(_ controller: UIViewController) {
        let alertController = UIAlertController(
            title: "Enable Face ID/Touch ID",
            message: "To use biometric authentication, you need to enable Face ID/Touch ID for this app in your device settings.",
            preferredStyle: .alert
        )
        let settingsAction = UIAlertAction(title: "Go to Settings", style: .default) { _ in
            if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(settingsURL, options: [:], completionHandler: nil)
            }
        }
        alertController.addAction(settingsAction)
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        controller.present(alertController, animated: true, completion: nil)
    }
}
