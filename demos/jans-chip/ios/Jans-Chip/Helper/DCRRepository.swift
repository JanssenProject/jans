//
//  DCRRepository.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.10.2023.
//

import Foundation
import Combine

final class DCRRepository {
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    private var cancellableSet : Set<AnyCancellable> = []
    
    func doDCR(scopeText: String, callback: ((Bool, Error?) -> ())?) {
        guard let configuration: OPConfigurationObject = RealmManager.shared.getObject() else {
            print("OpenID configuration not found in database.")
            return
        }
        
        let issuer = configuration.issuer
        let registrationUrl = configuration.registrationEndpoint
        
        let scopes = scopeText.split(separator: ",").joined(separator: " ")
        
        let dcRequest = DCRequest(
            issuer: issuer,
            redirectUris: [issuer],
            scope: scopes,
            responseTypes: ["code"],
            postLogoutRedirectUris: [issuer],
            grantTypes: ["authorization_code", "client_credentials"],
            applicationType: "web",
            clientName: AppConfig.APP_NAME + UUID().uuidString,
            tokenEndpointAuthMethod: "client_secret_basic"
        )
        
//        var claims: [String: Any] = [
//            "appName": AppConfig.APP_NAME,
//            "seq": UUID().uuidString,
//            "app_id": Bundle.main.bundleIdentifier ?? "Unknown"
//        ]
        
        // TODO: Get package check sum
//        claims["app_checksum"] = "app_checksum"
        // TODO: Get public JWK
//        dcRequest.jwks = ""
        
        serviceClient.doDCR(dcRequest: dcRequest, url: registrationUrl)
            .sink { result in
                switch result {
                case .success(let dcResponse):
                    print("Inside doDCR :: DCResponse :: \(dcResponse)")
                    let oidcClient = OIDCClient()
                    oidcClient.clientName = dcResponse.clientName
                    oidcClient.clientId = dcResponse.clientId
                    oidcClient.clientSecret = dcResponse.clientSecret
                    oidcClient.scope = scopes
                    
                    RealmManager.shared.save(object: oidcClient)
                    
                    callback?(true, nil)
                case .failure(let error):
                    print("Error in DCR.\n Error:  \(error)")
                    callback?(false, error)
                }
            }
            .store(in: &cancellableSet)
    }
}
