//
//  RegisterViewAssembler.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct RegisterViewAssembler {

    static func assembleNavigationWrapped(mainViewState: MainViewState) -> RegisterView {

        let state = RegisterViewState()
        let presenter = RegisterViewPresenterImpl(state: state, mainViewState: mainViewState)
        let interactor = RegisterViewInteractorImpl(presenter: presenter, mainViewModel: MainViewModelImpl())
        let view = RegisterView(state: state, interactor: interactor)
        
        return view
    }
}
