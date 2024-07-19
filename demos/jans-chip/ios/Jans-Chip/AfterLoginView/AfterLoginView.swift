//
//  AfterLoginView.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct AfterLoginView: View {
    
    private var state: AfterLoginViewState

    private let interactor: AfterLoginViewInteractor

    init(state: AfterLoginViewState, interactor: AfterLoginViewInteractor) {
        self.state = state
        self.interactor = interactor
    }
    
    var body: some View {
        VStack(alignment: .leading) {
            Image("janssen_logo")
                .resizable()
                .scaledToFit()
                .frame(width: 250, height: 100)
            Text("Welcome " + state.name)
            VStack(alignment: .leading) {
                List(state.userInfo) { info in
                    Text("\(info.key): " + info.value).padding()
                }
                .listStyle(.plain)
            }
            .border(.gray, width: 2)
            Spacer()
            JansButton(title: "Logout",
                       backgroundColor: Color.cyan) {
                interactor.onLogoutClick()
            }.padding(.top)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
        .onAppear {
            interactor.onAppear()
        }
    }
}

struct AfterLoginView_Previews: PreviewProvider {

    static var previews: some View {
        AfterLoginView(
            state: AfterLoginViewState(userInfo: [:]),
            interactor:
                AfterLoginViewInteractorImpl(
                    presenter: AfterLoginViewPresenterImpl(
                        state: AfterLoginViewState(userInfo: [:]),
                        mainViewState: MainViewState()
                    )
                )
        )
    }
}
