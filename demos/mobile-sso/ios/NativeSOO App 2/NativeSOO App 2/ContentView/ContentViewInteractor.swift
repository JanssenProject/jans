//
//  ContentViewInteractor.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/21/22.
//

import UIKit

protocol ContentViewInteractor: AnyObject {

    func onAppear(url: String)
    func onHostUrlTextChanged(text: String)
    func onLoginClick()
    func onShowAlert(message: String)
    func onAuthentication(authenticationURL: URL?, error: Error?)
}

final class ContentViewInteractorImpl: ContentViewInteractor {

    // MARK: Private

    private let presenter: ContentViewPresenter

    private let networkManager = NetworkManager.shared

    private var host: String = "" {
        didSet {
            presenter.handle(host: host)
        }
    }

    // MARK: Init

    init(presenter: ContentViewPresenter) {
        self.presenter = presenter
    }

    // MARK: - ContentViewInteractor implementation

    func onAppear(url: String) {
        host = url
    }

    func onHostUrlTextChanged(text: String) {
        host = text
    }

    func onLoginClick() {
        if validate() {
            // Step 1. Register, it needed only once
            networkManager.register { [weak self] client_id, _ in
                print("Client_id - \(client_id)")

                self?.networkManager.getToken { [weak self] token in
                    // step 3(4) get user info
                    KeychainManager.shared.token = token
                    self?.networkManager.getUserInfo(token: token) { userInfo in
                        self?.presenter.handle(userInfo: userInfo)
                    }
                }
            }

            return
        }

        onShowAlert(message: "Url is empty or invalid")
    }

    func onShowAlert(message: String) {
        presenter.showAlert(message: message)
    }

    func onAuthentication(authenticationURL: URL?, error: Error?) {
        if let error = error {
            onShowAlert(message: error.localizedDescription)
            return
        }

        guard let authenticationURL = authenticationURL else {
            return
        }

        var components = URLComponents()
        components.query = authenticationURL.fragment
        if let queryItems = components.queryItems {
            if let codeQuery = queryItems.first(where: { $0.name == "code" }), let code = codeQuery.value {
                // step 2(3) get TOKEN by calling restv1/token
                networkManager.getToken(code: code) { [weak self] token in
                    // step 3(4) get user info
                    KeychainManager.shared.token = token
                    self?.networkManager.getUserInfo(token: token) { userInfo in
                        self?.presenter.handle(userInfo: userInfo)
                    }
                }
            }
        }
    }

    // MARK: - Private

    private func validate() -> Bool {
        !host.isEmpty || verifyUrl(urlString: host)
    }

    private func verifyUrl(urlString: String) -> Bool {
        guard let url = URL(string: urlString) else {
            return false
        }

        return UIApplication.shared.canOpenURL(url)
    }
}
