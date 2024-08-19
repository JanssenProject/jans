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
    func onShowAlert(message: String, onAction: @escaping (AlertAction) -> Void)
    func onKeypassDelete(publicKeyCredentialSource: PublicKeyCredentialSource)
}

final class LoginViewInteractorImpl: LoginViewInteractor, LoginFlowProvider {
    
    private let presenter: LoginViewPresenter
    private let mainViewModel: MainViewModel
    
    private let biometricHelper = BiometricHelper.shared
    
    var isBiometricAvailable: Bool {
        guard Platform.isSimulator == false else {
            return Platform.isSimulator
        }
        
        return biometricHelper.canUseBiometricAuthentication()
    }
    
    var authMethod: String { "authenticate" }
    
    init(presenter: LoginViewPresenter, mainViewModel: MainViewModel) {
        self.presenter = presenter
        self.mainViewModel = mainViewModel
    }
    
    func onRegisterClick(username: String, password: String) {
        presenter.onLoading(visible: true)
        
        guard isBiometricAvailable else {
            presenter.onError(message: "Biometric authentication is not available!")
            return
        }
        
        Task {
            if let assertionResult = try? await proceedAssertion(username: username) {
                biometricHelper.authenticateWithBiometrics { [weak self] success, error in
                    DispatchQueue.main.async { [weak self] in
                        self?.proceedLoginFlow(username: username, password: password, assertionResult: assertionResult)
                    }
                }
            }
        }
    }
    
    func onShowAlert(message: String, onAction: @escaping (AlertAction) -> Void) {
        presenter.showAlert(message: message, onAction: onAction)
    }
    
    func goToEnrol() {
        presenter.onMainStateChanged(viewState: .register)
    }
    
    func goToSuccessLoginView(userInfo: UserInfo) {
        presenter.onMainStateChanged(viewState: .afterLogin(userInfo))
    }
    
    func onKeypassDelete(publicKeyCredentialSource: PublicKeyCredentialSource) {
        RealmManager.shared.delete(objectToDelete: publicKeyCredentialSource) { [weak self] result in
            if result {
                self?.presenter.onReloadList()
            } else {
                self?.presenter.onError(message: "Failed to delete passkey")
            }
        }
    }
    
    // MARK: - Private
    
    private func proceedAssertion(username: String) async throws -> AssertionResultRequest? {
        let assertion = try await mainViewModel.assertionOption(username: username)
        guard assertion.isSuccess == true else {
            DispatchQueue.main.async { [weak self] in
                self?.presenter.onError(message: assertion.errorMessage ?? "")
            }
            return nil
        }
        
        let authAdaptor = AuthAdaptor()
        
        if let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject() {
            let selectedPublicKeyCredentialSource = authAdaptor.selectPublicKeyCredentialSource(
                credentialSelector: LocalCredentialSelector(),
                assertionOptionResponse: assertion,
                origin: fidoConfiguration.issuer)
            authAdaptor.generateSignature(credentialSource: selectedPublicKeyCredentialSource)
            
            let assertionResultRequest = authAdaptor.authenticate(
                assertionOptionResponse: assertion,
                origin: fidoConfiguration.issuer,
                selectedCredential: selectedPublicKeyCredentialSource)
            return assertionResultRequest
        }
        
        return nil
    }
    
    private func proceedLoginFlow(username: String, password: String, assertionResult: AssertionResultRequest?) {
        var assertionResultRequest = ""
        assertionResult.flatMap {
            assertionResultRequest = JSONDecoding.shared.objectToJSON(object: $0) ?? ""
        }
        mainViewModel.proceedFlowGetUserInfo(
            username: username, 
            password: password,
            authMethod: authMethod, 
            assertionResultRequest: assertionResultRequest
        ) { [weak self] userInfo, errorMessage in
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
}

