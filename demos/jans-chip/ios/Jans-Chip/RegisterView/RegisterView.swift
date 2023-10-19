//
//  RegisterView.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct RegisterView: View {
    
    @ObservedObject
    private var state: RegisterViewState

    private let interactor: RegisterViewInteractor

    init(state: RegisterViewState, interactor: RegisterViewInteractor) {
        self.state = state
        self.interactor = interactor
    }
    
    var body: some View {
        VStack {
            Image("janssen_logo")
                .resizable()
                .scaledToFit()
                .frame(width: 250, height: 100)
            Text("Register OIDC Client")
            TextField("Configuration Endpoint", text: $state.issuer)
                .textFieldStyle(.roundedBorder)
            TextField("Scopes", text: $state.scopes)
                .textFieldStyle(.roundedBorder)
            JansButton(title: "Register",
                       backgroundColor: Color.cyan) {
                interactor.onRegisterClick(issuer: state.issuer, scope: state.scopes)
            }.padding(.top)
            if $state.loadingVisible.wrappedValue {
                ProgressView()
            }
            Spacer()
        }
        .frame(width: 250, height: 500)
    }
}

struct RegisterView_Previews: PreviewProvider {

    static var previews: some View {
        RegisterView(
            state: RegisterViewState(),
            interactor: RegisterViewInteractorImpl(
                presenter: RegisterViewPresenterImpl(
                    state: RegisterViewState(),
                    mainViewState: MainViewState()
                )
            )
        )
    }
}
