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
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundColor(.accentColor)
            Text("Hello, world!")
        }
        .padding()
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
