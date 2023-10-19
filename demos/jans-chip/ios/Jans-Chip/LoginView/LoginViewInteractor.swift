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
                case .failure(let error):
                    print("error: \(error)")
                    self?.presenter.onError(message: "Error in generating authorization code. Erorr: \(error.localizedDescription)")
                }
            }
            .store(in: &cancellableSet)
    }
}
