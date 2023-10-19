//
//  MainViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation
import RealmSwift

protocol MainViewInteractor: AnyObject {
    
    func onAppear()
}

final class MainViewInteractorImpl: MainViewInteractor {
    
    private let presenter: MainViewPresenterImpl
    
    private let integrityRepository = PlayIntegrityRepository()
    
    @ObservedResults(OIDCClient.self) var oidcClient
    
    init(presenter: MainViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onAppear() {
        checkAppIntegrity()
        
        let viewState: ViewState = oidcClient.isEmpty ? .register : .login
        presenter.onViewStateChanged(viewState: viewState)
    }
    
    // MARK: - Private part
    private func checkAppIntegrity() {
        let appIntegrity: AppIntegrityEntity? = RealmManager.shared.getObject()
        if appIntegrity == nil {
            RealmManager.shared.deleteAllAppIntegrity()
            integrityRepository.checkAppIntegrity()
        }
    }
}
