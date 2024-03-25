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
            VStack(spacing: 16) {
                Text("Register OIDC Client")
                VStack(alignment: .leading) {
                    Text("Configuration Endpoint:")
                    TextField("Configuration Endpoint", text: $state.issuer)
                        .textFieldStyle(.roundedBorder)
                }
                VStack(alignment: .leading) {
                    Text("Scopes:")
                    CheckListView(
                        checkListData: state.checkListData,
                        onSelection: { value, selected in
                            state.scopesChanged(scope: value.title, insert: selected)
                        })
                    .padding([.leading, .trailing], 0)
                    .border(.gray, width: 2)
                    .cornerRadius(4)
                    .frame(height: 200)
                }
            }
            JansButton(title: "Register",
                       disabled: state.loadingVisible,
                       backgroundColor: Color.cyan) {
                interactor.onRegisterClick(issuer: state.issuer, scope: state.scopes)
            }.padding(.top)
            if state.loadingVisible {
                ProgressView()
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
        .padding()
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
