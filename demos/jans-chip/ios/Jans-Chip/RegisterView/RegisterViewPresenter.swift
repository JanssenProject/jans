//
//  RegisterViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol RegisterViewPresenter: AnyObject {
    
    func onError(message: String)
    func onMainStateChanged(viewState: ViewState)
    func onLoading(visible: Bool)
}

final class RegisterViewPresenterImpl: RegisterViewPresenter {
    
    private let state: RegisterViewState
    private let mainViewState: MainViewState
    
    init(state: RegisterViewState, mainViewState: MainViewState) {
        self.state = state
        self.mainViewState = mainViewState
    }
    
    func onError(message: String) {
        UIApplication.showAlert(message: message)
    }
    
    func onMainStateChanged(viewState: ViewState) {
        mainViewState.viewState = viewState
    }
    
    func onLoading(visible: Bool) {
        state.loadingVisible = visible
    }
}
