//
//  KeychainManager.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 12/5/22.
//

import Foundation
import SwiftKeychainWrapper

final class KeychainManager {

    static let shared = KeychainManager()

    private init() { }

    private let keychainWrapperInstance = KeychainWrapper(
        serviceName: "NativeSSO",
        accessGroup: "FLRS8NH8Y9.com.gluu.NativeSSO"
    )

    var clientID: String? {
        get {
            UserDefaults.standard.string(forKey: KeychainWrapper.Key.appClientKey.rawValue)
//            keychainWrapperInstance.string(forKey: .appClientKey)
        }
        set {
            if let newValue = newValue {
                UserDefaults.standard.setValue(newValue, forKey: KeychainWrapper.Key.appClientKey.rawValue)
//                keychainWrapperInstance.set(newValue, forKey: KeychainWrapper.Key.appClientKey.rawValue)
            }
        }
    }

    var clientSecret: String? {
        get {
            UserDefaults.standard.string(forKey: KeychainWrapper.Key.appClientSecretKey.rawValue)
        }
        set {
            if let newValue = newValue {
                UserDefaults.standard.setValue(newValue, forKey: KeychainWrapper.Key.appClientSecretKey.rawValue)
            }
        }
    }

    var deviceToken: String? {
        get {
            keychainWrapperInstance.string(forKey: .appDeviceTokenKey)
        }
        set {
            if let newValue = newValue {
                keychainWrapperInstance.set(newValue, forKey: KeychainWrapper.Key.appDeviceTokenKey.rawValue)
            }
        }
    }

    var idToken: String? {
        get {
            keychainWrapperInstance.string(forKey: .appIdTokenKey)
        }
        set {
            if let newValue = newValue {
                keychainWrapperInstance.set(newValue, forKey: KeychainWrapper.Key.appIdTokenKey.rawValue)
            }
        }
    }

    var token: Token? {
        get {
            if let tokenData = UserDefaults.standard.object(forKey: KeychainWrapper.Key.tokenKey.rawValue) as? Data {
                do {
                    return try NSKeyedUnarchiver.unarchiveTopLevelObjectWithData(tokenData) as? Token
                } catch {
                    print("Error encoding object - \(error)")
                }
            }

            return nil
        }
        set {
            if let newValue = newValue {
                do {
                    let encodedData = try NSKeyedArchiver.archivedData(withRootObject: newValue, requiringSecureCoding: false)
                    UserDefaults.standard.setValue(encodedData, forKey: KeychainWrapper.Key.tokenKey.rawValue)
                } catch {
                    print("Error decoding object - \(error)")
                }
            }
        }
    }
}

extension KeychainWrapper.Key {
    static let appClientSecretKey: KeychainWrapper.Key = "app_client_secret_key"
    static let appClientKey: KeychainWrapper.Key = "app_client_key"
    static let appIdTokenKey: KeychainWrapper.Key = "app_id_token_key"
    static let appDeviceTokenKey: KeychainWrapper.Key = "app_device_token_key"
    static let tokenKey: KeychainWrapper.Key = "token_key"
}
