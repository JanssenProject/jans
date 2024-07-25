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
    private var oidcClient: OIDCClient?
    private var appIntegrity: AppIntegrityEntity?
    
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
                    oidcClient = try await fetchOIDCClient()
                    
                    // 4. Check application integrity
                    appIntegrity = try await checkAppIntegrity()
                    
                    // 5. Show the UI
                    DispatchQueue.main.async { [weak self] in
                        self?.presenter.onViewStateChanged(viewState: .register)
                    }
                } catch {
                    print("Request failed with error: \(error)")
                }
            }
        }
    }
}

private extension MainViewInteractorImpl {
    
    func fetchOPConfiguration(issuer: String) async throws -> OPConfiguration? {
        if let configuration: OPConfiguration = RealmManager.shared.getObject() {
            return configuration
        }
        let opConfiguration: OPConfiguration? = await withCheckedContinuation { continuation in
            serviceClient.getOPConfiguration(url: issuer + AppConfig.OP_CONFIG_URL)
                .sink { result in
                    if let error = result.error {
                        print("error: \(error)")
                        continuation.resume(returning: nil)
                    } else if let configurationResult = result.value {
                        RealmManager.shared.save(object: configurationResult)
                        continuation.resume(returning: configurationResult)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return opConfiguration
    }
    
    func fetchFidoConfiguration(issuer: String) async throws -> FidoConfiguration? {
        if let fidoConfiguration: FidoConfiguration = RealmManager.shared.getObject() {
            return fidoConfiguration
        }
        let fidoConfiguration: FidoConfiguration? = await withCheckedContinuation { continuation in
            serviceClient.getFidoConfiguration(url: issuer + AppConfig.FIDO_CONFIG_URL)
                .sink { result in
                    if let error = result.error {
                        print("error: \(error)")
                        continuation.resume(returning: nil)
                    } else if let configurationResult = result.value {
                        RealmManager.shared.save(object: configurationResult)
                        continuation.resume(returning: configurationResult)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return fidoConfiguration
    }
    
    func fetchOIDCClient() async throws -> OIDCClient? {
        if let oidcClient: OIDCClient = RealmManager.shared.getObject() {
            return oidcClient
        }
        
        guard let configuration: OPConfiguration = RealmManager.shared.getObject(), let dcRequest = DCRRepository().makeDCRRequestUsingSSA(configuration: configuration, ssa: AppConfig.SSA, scopeText: AppConfig.ALLOWED_REGISTRATION_SCOPES) else {
            return nil
        }
        
        let oidcClient: OIDCClient? = await withCheckedContinuation { continuation in
            serviceClient.doDCR(dcRequest: dcRequest, url: configuration.registrationEndpoint)
                .sink { result in
                    if let error = result.error {
                        print("Error in DCR.\n Error:  \(error)")
                        continuation.resume(returning: nil)
                    } else if let dcResponse = result.value {
                        print("Inside doDCR :: DCResponse :: \(dcResponse)")
                        let oidcClient = OIDCClient()
                        oidcClient.clientName = dcResponse.clientName ?? ""
                        oidcClient.clientId = dcResponse.clientId ?? ""
                        oidcClient.clientSecret = dcResponse.clientSecret  ?? ""
                        oidcClient.scope = AppConfig.ALLOWED_REGISTRATION_SCOPES.split(separator: ",").joined(separator: " ")
                        RealmManager.shared.save(object: oidcClient)
                        
                        continuation.resume(returning: oidcClient)
                    }
                }
                .store(in: &cancellableSet)
        }
        
        return oidcClient
    }
    
    func checkAppIntegrity() async throws -> AppIntegrityEntity? {
        if let appIntegrity: AppIntegrityEntity = RealmManager.shared.getObject() {
            return appIntegrity
        }
        
        RealmManager.shared.deleteAllAppIntegrity()
        integrityRepository.checkAppIntegrity()
        
        return nil
    }
}
