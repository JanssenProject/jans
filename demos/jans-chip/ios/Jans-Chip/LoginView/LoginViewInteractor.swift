//
//  LoginViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import Combine

protocol LoginViewInteractor: AnyObject {
    
    func onRegisterClick()
}

final class LoginViewInteractorImpl: LoginViewInteractor {
    
    private let presenter: LoginViewPresenterImpl
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    init(presenter: LoginViewPresenterImpl) {
        self.presenter = presenter
    
//        serviceClient.getOPConfiguration()
//            .sink { result in
//                switch result {
//                case .success(let configuration):
//                    print("configuration: \(configuration)")
//                case .failure(let error):
//                    print("error: \(error)")
//                }
//            }
//            .store(in: &cancellableSet)
    }
    
    func onRegisterClick() {}
}
