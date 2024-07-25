//
//  AfterLoginViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import Combine
import RealmSwift
import Alamofire

protocol AfterLoginViewInteractor: AnyObject {
    
    func onAppear()
    func onLogoutClick()
}

final class AfterLoginViewInteractorImpl: AfterLoginViewInteractor {
    
    private let presenter: AfterLoginViewPresenterImpl
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    init(presenter: AfterLoginViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onAppear() {
        
    }
    
    func onLogoutClick() {
        // Get OPConfiguration and OIDCClient
        guard let oidcClient: OIDCClient = RealmManager.shared.getObject(), let opConfiguration: OPConfiguration = RealmManager.shared.getObject() else {
            return
        }
        
        let authHeaderEncodedString = "\(oidcClient.clientId):\(oidcClient.clientSecret)".data(using: .utf8)?.base64EncodedString() ?? ""
        serviceClient.logout(token: oidcClient.recentGeneratedAccessToken, tokenTypeHint: "access_token", authHeader: "Basic \(authHeaderEncodedString)", url: opConfiguration.revocationEndpoint)
            .sink { [weak self] result in
                if let error = result.error {
                    print("error: \(error)")
//                    switch error {
//                    case .responseSerializationFailed(let reason):
//                        if case .invalidEmptyResponse = reason  {
                            self?.handleSuccessLogout(oidcClient: oidcClient)
//                            return
//                        }
//                        break
//                    default:
//                        break
//                    }
//                    print("error: \(error)")
//                    self?.presenter.onError(message: "Error in logout flow. Erorr: \(error.localizedDescription)")
                } else if let result = result.value {
                    print("logout result: \(result)")
                    self?.handleSuccessLogout(oidcClient: oidcClient)
                }
            }
            .store(in: &cancellableSet)
    }
    
    private func handleSuccessLogout(oidcClient: OIDCClient) {
        RealmManager.shared.updateOIDCClient(oidcCClient: oidcClient, with: "", and: "")
        presenter.onViewStateChanged(viewState: .login)
    }
}
