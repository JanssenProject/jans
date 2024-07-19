//
//  LoginViewAssembler.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct LoginViewAssembler {

    static func assembleNavigationWrapped(mainViewState: MainViewState) -> LoginView {

        let state = LoginViewState()
        let presenter = LoginViewPresenterImpl(state: state, mainViewState: mainViewState)
        
        let mainState = MainViewState()
        let mainPresenter = MainViewPresenterImpl(state: mainState)
        let mainInteractor = MainViewInteractorImpl(presenter: mainPresenter)
        
        let interactor = LoginViewInteractorImpl(presenter: presenter, mainInteractor: mainInteractor)
        let view = LoginView(state: state, interactor: interactor)
        
        return view
    }
}
