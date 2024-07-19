//
//  MainViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import RealmSwift
import Combine

protocol MainViewInteractor: AnyObject {
    
    func onAppear()
    func fetchFidoConfiguration(issuer: String) async throws -> FidoConfiguration?
    func processLogin(username: String, password: String, authMethod: String,
                      assertionResultRequest: String?) async throws -> LoginResponse
    func getToken(authorizationCode: String) async throws -> TokenResponse
    func getUserInfo(accessToken: String) async throws -> UserInfo
}

final class MainViewInteractorImpl: MainViewInteractor {
    
    private let presenter: MainViewPresenterImpl
    
    private let integrityRepository = PlayIntegrityRepository()
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    private var cancellableSet : Set<AnyCancellable> = []
    
    private var configuration: OPConfiguration?
    private var fidoConfiguration: FidoConfiguration?
    private var appIntegrity: AppIntegrityEntity?
    
    @ObservedResults(OIDCClient.self) var oidcClient
    
    init(presenter: MainViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onAppear() {
        if let claims: JWTClaimsSet = DPoPProofFactory.shared.getClaimsFromSSA(), let issuer = claims.ISSUER {
            Task {
                do {
                    // 1. get openid configuration
                    configuration = try await fetchOPConfiguration(issuer: issuer)
                    
                    // 2. get FIDO configuration
                    fidoConfiguration = try await fetchFidoConfiguration(issuer: issuer)
                    
                    // 3. check OIDC client
                    let oidcClient: OIDCClient? = try await fetchOIDCClient()
                    
                    // 4. Check application integrity
                    appIntegrity = try await checkAppIntegrity()
                    
                    // 5. Show the UI
                    presenter.onViewStateChanged(viewState: .register)
                } catch {
                    print("Request failed with error: \(error)")
                }
            }
        }
    }
    
    func fetchOPConfiguration(issuer: String) async throws -> OPConfiguration? {
        if let configuration: OPConfiguration = RealmManager.shared.getObject() {
            return configuration
        }
        let opConfiguration: OPConfiguration? = await withCheckedContinuation { continuation in
            serviceClient.getOPConfiguration(url: issuer + AppConfig.OP_CONFIG_URL)
                .sink { result in
                switch result {
                case .success(let configurationResult):
                    continuation.resume(returning: configurationResult)
                    RealmManager.shared.save(object: configurationResult)
                case .failure(let error):
                    print("error: \(error)")
                    continuation.resume(returning: nil)
                }
            }
            .store(in: &cancellableSet)
        }
        
        return opConfiguration
    }
    
    func fetchFidoConfiguration(issuer: String) async throws -> FidoConfiguration? {
        if let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject() {
            return fidoConfiguration
        }
        let fidoConfiguration: FidoConfiguration? = await withCheckedContinuation { continuation in
            serviceClient.getFidoConfiguration(url: issuer + AppConfig.FIDO_CONFIG_URL)
                .sink { result in
                switch result {
                case .success(let configurationResult):
                    continuation.resume(returning: configurationResult)
                    RealmManager.shared.save(object: configurationResult)
                case .failure(let error):
                    print("error: \(error)")
                    continuation.resume(returning: nil)
                }
            }
            .store(in: &cancellableSet)
        }
        
        return fidoConfiguration
    }
    
    func fetchOIDCClient() async throws -> OIDCClient? {
        if let oidcClient: OIDCClient = RealmManager.shared.getObject() {
            return oidcClient
        }
        
        guard let configuration: OPConfiguration = RealmManager.shared.getObject(), let dcRequest = DCRRepository().makeDCRRequestUsingSSA(configuration: configuration, ssa: AppConfig.SSA, scopeText: AppConfig.ALLOWED_REGISTRATION_SCOPES) else {
            return nil
        }
        
        let oidcClient: OIDCClient? = await withCheckedContinuation { continuation in
            serviceClient.doDCR(dcRequest: dcRequest, url: configuration.registrationEndpoint)
                .sink { result in
                    switch result {
                    case .success(let dcResponse):
                        print("Inside doDCR :: DCResponse :: \(dcResponse)")
                        let oidcClient = OIDCClient()
                        oidcClient.clientName = dcResponse.clientName ?? ""
                        oidcClient.clientId = dcResponse.clientId ?? ""
                        oidcClient.clientSecret = dcResponse.clientSecret  ?? ""
                        oidcClient.scope = AppConfig.ALLOWED_REGISTRATION_SCOPES.split(separator: ",").joined(separator: " ")
                        
                        RealmManager.shared.save(object: oidcClient)
                        
                        continuation.resume(returning: oidcClient)
                    case .failure(let error):
                        print("Error in DCR.\n Error:  \(error)")
                        continuation.resume(returning: nil)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return oidcClient
    }
    
    func checkAppIntegrity() async throws -> AppIntegrityEntity? {
        if let appIntegrity: AppIntegrityEntity = RealmManager.shared.getObject() {
            return appIntegrity
        }
        
        RealmManager.shared.deleteAllAppIntegrity()
        integrityRepository.checkAppIntegrity()
        
        return nil
    }
    
    func processLogin(username: String, password: String, authMethod: String,
                      assertionResultRequest: String?) async throws -> LoginResponse {
        var loginResponse: LoginResponse = LoginResponse(authorizationCode: "")
        guard let opConfiguration: OPConfiguration = RealmManager.shared.getObject() else {
            loginResponse.isSuccess = false
            loginResponse.errorMessage = "OpenID configuration not found in database."
            return loginResponse
        }
        
        guard let oidcClient: OIDCClient = RealmManager.shared.getObject() else {
            loginResponse.isSuccess = false
            loginResponse.errorMessage = "OpenID client not found in database."
            return loginResponse
        }
        
        loginResponse = await withCheckedContinuation { continuation in
            serviceClient.getAuthorizationChallenge(
                clientId: oidcClient.clientId,
                username: username,
                password: password,
                useDeviceSession: true,
                acrValues: "passkey",
                authMethod: authMethod,
                assertionResultRequest: assertionResultRequest ?? "",
                authorizationChallengeEndpoint: opConfiguration.authorizationChallengeEndpoint
            )
                .sink { result in
                    switch result {
                    case .success(let configuration):
                        print("configuration: \(configuration)")
                        continuation.resume(returning: configuration)
                    case .failure(let error):
                        print("error: \(error)")
                        loginResponse.isSuccess = false
                        loginResponse.errorMessage = error.localizedDescription
                        continuation.resume(returning: loginResponse)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return loginResponse
    }
    
    func getToken(authorizationCode: String) async throws -> TokenResponse {
        var tokenResponse: TokenResponse = TokenResponse(accessToken: "", tokenType: "")
        guard let opConfiguration: OPConfiguration = RealmManager.shared.getObject() else {
            tokenResponse.isSuccess = false
            tokenResponse.errorMessage = "OpenID configuration not found in database."
            return tokenResponse
        }
        
        guard let oidcClient: OIDCClient = RealmManager.shared.getObject() else {
            tokenResponse.isSuccess = false
            tokenResponse.errorMessage = "OpenID client not found in database."
            return tokenResponse
        }
        
        tokenResponse = await withCheckedContinuation { continuation in
            let authHeaderEncodedString = "\(oidcClient.clientId):\(oidcClient.clientSecret)".data(using: .utf8)?.base64EncodedString() ?? ""
            serviceClient.getToken(
                clientId: oidcClient.clientId,
                code: authorizationCode,
                grantType: "authorization_code",
                redirectUri: opConfiguration.issuer,
                scope: oidcClient.scope,
                authHeader: "Basic \(authHeaderEncodedString)",
                dpopJwt: DPoPProofFactory.shared.issueDPoPJWTToken(httpMethod: "POST", requestUrl: opConfiguration.issuer),
                url: opConfiguration.tokenEndpoint)
            .sink { result in
                switch result {
                case .success(let token):
                    print("token: \(token)")
                    continuation.resume(returning: token)
                case .failure(let error):
                    print("error: \(error)")
                    tokenResponse.isSuccess = false
                    tokenResponse.errorMessage = error.localizedDescription
                    continuation.resume(returning: tokenResponse)
                }
            }
            .store(in: &cancellableSet)
        }
        
        return tokenResponse
    }
    
    func getUserInfo(accessToken: String) async throws -> UserInfo {
        var userInfo = UserInfo()
        guard let opConfiguration: OPConfiguration = RealmManager.shared.getObject() else {
            userInfo.isSuccess = false
            userInfo.errorMessage = "OpenID configuration not found in database."
            return userInfo
        }
        
        userInfo = await withCheckedContinuation { continuation in
            serviceClient.getUserInfo(accessToken: accessToken, authHeader: "Bearer \(accessToken)", url: opConfiguration.userinfoEndpoint)
                .sink { result in
                    switch result {
                    case .success(let userAdditionalInfo):
                        print("userInfo: \(userAdditionalInfo)")
                        userInfo.additionalInfo = userAdditionalInfo
                        continuation.resume(returning: userInfo)
                    case .failure(let error):
                        print("error: \(error)")
                        userInfo.isSuccess = false
                        userInfo.errorMessage = error.localizedDescription
                        continuation.resume(returning: userInfo)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return userInfo
    }
}
