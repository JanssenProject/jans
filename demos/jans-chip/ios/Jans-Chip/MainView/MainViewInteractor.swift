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
    
    @ObservedResults(OIDCClient.self) var oidcClient
    
    init(presenter: MainViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onAppear() {
        let viewState: ViewState = oidcClient.isEmpty ? .register : .login
        presenter.onViewStateChanged(viewState: viewState)
    }
}
