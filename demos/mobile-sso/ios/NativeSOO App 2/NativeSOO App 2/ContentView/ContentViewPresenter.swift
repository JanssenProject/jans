//
//  ContentViewPresenter.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/21/22.
//

import Foundation

protocol ContentViewPresenter: AnyObject {

    func handle(host: String)
    func handle(authorizedUrl: URL)
    func handle(userInfo: String)
    func showAlert(message: String)
    func login()
}

final class ContentViewPresenterImpl: ContentViewPresenter {

    private let state: ContentViewState

    init(state: ContentViewState) {
        self.state = state
    }

    func handle(host: String) {
        state.hostUrl = host
    }

    func handle(authorizedUrl: URL) {
        state.showingWebView = true
        state.authorizeWebViewURL = authorizedUrl
    }

    func showAlert(message: String) {
        state.errorText = message
        state.showingAlert = true
    }

    func handle(userInfo: String) {
        state.resultText = userInfo
    }

    func login() {

    }
}
