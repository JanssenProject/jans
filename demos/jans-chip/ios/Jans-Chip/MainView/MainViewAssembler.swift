//
//  MainViewAssembler.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI
import AlamofireNetworkActivityLogger

struct MainViewAssembler {

    static func assembleNavigationWrapped() -> MainView {
        
        mainSetup()

        let state = MainViewState()
        let presenter = MainViewPresenterImpl(state: state)
        let interactor = MainViewInteractorImpl(presenter: presenter)
        let view = MainView(state: state, interactor: interactor)
        
        return view
    }
    
    private static func mainSetup() {
        NetworkActivityLogger.shared.startLogging()
    }
}
