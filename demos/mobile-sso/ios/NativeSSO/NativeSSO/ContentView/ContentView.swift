//
//  ContentView.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/21/22.
//

import SwiftUI
import BetterSafariView

struct ContentView: View {

    @ObservedObject
    private var state: ContentViewState

    private let interactor: ContentViewInteractor

    init(state: ContentViewState = ContentViewState()) {
        self.state = state
        
        let presenter: ContentViewPresenter = ContentViewPresenterImpl(state: state)
        interactor = ContentViewInteractorImpl(presenter: presenter)
    }

    var body: some View {
        VStack {
            Text("Native SSO")

            VStack(alignment: .leading) {
                GluuTextField(title: "Enter host url", text: state.hostUrl, placeholder: "Enter host url") {
                    interactor.onHostUrlTextChanged(text: $0)
                }
                Spacer()
            }
            .padding()

            Button("Login") {
                interactor.onLoginClick()
            }
            .alert(state.errorText, isPresented: $state.showingAlert) {
                Button("OK", role: .cancel) { state.showingAlert = false }
            }

            VStack(alignment: .leading) {
                Text(state.resultText)
                    .multilineTextAlignment(.leading)
                Spacer()
            }
            .padding()

            Spacer()
            .padding()
        }
        .sheet(isPresented: $state.showingWebView) {
            state.authorizeWebViewURL.flatMap {
                Webview(url: $0)
            }
        }
        .webAuthenticationSession(isPresented: $state.showingWebView) {
            WebAuthenticationSession(
                url: state.authorizeWebViewURL!,
                callbackURLScheme: "iOSNativeSSO"
            ) { callbackURL, error in
                interactor.onAuthentication(authenticationURL: callbackURL, error: error)
            }
            .prefersEphemeralWebBrowserSession(false)
        }
        .onAppear {
            interactor.onAppear(url: state.hostUrl)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
