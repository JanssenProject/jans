//
//  RegisterViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import Combine
import RealmSwift

protocol RegisterViewInteractor: AnyObject {
    
    func onLoginClick(username: String, password: String)
    func goToLogin()
}

final class RegisterViewInteractorImpl: RegisterViewInteractor {
    
    private let presenter: RegisterViewPresenter
    
    private let authenticator = Authenticator()
    private let authAdapter = AuthAdaptor()
    private let mainInteractor = MainViewInteractorImpl(presenter: MainViewPresenterImpl(state: MainViewState()))
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    @ObservedResults(OIDCClient.self) var oidcClient
    @ObservedResults(OPConfiguration.self) var opConfiguration
    
    init(presenter: RegisterViewPresenter) {
        self.presenter = presenter
    }
    
    func goToLogin() {
        presenter.onViewStateChanged(viewState: .login)
    }
    
    func onLoginClick(username: String, password: String) {
        guard !username.isEmpty else {
            presenter.onError(message: "'User name' value cannot be empty")
            return
        }
        
        guard !password.isEmpty else {
            presenter.onError(message: "'Password' value cannot be empty")
            return
        }
        
        if authAdapter.isCredentialsPresent(username: username) {
            presenter.onError(message: "Username already enrolled!")
            presenter.onLoading(visible: false)
            return
        }
        
        presenter.onLoading(visible: true)
        
        Task {
            do {
                
                // 1. Get login response
                let loginResponse = try await mainInteractor.processLogin(
                    username: username,
                    password: password,
                    authMethod: "passkey",
                    assertionResultRequest: nil
                )
                guard loginResponse.isSuccess else {
                    presenter.onError(message: loginResponse.errorMessage ?? "")
                    return
                }
                
                // 2. get token
                let token = try await mainInteractor.getToken(
                    authorizationCode: loginResponse.authorizationCode)
                guard token.isSuccess == true else {
                    presenter.onError(message: token.errorMessage ?? "")
                    return
                }
                
                if let oidcClient: OIDCClient = RealmManager.shared.getObject() {
                    let accessToken = token.accessToken
                    let idToken = token.idToken ?? ""
                    if !accessToken.isEmpty {
                        RealmManager.shared.updateOIDCClient(oidcCClient: oidcClient, with: accessToken, and: idToken)
                    }
                }
                
                let userInfo = try await mainInteractor.getUserInfo(accessToken: token.accessToken)
                guard userInfo.isSuccess == true else {
                    presenter.onError(message: userInfo.errorMessage ?? "")
                    return
                }
                
                let attestationOptionResponse = try await attestationOption(userName: username, passwordText: password, userInfo: userInfo)
                let handledOptionResponse = try await handleAttestationOption(response: attestationOptionResponse, passwordText: password, userInfo: userInfo)
                guard handledOptionResponse.isSuccess == true else {
                    presenter.onError(message: handledOptionResponse.errorMessage ?? "")
                    return
                }
                
                presenter.onViewStateChanged(viewState: .afterLogin(userInfo))
                presenter.onLoading(visible: false)
            }
        }
    }
    
    // TODO: server returns error 500
    
    private func attestationOption(userName: String, passwordText: String, userInfo: UserInfo) async throws -> AttestationOptionResponse {
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
                    switch result {
                    case .success(let response):
                        print("attestation option response: \(response)")
                        continuation.resume(returning: response)
                    case .failure(let error):
                        print("Error in fetching AttestationOptionResponse : \(error)")
                        attestationResponse.isSuccess = false
                        attestationResponse.errorMessage = error.localizedDescription
                        continuation.resume(returning: attestationResponse)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return attestationResponse
    }
    
    private func handleAttestationOption(response: AttestationOptionResponse, passwordText: String, userInfo: UserInfo) async throws -> AttestationOptionResponse {
        var attestationOptionResponse = response
        guard let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject() else {
            attestationOptionResponse.isSuccess = false
            attestationOptionResponse.errorMessage = "Fido configuration not found in database."
            return attestationOptionResponse
        }
        
        if response.challenge == nil {
            let errorMessage = "Challenge field in attestationOptionResponse is null."
            attestationOptionResponse.isSuccess = false
            attestationOptionResponse.errorMessage = errorMessage
            return attestationOptionResponse
        } else {
            let publicKeyCredentialSource = getPublicKeyCredentialSource(responseFromAPI: attestationOptionResponse, passwordText: passwordText, origin: fidoConfiguration.issuer)
            if let attestationResultRequest = authenticator.register(responseFromAPI: attestationOptionResponse, origin: fidoConfiguration.issuer, credentialSource: publicKeyCredentialSource) {
                    return try await attestationResult(attestationResultRequest: attestationResultRequest)
            }
        }
        
        return attestationOptionResponse
    }
    
    private func getPublicKeyCredentialSource(responseFromAPI: AttestationOptionResponse?, passwordText: String, origin: String?) -> PublicKeyCredentialSource? {
        let options = authenticator.generateAuthenticatorMakeCredentialOptions(responseFromAPI: responseFromAPI, origin: origin)
        return authenticator.getPublicKeyCredentialSource(options: options, passwordText: passwordText)
    }
    
    private func attestationResult(attestationResultRequest: AttestationResultRequest) async throws -> AttestationOptionResponse {
        var attestationOptionResponse = AttestationOptionResponse()
        guard let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject(), let url = fidoConfiguration.issuer else {
            attestationOptionResponse.isSuccess = false
            attestationOptionResponse.errorMessage = "Fido configuration not found in database."
            return attestationOptionResponse
        }
        
        attestationOptionResponse = await withCheckedContinuation { continuation in
            serviceClient.attestationResult(request: attestationResultRequest, url: url)
                .sink { result in
                    switch result {
                    case .success(let response):
                        print("attestation option response: \(response)")
                        continuation.resume(returning: response)
                    case .failure(let error):
                        print("Error in fetching AttestationOptionResponse : \(error)")
                        attestationOptionResponse.isSuccess = false
                        attestationOptionResponse.errorMessage = error.localizedDescription
                        continuation.resume(returning: attestationOptionResponse)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return attestationOptionResponse
    }
}
