//
//  LoginViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import Combine

protocol LoginViewInteractor: AnyObject {
    
    func onRegisterClick(issuer: String, scope: String, username: String, password: String)
    func goToEnrol()
}

final class LoginViewInteractorImpl: LoginViewInteractor {
    
    private let presenter: LoginViewPresenter
    private let mainInteractor: MainViewInteractor
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private let dcrRepository = DCRRepository()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    init(presenter: LoginViewPresenter, mainInteractor: MainViewInteractor) {
        self.presenter = presenter
        self.mainInteractor = mainInteractor
    }
    
    func onRegisterClick(issuer: String, scope: String, username: String, password: String) {
        guard !issuer.isEmpty else {
            presenter.onError(message: "'Issuer' value cannot be empty")
            return
        }
        
        guard !scope.isEmpty else {
            presenter.onError(message: "'Scope' value cannot be empty")
            return
        }
        
        presenter.onLoading(visible: true)
        
        Task {
            do {
                // 1. get login response
                let loginResponse = try await mainInteractor.processLogin(
                    username: username,
                    password: password,
                    authMethod: "authenticate",
                    assertionResultRequest: nil)
                guard loginResponse.isSuccess == true else {
                    DispatchQueue.main.async { [weak self] in
                        self?.presenter.onError(message: loginResponse.errorMessage ?? "")
                    }
                    return
                }
                
                // 2. get token
                let token = try await mainInteractor.getToken(
                    authorizationCode: loginResponse.authorizationCode)
                guard token.isSuccess == true else {
                    DispatchQueue.main.async { [weak self] in
                        self?.presenter.onError(message: token.errorMessage ?? "")
                    }
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
                    DispatchQueue.main.async { [weak self] in
                        self?.presenter.onError(message: userInfo.errorMessage ?? "")
                    }
                    return
                }
                
                DispatchQueue.main.async { [weak self] in
                    self?.goToSuccessLoginView(userInfo: userInfo)
                }
            }
        }
    }
    
    func goToEnrol() {
        presenter.onMainStateChanged(viewState: .register)
    }
    
    func goToSuccessLoginView(userInfo: UserInfo) {
        presenter.onMainStateChanged(viewState: .afterLogin(userInfo))
    }
}

