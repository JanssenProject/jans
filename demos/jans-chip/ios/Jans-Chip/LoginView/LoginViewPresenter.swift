//
//  LoginViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol LoginViewPresenter: AnyObject {
    
    func onError(message: String)
    func onMainStateChanged(viewState: ViewState)
    func onLoading(visible: Bool)
    func onReloadList()
    func showAlert(message: String, onAction: @escaping (AlertAction) -> Void)
}

final class LoginViewPresenterImpl: LoginViewPresenter {
    
    private let state: LoginViewState
    private let mainViewState: MainViewState
    
    init(state: LoginViewState, mainViewState: MainViewState) {
        self.state = state
        self.mainViewState = mainViewState
    }
    
    func onError(message: String) {
        DispatchQueue.main.async { [weak self] in
            self?.state.loadingVisible = false
            UIApplication.showAlert(message: message)
        }
    }
    
    func onMainStateChanged(viewState: ViewState) {
        mainViewState.viewState = viewState
    }
    
    func onLoading(visible: Bool) {
        state.loadingVisible = visible
    }
    
    func showAlert(message: String, onAction: @escaping (AlertAction) -> Void) {
        DispatchQueue.main.async { [weak self] in
            self?.state.loadingVisible = false
            UIApplication.showAlert(message: message, onAction: onAction)
        }
    }
    
    func onReloadList() {
        state.loadPasskeyList()
    }
}
