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
    @State private var selectedPublicKeyCredential: PublicKeyCredentialRow?
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
            VStack(spacing: 16) {
                Text(state.titleText)
                if state.passkeyList.isEmpty == false {
                    VStack(alignment: .leading) {
                        List(state.passkeyList) { publicKeyCredentialRow in
                            PublicKeyCredentialRowView(
                                publicKeyCredentialRow: publicKeyCredentialRow,
                                isSelected: publicKeyCredentialRow.heading == $selectedPublicKeyCredential.wrappedValue?.heading
                            )
                            .onTapGesture {
                                selectedPublicKeyCredential = publicKeyCredentialRow
                            }
                        }
                        .listStyle(.plain)
                        .padding(.horizontal, 0)
                        .border(.gray, width: 2)
                        .cornerRadius(4)
                        .frame(height: 200)
                        .background(.white)
                    }
                    JansButton(title: "Continue",
                               disabled: state.loadingVisible || selectedPublicKeyCredential == nil,
                               backgroundColor: Color.cyan) {
                        interactor.onRegisterClick(
                            username: selectedPublicKeyCredential?.userDisplayName ?? "",
                            password: selectedPublicKeyCredential?.userPassword ?? ""
                        )
                    }.padding(.top)
                }
            }
            if state.loadingVisible {
                ProgressView()
            }
            Spacer()
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text("Don't have an account?")
                Button("Enrol") {
                    interactor.goToEnrol()
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
    }
}
