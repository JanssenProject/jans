//
//  RegisterViewAssembler.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct RegisterViewAssembler {

    static func assembleNavigationWrapped() -> RegisterView {

        let state = RegisterViewState()
        let presenter = RegisterViewPresenterImpl(state: state)
        let interactor = RegisterViewInteractorImpl(presenter: presenter)
        let view = RegisterView(state: state, interactor: interactor)
//        let hosting = UIHostingController(rootView: view)
//        hosting.hidesBottomBarWhenPushed = true
//
//        return hosting
        
        return view
    }
}
