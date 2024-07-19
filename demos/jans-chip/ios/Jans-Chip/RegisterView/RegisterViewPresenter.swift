//
//  RegisterViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol RegisterViewPresenter: AnyObject {
    
    func onViewStateChanged(viewState: ViewState)
    func onError(message: String)
    func onLoading(visible: Bool)
}

final class RegisterViewPresenterImpl: RegisterViewPresenter {
    
    private let state: RegisterViewState
    private let mainViewState: MainViewState
    
    init(state: RegisterViewState, mainViewState: MainViewState) {
        self.state = state
        self.mainViewState = mainViewState
    }
    
    func onViewStateChanged(viewState: ViewState) {
        mainViewState.viewState = viewState
    }
    
    func onError(message: String) {
        onLoading(visible: false)
        DispatchQueue.main.async {
            UIApplication.showAlert(message: message)
        }
    }
    
    func onLoading(visible: Bool) {
        state.loadingVisible = visible
    }
}
