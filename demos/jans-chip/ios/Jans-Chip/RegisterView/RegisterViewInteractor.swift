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
    
    private let dcrRepository = DCRRepository()
    
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
                case .success(let configurationResult):
                    var configuration = configurationResult
                    configuration.isSuccessful = true
                    configuration.sno = AppConfig.DEFAULT_S_NO
                    
                    print("Inside fetchOPConfiguration :: opConfiguration :: \(configuration.toString)")
                    
                    self?.handleFetchOPConfiguration(configuration: configuration, scopeText: scopeText)
                case .failure(let error):
                    print("error: \(error)")
                    self?.presenter.onError(message: error.localizedDescription)
                }
            }
            .store(in: &cancellableSet)
    }
    
    private func handleFetchOPConfiguration(configuration: OPConfiguration, scopeText: String) {
        if configuration.isSuccessful {
            // Save configuration into local DB
            RealmManager.shared.deleteAllConfiguration()
            RealmManager.shared.save(object: configuration.opConfigurationObject)
            doDCR(scopeText: scopeText)
        } else {
            presenter.onError(message: "Error with fetching OPConfiguration")
        }
    }
    
    private func doDCR(scopeText: String) {
        dcrRepository.doDCR(scopeText: scopeText)
    }
}
