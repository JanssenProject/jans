//
//  RegisterViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import Combine

typealias ServiceResult = (Bool, Error?) -> Void

protocol RegisterViewInteractor: AnyObject {
    
    func onRegisterClick(issuer: String, scope: String)
}

final class RegisterViewInteractorImpl: RegisterViewInteractor {
    
    private let presenter: RegisterViewPresenterImpl
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    init(presenter: RegisterViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onRegisterClick(issuer: String, scope: String) {
        guard !issuer.isEmpty else {
            presenter.onError(message: "'Issuer' value cannot be empty")
            return
        }
        
        guard !scope.isEmpty else {
            presenter.onError(message: "'Scope' value cannot be empty")
            return
        }
        
        fetchOPConfiguration(configurationUrl: issuer, scopeText: scope)
    }
    
    // Step 1: Get Configuration
    private func fetchOPConfiguration(configurationUrl: String, scopeText: String) {
        serviceClient.getOPConfiguration(url: configurationUrl)
            .sink { [weak self] result in
                switch result {
                case .success(let configuration):
                    print("configuration: \(configuration)")
                    self?.handleFetchOPConfiguration(configuration: configuration)
                case .failure(let error):
                    print("error: \(error)")
                }
            }
            .store(in: &cancellableSet)
    }
    
    private func handleFetchOPConfiguration(configuration: OPConfiguration) {
        // Save configuration into local DB
//        RealmManager.shared.save(object: configuration)
    }
}
