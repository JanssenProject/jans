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
            case .afterLogin:
                Text("No developed yet")
            }
        }
        .onAppear {
            interactor.onAppear()
        }
    }
}

struct MainView_Previews: PreviewProvider {

    static var previews: some View {
        MainView(
            state: MainViewState(),
            interactor: MainViewInteractorImpl(presenter: MainViewPresenterImpl(state: MainViewState()))
        )
    }
}
