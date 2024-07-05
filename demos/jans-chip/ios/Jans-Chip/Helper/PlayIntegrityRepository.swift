//
//  PlayIntegrityRepository.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.10.2023.
//

import Foundation
import DeviceCheck
import CryptoKit

final class PlayIntegrityRepository {
    
    func checkAppIntegrity() {
        let curDevice = DCDevice.current
        if curDevice.isSupported {
            curDevice.generateToken(completionHandler: { (data, error) in
                if let tokenData = data {
                    print("Received token \(tokenData.base64EncodedString())")
                    UserDefaults.standard.setValue(tokenData.base64EncodedString(), forKey: "AppIntegrityEntityDeviceToken")
                } else {
                    print("Hit error: \(String(describing: error?.localizedDescription))")
                }
            })
        }
        
//        let service = DCAppAttestService.shared
//        
//        guard service.isSupported else {
//            return
//        }
//        
//        service.generateKey { (keyIdentifier, error) in
//            guard error == nil else {
//                return
//            }
//            
//            guard let keyIdentifier = keyIdentifier else {
//                return
//            }
//            
//            let requestJSON = "{'requestedPremiumLevel': 300}".data(using: .utf8)!
//            let hash = Data(SHA256.hash(data: requestJSON))
//            
//            service.attestKey(keyIdentifier, clientDataHash: hash) { attestation, error in
//                guard error == nil else {
//                        return
//                }
//
//
//                // Send the attestation object to your server for verification.
//            }
//            
//            service.generateAssertion(keyIdentifier, clientDataHash: hash) { assertion, error in
//                guard let error = error as? DCError else {
//                    let assertionString = assertion?.base64EncodedString()
//                    // Send the signed assertion to your server.
//                    // The server will validate it, grab your request and process it.
//                    return
//                }
//                
//                print(error)
//            }
//        }
    }
}
