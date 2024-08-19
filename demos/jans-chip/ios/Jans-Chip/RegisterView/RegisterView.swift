//
//  RegisterView.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI
import Combine

struct RegisterView: View {
    
    @ObservedObject
    private var state: RegisterViewState
    
    @State private var userName: String = ""
    @State private var password: String = ""
    
    private let interactor: RegisterViewInteractor
    
    init(state: RegisterViewState, interactor: RegisterViewInteractor) {
        self.state = state
        self.interactor = interactor
    }
    
    var body: some View {
        VStack(spacing: 24) {
            Image("janssen_logo")
                .resizable()
                .scaledToFit()
                .frame(width: 250, height: 100)
            Text("Enrol Account")
            VStack {
                TextField("User name", text: $userName)
                    .onChange(of: userName) { newValue in
                        userName = newValue
                    }
                    .autocapitalization(.none)
                    .textFieldStyle(.roundedBorder)
                    .frame(height: 50)
                SecureInputView(titleKey: "Password", inputValue: $password)
                    .onChange(of: password) { newValue in
                        password = newValue
                    }
                    .autocapitalization(.none)
                    .textFieldStyle(.roundedBorder)
                    .frame(height: 50)
            }
            JansButton(title: "Enrol",
                       disabled: state.loadingVisible,
                       backgroundColor: Color.cyan) {
                interactor.onLoginClick(username: userName, password: password)
            }.padding(.top)
            if state.loadingVisible {
                ProgressView()
            }
            Spacer()
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text("Already enrolled?")
                Button("Login") {
                    interactor.goToLogin()
                }
            }
        }
        .padding(.horizontal)
    }
}

