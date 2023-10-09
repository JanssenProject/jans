//
//  RegisterViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import Combine

protocol RegisterViewInteractor: AnyObject {
    
    func onRegisterClick()
}

final class RegisterViewInteractorImpl: RegisterViewInteractor {
    
    private let presenter: RegisterViewPresenterImpl
    
    private lazy var serviceClient = {
        ServiceClient()
    }()
    
    private var cancellableSet : Set<AnyCancellable> = []
    
    init(presenter: RegisterViewPresenterImpl) {
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
