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

final class RegisterViewInteractorImpl: RegisterViewInteractor, LoginFlowProvider {
    
    private let presenter: RegisterViewPresenter
    private let mainViewModel: MainViewModel
    
    private let authenticator = Authenticator()
    private let authAdapter = AuthAdaptor()
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    var authMethod: String { "enroll" }
    
    @ObservedResults(OIDCClient.self) var oidcClient
    @ObservedResults(OPConfiguration.self) var opConfiguration
    
    init(presenter: RegisterViewPresenter, mainViewModel: MainViewModel) {
        self.presenter = presenter
        self.mainViewModel = mainViewModel
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
        
        
        mainViewModel.proceedFlowGetUserInfo(
            username: username,
            password: password,
            authMethod: authMethod,
            assertionResultRequest: nil) { [weak self] userInfo, errorMessage in
                if let errorMessage {
                    self?.presenter.onError(message: errorMessage)
                    return
                }
                
                if let userInfo {
                    Task {
                        do {
                            let attestationOptionResponse = try await self?.mainViewModel.attestationOption(userName: username, passwordText: password, userInfo: userInfo)
                            if let attestationOptionResponse {
                                guard attestationOptionResponse.isSuccess == true else {
                                    self?.presenter.onError(message: attestationOptionResponse.errorMessage ?? "")
                                    return
                                }
                                
                                let handledOptionResponse = try await self?.handleAttestationOption(response: attestationOptionResponse, passwordText: password, userInfo: userInfo)
                                guard handledOptionResponse?.isSuccess == true else {
                                    self?.presenter.onError(message: handledOptionResponse?.errorMessage ?? "")
                                    return
                                }
                                
                                DispatchQueue.main.async {
                                    self?.presenter.onViewStateChanged(viewState: .afterLogin(userInfo))
                                }
                            }
                        }
                    }
                }
            }
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
                return try await mainViewModel.attestationResult(attestationResultRequest: attestationResultRequest)
            }
        }
        
        return attestationOptionResponse
    }
    
    private func getPublicKeyCredentialSource(responseFromAPI: AttestationOptionResponse?, passwordText: String, origin: String?) -> PublicKeyCredentialSource? {
        let options = authenticator.generateAuthenticatorMakeCredentialOptions(responseFromAPI: responseFromAPI, origin: origin)
        return authenticator.getPublicKeyCredentialSource(options: options, passwordText: passwordText)
    }
}
