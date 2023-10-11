//
//  RegisterViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol RegisterViewPresenter: AnyObject {
    
    func onError(message: String)
}

final class RegisterViewPresenterImpl: RegisterViewPresenter {
    
    private let state: RegisterViewState
    
    init(state: RegisterViewState) {
        self.state = state
    }
    
    func onError(message: String) {
        UIApplication.showAlert(message: message)
    }
}
