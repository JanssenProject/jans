//
//  MainViewModel.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 23.07.2024.
//

import Foundation
import Combine

protocol MainViewModel {
    
    func proceedFlowGetUserInfo(username: String, password: String, authMethod: String, assertionResultRequest: String?, completion: @escaping ((UserInfo?, String?) -> Void))
    func assertionOption(username: String) async throws -> AssertionOptionResponse
    func attestationOption(userName: String, passwordText: String, userInfo: UserInfo) async throws -> AttestationOptionResponse
    func attestationResult(attestationResultRequest: AttestationResultRequest) async throws -> AttestationOptionResponse
    func doDCRUsingSSA(ssa: String, scopeText: String) async throws -> OIDCClient?
}

final class MainViewModelImpl: MainViewModel {
    private let acrValues = "passkey"
    
    private let dcrRepository = DCRRepository()
    private var cancellableSet : Set<AnyCancellable> = []
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    func proceedFlowGetUserInfo(
        username: String,
        password: String,
        authMethod: String,
        assertionResultRequest: String?,
        completion: @escaping ((UserInfo?, String?) -> Void)
    ) {
        Task {
            // 1. Get login response
            let loginResponse = try await processLogin(
                username: username,
                password: password,
                authMethod: authMethod,
                assertionResultRequest: assertionResultRequest
            )
            guard loginResponse.isSuccess else {
                completion(nil, loginResponse.errorMessage ?? "")
                return
            }
            
            // 2. get token
            let token = try await getToken(
                authorizationCode: loginResponse.authorizationCode)
            guard token.isSuccess == true else {
                completion(nil, token.errorMessage ?? "")
                return
            }
            
            if let oidcClient: OIDCClient = RealmManager.shared.getObject() {
                let accessToken = token.accessToken
                let idToken = token.idToken ?? ""
                if !accessToken.isEmpty {
                    RealmManager.shared.updateOIDCClient(oidcCClient: oidcClient, with: accessToken, and: idToken)
                }
            }
            
            let userInfo = try await getUserInfo(accessToken: token.accessToken)
            guard userInfo.isSuccess == true else {
                completion(nil, userInfo.errorMessage ?? "")
                return
            }
            
            completion(userInfo, nil)
        }
    }
    
    func assertionOption(username: String) async throws -> AssertionOptionResponse {
        var optionResponse: AssertionOptionResponse = AssertionOptionResponse(isSuccess: false)
        guard let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject(), let assertionOptionsEndpoint = fidoConfiguration.assertion?.optionsEndpoint else {
            optionResponse.isSuccess = false
            optionResponse.errorMessage = "Fido configuration not found in database."
            return optionResponse
        }
        
        let assertionOptioRequest = AssertionOptionRequest(username: username)
        optionResponse = await withCheckedContinuation { continuation in
            serviceClient.assertionOption(request: assertionOptioRequest, url: assertionOptionsEndpoint)
                .sink { result in
                    if let error = result.error {
                        print("assertionOption method gets an error: \(error)")
                        optionResponse.isSuccess = false
                        optionResponse.errorMessage = error.localizedDescription
                        continuation.resume(returning: optionResponse)
                    } else if var optionResponse = result.value {
                        optionResponse.isSuccess = true
                        print("AssertionOptionResponse: \(optionResponse)")
                        continuation.resume(returning: optionResponse)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return optionResponse
    }
    
    func attestationOption(userName: String, passwordText: String, userInfo: UserInfo) async throws -> AttestationOptionResponse {
        var attestationResponse = AttestationOptionResponse()
        guard let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject(), let attestationOptionsEndpoint = fidoConfiguration.attestation?.optionsEndpoint else {
            attestationResponse.isSuccess = false
            attestationResponse.errorMessage = "Fido configuration not found in database."
            return attestationResponse
        }
        
        let request = AttestationOptionRequest(username: userName, attestation: "none")
        attestationResponse = await withCheckedContinuation { continuation in
            serviceClient.attestationOption(attestationRequest: request, url: attestationOptionsEndpoint)
                .sink { result in
                    if let error = result.error {
                        print("attestationOption method gets an error : \(error)")
                        attestationResponse.isSuccess = false
                        attestationResponse.errorMessage = error.localizedDescription
                        continuation.resume(returning: attestationResponse)
                    } else if let response = result.value {
                        print("attestation option response: \(response)")
                        continuation.resume(returning: response)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return attestationResponse
    }
    
    func attestationResult(attestationResultRequest: AttestationResultRequest) async throws -> AttestationOptionResponse {
        var attestationOptionResponse = AttestationOptionResponse()
        guard let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject(), let url = fidoConfiguration.issuer else {
            attestationOptionResponse.isSuccess = false
            attestationOptionResponse.errorMessage = "Fido configuration not found in database."
            return attestationOptionResponse
        }
        
        attestationOptionResponse = await withCheckedContinuation { continuation in
            serviceClient.attestationResult(request: attestationResultRequest, url: url, completion: { result in
                if result.lowercased() == ServiceClient.simpleSuccess.lowercased() {
                    attestationOptionResponse.isSuccess = true
                    attestationOptionResponse.errorMessage = nil
                    continuation.resume(returning: attestationOptionResponse)
                } else {
                    attestationOptionResponse.isSuccess = false
                    attestationOptionResponse.errorMessage = "Error in assertion option."
                    continuation.resume(returning: attestationOptionResponse)
                }
            })
        }
        
        return attestationOptionResponse
    }
    
    func doDCRUsingSSA(ssa: String, scopeText: String) async throws -> OIDCClient? {
        try? await dcrRepository.doDCRUsingSSA(ssaJwt: ssa, scopeText: scopeText)
    }
}

private extension MainViewModelImpl {
    
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
                acrValues: acrValues,
                authMethod: authMethod,
                assertionResultRequest: assertionResultRequest ?? "",
                authorizationChallengeEndpoint: opConfiguration.authorizationChallengeEndpoint
            )
                .sink { result in
                    if let error = result.error {
                        print("getAuthorizationChallenge method gets an error: \(error)")
                        loginResponse.isSuccess = false
                        loginResponse.errorMessage = error.localizedDescription
                        continuation.resume(returning: loginResponse)
                    } else if let configuration = result.value {
                        print("configuration: \(configuration)")
                        continuation.resume(returning: configuration)
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
                grantType: AppConfig.GRAND_TYPE,
                redirectUri: opConfiguration.issuer,
                scope: oidcClient.scope,
                authHeader: "Basic \(authHeaderEncodedString)",
                dpopJwt: DPoPProofFactory.shared.issueDPoPJWTToken(httpMethod: "POST", requestUrl: opConfiguration.issuer),
                url: opConfiguration.tokenEndpoint)
            .sink { result in
                if let error = result.error {
                    print("getToken method gets an error: \(error)")
                    tokenResponse.isSuccess = false
                    tokenResponse.errorMessage = error.localizedDescription
                    continuation.resume(returning: tokenResponse)
                } else if let token = result.value {
                    print("token: \(token)")
                    continuation.resume(returning: token)
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
                    if let error = result.error {
                        print("getUserInfo method gets an error: \(error)")
                        userInfo.isSuccess = false
                        userInfo.errorMessage = error.localizedDescription
                        continuation.resume(returning: userInfo)
                    } else if let userAdditionalInfo = result.value {
                        print("userInfo: \(userAdditionalInfo)")
                        userInfo.additionalInfo = userAdditionalInfo
                        continuation.resume(returning: userInfo)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return userInfo
    }
}
