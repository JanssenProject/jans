//
//  MainViewAssembler.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct MainViewAssembler {

    static func assembleNavigationWrapped() -> MainView {

        let state = MainViewState()
        let presenter = MainViewPresenterImpl(state: state)
        let interactor = MainViewInteractorImpl(presenter: presenter)
        let view = MainView(state: state, interactor: interactor)
//        let hosting = UIHostingController(rootView: view)
//        hosting.hidesBottomBarWhenPushed = true
//
//        return hosting
        
        return view
    }
}
