//
//  MainViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import RealmSwift
import Combine

protocol MainViewInteractor: AnyObject {
    
    func onAppear()
}

final class MainViewInteractorImpl: MainViewInteractor {
    
    private let presenter: MainViewPresenterImpl
    
    private let integrityRepository = PlayIntegrityRepository()
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    private var cancellableSet : Set<AnyCancellable> = []
    
    private var configuration: OPConfiguration?
    private var fidoConfiguration: FidoConfiguration?
    private var appIntegrity: AppIntegrityEntity?
    
    @ObservedResults(OIDCClient.self) var oidcClient
    
    init(presenter: MainViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onAppear() {
        if let claims: JWTClaimsSet = DPoPProofFactory.shared.getClaimsFromSSA(), let issuer = claims.ISSUER {
            Task {
                do {
                    // 1. get openid configuration
                    configuration = try await fetchOPConfiguration(issuer: issuer)
                    
                    // 2. get FIDO configuration
                    fidoConfiguration = try await fetchFidoConfiguration(issuer: issuer)
                    
                    // 3. check OIDC client
                    let oidcClient: OIDCClient? = try await fetchOIDCClient()
                    
                    // 4. Check application integrity
                    appIntegrity = try await checkAppIntegrity()
                    
                    let viewState: ViewState = oidcClient == nil ? .register : .login
                    presenter.onViewStateChanged(viewState: viewState)
                } catch {
                    print("Request failed with error: \(error)")
                }
            }
        }
    }
    
    // MARK: - Private part
    
    private func fetchOPConfiguration(issuer: String) async throws -> OPConfiguration? {
        if let configuration: OPConfiguration = RealmManager.shared.getObject() {
            return configuration
        }
        let opConfiguration: OPConfiguration? = await withCheckedContinuation { continuation in
            serviceClient.getOPConfiguration(url: issuer + AppConfig.OP_CONFIG_URL)
                .sink { result in
                switch result {
                case .success(let configurationResult):
                    continuation.resume(returning: configurationResult)
                    RealmManager.shared.save(object: configurationResult)
                case .failure(let error):
                    print("error: \(error)")
                    continuation.resume(returning: nil)
                }
            }
            .store(in: &cancellableSet)
        }
        
        return opConfiguration
    }
    
    private func fetchFidoConfiguration(issuer: String) async throws -> FidoConfiguration? {
        if let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject() {
            return fidoConfiguration
        }
        let fidoConfiguration: FidoConfiguration? = await withCheckedContinuation { continuation in
            serviceClient.getFidoConfiguration(url: issuer + AppConfig.FIDO_CONFIG_URL)
                .sink { result in
                switch result {
                case .success(let configurationResult):
                    continuation.resume(returning: configurationResult)
                    RealmManager.shared.save(object: configurationResult)
                case .failure(let error):
                    print("error: \(error)")
                    continuation.resume(returning: nil)
                }
            }
            .store(in: &cancellableSet)
        }
        
        return fidoConfiguration
    }
    
    private func fetchOIDCClient() async throws -> OIDCClient? {
        if let oidcClient: OIDCClient = RealmManager.shared.getObject() {
            return oidcClient
        }
        
        guard let configuration: OPConfiguration = RealmManager.shared.getObject(), let dcRequest = DCRRepository().makeDCRRequestUsingSSA(configuration: configuration, ssa: AppConfig.SSA, scopeText: AppConfig.ALLOWED_REGISTRATION_SCOPES) else {
            return nil
        }
        
        let oidcClient: OIDCClient? = await withCheckedContinuation { continuation in
            serviceClient.doDCR(dcRequest: dcRequest, url: configuration.registrationEndpoint)
                .sink { result in
                    switch result {
                    case .success(let dcResponse):
                        print("Inside doDCR :: DCResponse :: \(dcResponse)")
                        let oidcClient = OIDCClient()
                        oidcClient.clientName = dcResponse.clientName ?? ""
                        oidcClient.clientId = dcResponse.clientId ?? ""
                        oidcClient.clientSecret = dcResponse.clientSecret  ?? ""
                        oidcClient.scope = AppConfig.ALLOWED_REGISTRATION_SCOPES.split(separator: ",").joined(separator: " ")
                        
                        RealmManager.shared.save(object: oidcClient)
                        
                        continuation.resume(returning: oidcClient)
                    case .failure(let error):
                        print("Error in DCR.\n Error:  \(error)")
                        continuation.resume(returning: nil)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return oidcClient
    }
    
    private func checkAppIntegrity() async throws -> AppIntegrityEntity? {
        if let appIntegrity: AppIntegrityEntity = RealmManager.shared.getObject() {
            return appIntegrity
        }
        
        RealmManager.shared.deleteAllAppIntegrity()
        integrityRepository.checkAppIntegrity()
        
        return nil
    }
}
