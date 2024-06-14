//
//  LoginViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol LoginViewPresenter: AnyObject {
    
    func onViewStateChanged(viewState: ViewState)
    func onError(message: String)
    func onLoading(visible: Bool)
}

final class LoginViewPresenterImpl: LoginViewPresenter {
    
    private let state: LoginViewState
    private let mainViewState: MainViewState
    
    init(state: LoginViewState, mainViewState: MainViewState) {
        self.state = state
        self.mainViewState = mainViewState
    }
    
    func onViewStateChanged(viewState: ViewState) {
        mainViewState.viewState = viewState
    }
    
    func onError(message: String) {
        UIApplication.showAlert(message: message)
    }
    
    func onLoading(visible: Bool) {
        state.loadingVisible = visible
    }
}
