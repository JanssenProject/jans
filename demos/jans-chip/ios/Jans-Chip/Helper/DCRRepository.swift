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
        guard let configuration: OPConfiguration = RealmManager.shared.getObject() else {
            print("OpenID configuration not found in database.")
            return
        }
        
        let issuer = configuration.issuer
        let registrationUrl = configuration.registrationEndpoint
        
        let scopes = scopeText.split(separator: ",").joined(separator: " ")
        
        let dcRequest = DCRequest(
            clientName: AppConfig.APP_NAME + UUID().uuidString,
            evidence: nil,
            jwks: nil,
            scope: scopeText,
            responseTypes: ["code"],
            grantTypes: ["authorization_code", "client_credentials"],
            ssa: "",
            applicationType: "native",
            redirectUris: [issuer])
        
        var claims = JansIntegrityClaims(
            appName: AppConfig.APP_NAME,
            seq: UUID().uuidString,
            app_id: Bundle.main.bundleIdentifier ?? "Unknown"
        )
        
        if let appIntegrityEntityDeviceToken = UserDefaults.standard.string(forKey: "AppIntegrityEntityDeviceToken") {
            claims.app_integrity_result = appIntegrityEntityDeviceToken
        }
        
        // TODO: Get package check sum
        let mainUrl = Bundle.main.bundleURL
        do {
            let ipaFileData = try mainUrl.bookmarkData()
            
            let checksum = ipaFileData.checksum()
            claims.app_checksum = checksum.description
        } catch(let error){
            print("Error getting file data: \(error)")
        }
        
//        let evidenceJwt = DPoPProofFactory.shared.issueJWTToken(claims: claims)
//        dcRequest.evidence = evidenceJwt
        
//        dcRequest.jwks = ""
        
        serviceClient.doDCR(dcRequest: dcRequest, url: registrationUrl)
            .sink { result in
                if let error = result.error {
                    print("Error in DCR.\n Error:  \(error)")
                    callback?(false, error)
                } else if let dcResponse = result.value {
                    print("Inside doDCR :: DCResponse :: \(dcResponse)")
                    let oidcClient = OIDCClient()
                    oidcClient.clientName = dcResponse.clientName ?? ""
                    oidcClient.clientId = dcResponse.clientId ?? ""
                    oidcClient.clientSecret = dcResponse.clientSecret ?? ""
                    oidcClient.scope = scopes
                    
                    RealmManager.shared.save(object: oidcClient)
                    
                    callback?(true, nil)
                }
            }
            .store(in: &cancellableSet)
    }
    
    func makeDCRRequestUsingSSA(configuration: OPConfiguration, ssa: String, scopeText: String) -> DCRequest? {
        let issuer = configuration.issuer
        
        let dcRequest = DCRequest(
            clientName: AppConfig.APP_NAME + UUID().uuidString,
            evidence: nil,
            jwks: nil,
            scope: scopeText,
            responseTypes: ["code"],
            grantTypes: ["authorization_code", "client_credentials"],
            ssa: ssa,
            applicationType: "native",
            redirectUris: [issuer]
        )
        
        var claims = JansIntegrityClaims(
            appName: AppConfig.APP_NAME,
            seq: UUID().uuidString,
            app_id: Bundle.main.bundleIdentifier ?? "Unknown"
        )
        
        if let appIntegrityEntityDeviceToken = UserDefaults.standard.string(forKey: "AppIntegrityEntityDeviceToken") {
            claims.app_integrity_result = appIntegrityEntityDeviceToken
        }
        
        // TODO: Get package check sum
        let mainUrl = Bundle.main.bundleURL
        do {
            let ipaFileData = try mainUrl.bookmarkData()
            
            let checksum = ipaFileData.checksum()
            claims.app_checksum = checksum.description
        } catch(let error){
            print("Error getting file data: \(error)")
        }
        
        return dcRequest
    }
    
    func doDCRUsingSSA(ssaJwt: String?, scopeText: String?) async throws -> OIDCClient {
        var oidcClient = OIDCClient()
        
        guard let opConfiguration: OPConfiguration = RealmManager.shared.getObject() else {
            print("OpenID configuration not found in database.")
            oidcClient.isSuccess = false
            oidcClient.errorMessage = "OpenID configuration not found in database.."
            return oidcClient
        }
        
        let issuer: String = opConfiguration.issuer
        let registrationUrl: String = opConfiguration.registrationEndpoint
        
        var dcrRequest = SSARegRequest(
            clientName: AppConfig.APP_NAME + UUID().uuidString,
            evidence: nil,
            jwks: nil,
            scope: scopeText,
            responseTypes: ["code"],
            grantTypes: ["authorization_code", "client_credentials"],
            ssa: ssaJwt,
            applicationType: "native",
            redirectUris: [issuer])
        
        var claims: [String: String] = [
            "appName": AppConfig.APP_NAME,
            "seq": UUID().uuidString,
            "app_id": Bundle.main.bundleIdentifier ?? "noneapp_checksum"
        ]
        
        let mainUrl = Bundle.main.bundleURL
        do {
            let ipaFileData = try mainUrl.bookmarkData()
            
            let checksum = ipaFileData.checksum()
            claims["app_checksum"] = checksum.description
        } catch(let error) {
            let errorMessage = "Error in generating app checksum " + error.localizedDescription
            print(errorMessage)
            oidcClient.isSuccess = false
            oidcClient.errorMessage = errorMessage
            return oidcClient
        }
        
        let evidenceJwt = DPoPProofFactory.shared.issueJWTToken(claims: claims)
        dcrRequest.evidence = evidenceJwt
        
        dcrRequest.jwks = ""
        
        oidcClient = await withCheckedContinuation { continuation in
            serviceClient.doDCR(dcRequest: dcrRequest, url: registrationUrl)
                .sink { result in
                    if let error = result.error {
                        print("Error in DCR.\n Error:  \(error)")
                        oidcClient.isSuccess = false
                        oidcClient.errorMessage = "Error in DCR.\n Error:  \(error)"
                        continuation.resume(returning: oidcClient)
                    } else if let dcrResponse = result.value {
                        print("Inside doDCR :: DCResponse :: \(dcrResponse)")
                        
                        oidcClient.sno = AppConfig.DEFAULT_S_NO
                        oidcClient.clientName = dcrResponse.clientName ?? ""
                        oidcClient.clientId = dcrResponse.clientId ?? ""
                        oidcClient.clientSecret = dcrResponse.clientSecret ?? ""
                        oidcClient.scope = scopeText ?? ""
                        oidcClient.clientName = dcrResponse.clientName ?? ""
                        oidcClient.isSuccess = true
                        
                        RealmManager.shared.deleteAllOIDCClient()
                        RealmManager.shared.save(object: oidcClient)
                        
                        continuation.resume(returning: oidcClient)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return oidcClient
    }
}
