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
}

final class LoginViewInteractorImpl: LoginViewInteractor {
    
    private let presenter: LoginViewPresenterImpl
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    @ObservedResults(OIDCClient.self) var oidcClient
    @ObservedResults(OPConfigurationObject.self) var opConfiguration
    
    init(presenter: LoginViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onAppear() {
        let viewState: ViewState = oidcClient.isEmpty ? .register : .login
        presenter.onViewStateChanged(viewState: viewState)
    }
    
    func onLoginClick(username: String, password: String) {
        // Get OPConfiguration and OIDCClient
        guard let oidcClient: OIDCClient = RealmManager.shared.getObject(), let opConfiguration: OPConfigurationObject = RealmManager.shared.getObject() else {
            return
        }
        
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
                }
            }
            .store(in: &cancellableSet)
    }
    
    func getToken(authorizationCode: String, usernameText: String, passwordText: String) {
        // Get OPConfiguration and OIDCClient
        guard let oidcClient: OIDCClient = RealmManager.shared.getObject(), let opConfiguration: OPConfigurationObject = RealmManager.shared.getObject() else {
            return
        }
        
        let authHeaderEncodedString = "\(oidcClient.clientId):\(oidcClient.clientSecret)".data(using: .utf8)?.base64EncodedString() ?? ""
        serviceClient.getToken(
            clientId: oidcClient.clientId,
            code: authorizationCode,
            grantType: "authorization_code",
            redirectUri: opConfiguration.issuer,
            scope: oidcClient.scope, // "openId",
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
                }
            case .failure(let error):
                print("error: \(error)")
                self?.presenter.onError(message: "Error in generating authorization token. Erorr: \(error.localizedDescription)")
            }
        }
        .store(in: &cancellableSet)
    }
    
    private func getUserInfo(accessToken: String) {
        guard let opConfiguration: OPConfigurationObject = RealmManager.shared.getObject() else {
            return
        }
        
        serviceClient.getUserInfo(accessToken: accessToken, authHeader: "Bearer \(accessToken)", url: opConfiguration.userinfoEndpoint)
            .sink { [weak self] result in
                switch result {
                case .success(let userInfo):
                    print("userInfo: \(userInfo)")
                case .failure(let error):
                    print("error: \(error)")
                    self?.presenter.onError(message: "Error in getting user info. Erorr: \(error.localizedDescription)")
                }
            }
            .store(in: &cancellableSet)
    }
}
