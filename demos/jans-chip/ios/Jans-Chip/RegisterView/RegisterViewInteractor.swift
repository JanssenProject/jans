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
        
        presenter.onLoading(visible: true)
        fetchOPConfiguration(configurationUrl: issuer + AppConfig.OP_CONFIG_URL, scopeText: scope)
    }
    
    // Step 1: Get Configuration
    private func fetchOPConfiguration(configurationUrl: String, scopeText: String) {
        serviceClient.getOPConfiguration(url: configurationUrl)
            .sink { [weak self] result in
                switch result {
                case .success(let configurationResult):
                    let configuration = configurationResult
                    configuration.isSuccessful = true
                    configuration.sno = AppConfig.DEFAULT_S_NO
                    
                    print("Inside fetchOPConfiguration :: opConfiguration :: \(configuration.toString)")
                    
                    self?.handleFetchOPConfiguration(configuration: configuration, scopeText: scopeText)
                case .failure(let error):
                    print("error: \(error)")
                    self?.presenter.onError(message: error.localizedDescription)
                    self?.presenter.onLoading(visible: false)
                }
            }
            .store(in: &cancellableSet)
    }
    
    private func handleFetchOPConfiguration(configuration: OPConfiguration, scopeText: String) {
        if configuration.isSuccessful {
            // Save configuration into local DB
            RealmManager.shared.deleteAllConfiguration()
            RealmManager.shared.save(object: configuration)
            doDCR(scopeText: scopeText)
        } else {
            presenter.onError(message: "Error with fetching OPConfiguration")
            presenter.onLoading(visible: false)
        }
    }
    
    private func doDCR(scopeText: String) {
        dcrRepository.doDCR(scopeText: scopeText) { [weak self] result, error in
            self?.presenter.onLoading(visible: false)
            guard let error = error else {
                if result {
                    self?.presenter.onMainStateChanged(viewState: .login)
                }
                return
            }
            
            self?.presenter.onError(message: error.localizedDescription)
            self?.presenter.onLoading(visible: false)
        }
    }
}
