//
//  AppIntegrityResponse.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.10.2023.
//

import Foundation

struct AppIntegrityResponse: Codable {
    
    var requestDetails: RequestDetails
    var appIntegrity: AppIntegrity
    var deviceIntegrity: DeviceIntegrity
    var accountDetails: AccountDetails
    var error: String
    
    var isSuccessful: Bool = false
}

struct RequestDetails: Codable {
    var requestPackageName: String
    var timestampMillis: String
    var nonce: String
}

struct AppIntegrity: Codable {
    var requestPackageName: String
}

struct DeviceIntegrity: Codable {
    var deviceRecognitionVerdict: [String]
}

struct AccountDetails: Codable {
    var appLicensingVerdict: String
}


