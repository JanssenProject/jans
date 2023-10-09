//
//  LoginViewAssembler.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct LoginViewAssembler {

    static func assembleNavigationWrapped() -> LoginView {

        let state = LoginViewState()
        let presenter = LoginViewPresenterImpl(state: state)
        let interactor = LoginViewInteractorImpl(presenter: presenter)
        let view = LoginView(state: state, interactor: interactor)
        
        return view
    }
}
