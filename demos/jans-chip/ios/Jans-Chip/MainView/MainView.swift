//
//  MainView.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct MainView: View {
    
    @ObservedObject
    private var state: MainViewState

    private let interactor: MainViewInteractor

    init(state: MainViewState, interactor: MainViewInteractor) {
        self.state = state
        self.interactor = interactor
    }
    
    var body: some View {
        VStack {
            switch state.viewState {
            case .initial:
                Image("janssen_logo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 250, height: 100)
                Text("Loading...")
            case .register:
                RegisterViewAssembler.assembleNavigationWrapped(mainViewState: state)
            case .login:
                LoginViewAssembler.assembleNavigationWrapped(mainViewState: state)
            case .afterLogin(let userInfo):
                AfterLoginViewAssembler.assembleNavigationWrapped(mainViewState: state, userInfo: userInfo)
            }
        }
        .onAppear {
            interactor.onAppear()
        }
    }
}
