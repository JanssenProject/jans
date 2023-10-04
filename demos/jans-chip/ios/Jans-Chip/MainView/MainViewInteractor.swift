//
//  MainViewInteractor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import Foundation

protocol MainViewInteractor: AnyObject {
    
    func onRegisterClick()
}

final class MainViewInteractorImpl: MainViewInteractor {
    
    private let presenter: MainViewPresenterImpl
    
    init(presenter: MainViewPresenterImpl) {
        self.presenter = presenter
    }
    
    func onRegisterClick() {}
}
