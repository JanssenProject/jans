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
                
                attestationOption(userName: username, passwordText: password, userInfo: userInfo)
            }
        }
    }
    
    // TODO: server returns error 500
    
    private func attestationOption(userName: String, passwordText: String, userInfo: UserInfo) {
        guard let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject(), let attestationOptionsEndpoint = fidoConfiguration.attestation?.optionsEndpoint else {
            self.presenter.onViewStateChanged(viewState: .afterLogin(userInfo))
            self.presenter.onLoading(visible: false)
            return
        }
        
        let request = AttestationOptionRequest(username: userName, attestation: "none")
        serviceClient.attestationOption(attestationRequest: request, url: attestationOptionsEndpoint)
            .sink { [weak self] result in
                switch result {
                case .success(let response):
                    print("attestation option response: \(response)")
                    self?.handleAttestationOption(response: response, origin: fidoConfiguration.issuer, passwordText: passwordText, userInfo: userInfo)
                case .failure(let error):
                    print("Error in fetching AttestationOptionResponse : \(error)")
                    self?.presenter.onError(message: "Error in fetching AttestationOptionResponse : \(error.localizedDescription)")
                    self?.presenter.onLoading(visible: false)
                }
            }
            .store(in: &cancellableSet)
    }
    
    private func handleAttestationOption(response: AttestationOptionResponse, origin: String?, passwordText: String, userInfo: UserInfo) {
        var attestationOptionResponse = response
        if response.challenge == nil {
            let errorMessage = "Challenge field in attestationOptionResponse is null."
            attestationOptionResponse.isSuccessful = false
            attestationOptionResponse.errorMessage = errorMessage
            presenter.onError(message: errorMessage)
            presenter.onLoading(visible: false)
        } else {
            let publicKeyCredentialSource = getPublicKeyCredentialSource(responseFromAPI: attestationOptionResponse, passwordText: passwordText, origin: origin)
            if let attestationResultRequest = authenticator.register(responseFromAPI: attestationOptionResponse, origin: origin, credentialSource: publicKeyCredentialSource) {
                Task {
                    do {
                        let attestationResultResponse = try await attestationResult(attestationResultRequest: attestationResultRequest)
                        print("attestationResultResponse -- \(String(describing: attestationResultResponse))")
                        
                        self.presenter.onViewStateChanged(viewState: .afterLogin(userInfo))
                        self.presenter.onLoading(visible: false)
                    }
                }
            } else {
                self.presenter.onViewStateChanged(viewState: .afterLogin(userInfo))
                self.presenter.onLoading(visible: false)
            }
        }
    }
    
    private func getPublicKeyCredentialSource(responseFromAPI: AttestationOptionResponse?, passwordText: String, origin: String?) -> PublicKeyCredentialSource? {
        let options = authenticator.generateAuthenticatorMakeCredentialOptions(responseFromAPI: responseFromAPI, origin: origin)
        return authenticator.getPublicKeyCredentialSource(options: options, passwordText: passwordText)
    }
    
    private func attestationResult(attestationResultRequest: AttestationResultRequest) async throws -> AttestationOptionResponse? {
        guard let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject(), let url = fidoConfiguration.issuer else {
            presenter.onError(message: "Fido configuration not found in database.")
            return nil
        }
        let attestationResult: AttestationOptionResponse? = await withCheckedContinuation { continuation in
            serviceClient.attestationResult(request: attestationResultRequest, url: url)
                .sink { result in
                    switch result {
                    case .success(let response):
                        print("attestation option response: \(response)")
                        continuation.resume(returning: response)
                    case .failure(let error):
                        print("Error in fetching AttestationOptionResponse : \(error)")
                        continuation.resume(returning: nil)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return attestationResult
    }
}
