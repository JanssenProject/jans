//
//  DCRRepository.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.10.2023.
//

import Foundation

final class DCRRepository {
    
    func doDCR(scopeText: String) {
        guard let configuration = RealmManager.shared.getConfiguration() else {
            print("OpenID configuration not found in database.")
            return
        }
        
        let issuer = configuration.issuer
        let registrationUrl = configuration.registrationEndpoint
        
        let dcRequest = DCRequest(
            issuer: issuer,
            redirectUris: [issuer],
            scope: scopeText,
            responseTypes: ["code"],
            postLogoutRedirectUris: [issuer],
            grantTypes: ["authorization_code", "client_credentials"],
            applicationType: "web",
            clientName: AppConfig.APP_NAME + UUID().uuidString,
            tokenEndpointAuthMethod: "client_secret_basic"
        )
        
        var claims: [String: Any] = [
            "appName": AppConfig.APP_NAME,
            "seq": UUID().uuidString,
            "app_id": Bundle.main.bundleIdentifier ?? "Unknown"
        ]
        
        // TODO: Get package check sum
//        claims["app_checksum"] = "app_checksum"
        // TODO: Get public JWK
//        dcRequest.jwks = ""
        
        
    }
}
