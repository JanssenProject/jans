//
//  LoginViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol LoginViewPresenter: AnyObject {
    
    func login()
}

final class LoginViewPresenterImpl: LoginViewPresenter {
    
    private let state: LoginViewState
    
    init(state: LoginViewState) {
        self.state = state
    }
    
    func login() {
        
    }
}
