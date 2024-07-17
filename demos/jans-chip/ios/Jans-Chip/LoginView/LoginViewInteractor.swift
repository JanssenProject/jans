//
//  LoginViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import Combine
import RealmSwift

protocol LoginViewInteractor: AnyObject {
    
    func onAppear()
    func onLoginClick(username: String, password: String)
    func goToEnrol()
}

final class LoginViewInteractorImpl: LoginViewInteractor {
    
    private let presenter: LoginViewPresenterImpl
    
    private let authenticator = Authenticator()
    
    private let authAdapter = AuthAdaptor()
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    @ObservedResults(OIDCClient.self) var oidcClient
    @ObservedResults(OPConfiguration.self) var opConfiguration
    
    init(presenter: LoginViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onAppear() {
        let viewState: ViewState = oidcClient.isEmpty ? .register : .login
        presenter.onViewStateChanged(viewState: viewState)
    }
    
    func goToEnrol() {
        presenter.onViewStateChanged(viewState: .register)
    }
    
    func onLoginClick(username: String, password: String) {
        
        if authAdapter.isCredentialsPresent(username: username) {
            presenter.onError(message: "Username already enrolled!")
            presenter.onLoading(visible: false)
            return
        }
        
        // Get OPConfiguration and OIDCClient
        guard let oidcClient: OIDCClient = RealmManager.shared.getObject(), let opConfiguration: OPConfiguration = RealmManager.shared.getObject() else {
            return
        }
        presenter.onLoading(visible: true)
        // Create a call to request an authorization challenge
        
        serviceClient.getAuthorizationChallenge(clientId: oidcClient.clientId, username: username, password: password, authorizationChallengeEndpoint: opConfiguration.authorizationChallengeEndpoint, url: opConfiguration.issuer)
            .sink { [weak self] result in
                switch result {
                case .success(let configuration):
                    print("configuration: \(configuration)")
                    self?.getToken(authorizationCode: configuration.authorizationCode, usernameText: username, passwordText: password)
                case .failure(let error):
                    print("error: \(error)")
                    self?.presenter.onError(message: "Error in generating authorization code. Erorr: \(error.localizedDescription)")
                    self?.presenter.onLoading(visible: false)
                }
            }
            .store(in: &cancellableSet)
    }
    
    func getToken(authorizationCode: String, usernameText: String, passwordText: String) {
        // Get OPConfiguration and OIDCClient
        guard let oidcClient: OIDCClient = RealmManager.shared.getObject(), let opConfiguration: OPConfiguration = RealmManager.shared.getObject() else {
            presenter.onLoading(visible: false)
            return
        }
        
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
        .sink { [weak self] result in
            switch result {
            case .success(let token):
                print("token: \(token)")
                let accessToken = token.accessToken
                let idToken = token.idToken ?? ""
                if !accessToken.isEmpty {
                    RealmManager.shared.updateOIDCClient(oidcCClient: oidcClient, with: accessToken, and: idToken)
                    self?.getUserInfo(accessToken: accessToken)
                } else {
                    self?.presenter.onLoading(visible: false)
                }
            case .failure(let error):
                print("error: \(error)")
                self?.presenter.onError(message: "Error in generating authorization token. Error: \(error.localizedDescription)")
                self?.presenter.onLoading(visible: false)
            }
        }
        .store(in: &cancellableSet)
    }
    
    private func getUserInfo(accessToken: String) {
        guard let opConfiguration: OPConfiguration = RealmManager.shared.getObject() else {
            presenter.onLoading(visible: false)
            return
        }
        
        serviceClient.getUserInfo(accessToken: accessToken, authHeader: "Bearer \(accessToken)", url: opConfiguration.userinfoEndpoint)
            .sink { [weak self] result in
                switch result {
                case .success(let userInfo):
                    print("userInfo: \(userInfo)")
                    self?.attestationOption(userName: userInfo.name ?? "admin", userInfo: userInfo)
                case .failure(let error):
                    print("error: \(error)")
                    self?.presenter.onError(message: "Error in getting user info. Erorr: \(error.localizedDescription)")
                    self?.presenter.onLoading(visible: false)
                }
            }
            .store(in: &cancellableSet)
    }
    
    // TODO: server returns error 500
    
    private func attestationOption(userName: String, userInfo: UserInfo) {
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
                    self?.handleAttestationOption(response: response, origin: fidoConfiguration.issuer)
                case .failure(let error):
                    print("Error in fetching AttestationOptionResponse : \(error)")
                    self?.presenter.onError(message: "Error in fetching AttestationOptionResponse : \(error.localizedDescription)")
                    self?.presenter.onLoading(visible: false)
                }
            }
            .store(in: &cancellableSet)
    }
    
    private func handleAttestationOption(response: AttestationOptionResponse, origin: String?) {
        var attestationOptionResponse = response
        if response.challenge == nil {
            let errorMessage = "Challenge field in attestationOptionResponse is null."
            attestationOptionResponse.isSuccessful = false
            attestationOptionResponse.errorMessage = errorMessage
            presenter.onError(message: errorMessage)
            presenter.onLoading(visible: false)
        } else {
            let publicKeyCredentialSource = getPublicKeyCredentialSource(responseFromAPI: attestationOptionResponse, origin: origin)
            if let attestationResultRequest = authenticator.register(responseFromAPI: attestationOptionResponse, origin: origin, credentialSource: publicKeyCredentialSource) {
                Task {
                    do {
                        let attestationResultResponse = try await attestationResult(attestationResultRequest: attestationResultRequest)
                    }
                }
            }
        }
    }
    
    private func getPublicKeyCredentialSource(responseFromAPI: AttestationOptionResponse?, origin: String?) -> PublicKeyCredentialSource? {
        let options = authenticator.generateAuthenticatorMakeCredentialOptions(responseFromAPI: responseFromAPI, origin: origin)
        return authenticator.getPublicKeyCredentialSource(options: options)
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
