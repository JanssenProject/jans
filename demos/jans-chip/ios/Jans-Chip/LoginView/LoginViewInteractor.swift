//
//  LoginViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import Combine

protocol LoginViewInteractor: AnyObject {
    
    func onRegisterClick(username: String, password: String)
    func goToEnrol()
}

final class LoginViewInteractorImpl: LoginViewInteractor {
    
    private let presenter: LoginViewPresenter
    private let mainViewModel: MainViewModel
    
    private let biometricHelper = BiometricHelper.shared
    
    var isBiometricAvailable: Bool {
        guard Platform.isSimulator == false else {
            return Platform.isSimulator
        }
        
        return biometricHelper.canUseBiometricAuthentication()
    }
    
    init(presenter: LoginViewPresenter, mainViewModel: MainViewModel) {
        self.presenter = presenter
        self.mainViewModel = mainViewModel
    }
    
    func onRegisterClick(username: String, password: String) {
        presenter.onLoading(visible: true)
        
        if isBiometricAvailable {
            proceedAssertion(username: username)
            biometricHelper.authenticateWithBiometrics { [weak self] success, error in
                self?.proceedLoginFlow(username: username, password: password)
            }
        } else {
            presenter.onError(message: "Biometric authentication is not available!")
        }
    }
    
    // MARK: - Private
    
    private func proceedAssertion(username: String) {
        Task {
            do {
                let assertion = try await mainViewModel.assertionOption(username: username)
                guard assertion.isSuccess == true else {
                    DispatchQueue.main.async { [weak self] in
                        self?.presenter.onError(message: assertion.errorMessage ?? "")
                    }
                    return
                }
                
                let authAdaptor = AuthAdaptor()
                
                if let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject() {
                    let selectedPublicKeyCredentialSource = authAdaptor.selectPublicKeyCredentialSource(
                        credentialSelector: LocalCredentialSelector(),
                        assertionOptionResponse: assertion,
                        origin: fidoConfiguration.issuer)
                    authAdaptor.generateSignature(credentialSource: selectedPublicKeyCredentialSource)
                }
            }
        }
    }
    
    private func proceedLoginFlow(username: String, password: String) {
        mainViewModel.proceedFlowGetUserInfo(
            username: username, 
            password: password,
            authMethod: "authenticate") { [weak self] userInfo, errorMessage in
                if let errorMessage {
                    self?.presenter.onError(message: errorMessage)
                    return
                }
                
                if let userInfo {
                    DispatchQueue.main.async {
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

