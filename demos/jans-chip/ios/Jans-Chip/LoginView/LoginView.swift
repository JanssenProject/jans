//
//  LoginView.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct LoginView: View {
    
    @ObservedObject
    private var state: LoginViewState

    private let interactor: LoginViewInteractor

    init(state: LoginViewState, interactor: LoginViewInteractor) {
        self.state = state
        self.interactor = interactor
    }
    
    var body: some View {
        VStack {
            Image("janssen_logo")
                .resizable()
                .scaledToFit()
                .frame(width: 250, height: 100)
            Text("User Login")
            TextField("User name", text: $state.userName)
                .textFieldStyle(.roundedBorder)
            SecureField("Password", text: $state.password)
                .textFieldStyle(.roundedBorder)
            JansButton(title: "Login",
                       disabled: state.loadingVisible,
                       backgroundColor: Color.cyan) {
                interactor.onLoginClick(username: state.userName, password: state.password)
            }.padding(.top)
            if state.loadingVisible {
                ProgressView()
            }
            Spacer()
            HStack(spacing: 24) {
                Text("Need Enrol?")
                JansButton(title: "Enroll",
                           disabled: false,
                           backgroundColor: Color.green) {
                    interactor.goToEnrol()
                }
            }
        }
        .frame(width: 250, height: 500)
        .onAppear {
            interactor.onAppear()
        }
    }
}

struct LoginView_Previews: PreviewProvider {

    static var previews: some View {
        LoginView(
            state: LoginViewState(),
            interactor:
                LoginViewInteractorImpl(
                    presenter: LoginViewPresenterImpl(
                        state: LoginViewState(),
                        mainViewState: MainViewState()
                    )
                )
        )
    }
}
