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
            TextField("Scopes", text: $state.password)
                .textFieldStyle(.roundedBorder)
            JansButton(title: "Login",
                       backgroundColor: Color.cyan) {
                print("Start login flow")
            }.padding(.top)
            if $state.loadingVisible.wrappedValue {
                ProgressView()
            }
            Spacer()
        }
        .frame(width: 250, height: .infinity)
    }
}

struct LoginView_Previews: PreviewProvider {

    static var previews: some View {
        LoginView(
            state: LoginViewState(),
            interactor:
                LoginViewInteractorImpl(presenter: LoginViewPresenterImpl(state: LoginViewState()))
        )
    }
}
