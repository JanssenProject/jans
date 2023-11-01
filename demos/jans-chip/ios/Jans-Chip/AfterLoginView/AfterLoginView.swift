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
            Text("User Login Information:")
            VStack(alignment: .leading) {
                Text("sub: " + state.sub).padding()
                state.name.flatMap {
                    Text("name: " + $0).padding()
                }
                state.nickname.flatMap {
                    Text("nickname: " + $0).padding()
                }
                state.givenName.flatMap {
                    Text("givenName: " + $0).padding()
                }
                state.middleName.flatMap {
                    Text("middleName: " + $0).padding()
                }
                state.inum.flatMap {
                    Text("inum: " + $0).padding()
                }
                state.familyName.flatMap {
                    Text("familyName: " + $0).padding()
                }
                state.jansAdminUIRole.flatMap {
                    Text("jansAdminUIRole: " + $0).padding()
                }
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
            state: AfterLoginViewState(userInfo: UserInfo(sub: "sub: ")),
            interactor:
                AfterLoginViewInteractorImpl(
                    presenter: AfterLoginViewPresenterImpl(
                        state: AfterLoginViewState(userInfo: UserInfo(sub: "")),
                        mainViewState: MainViewState()
                    )
                )
        )
    }
}
