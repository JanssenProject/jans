//
//  AfterLoginViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol AfterLoginViewPresenter: AnyObject {
    
    func onViewStateChanged(viewState: ViewState)
    func onError(message: String)
}

final class AfterLoginViewPresenterImpl: AfterLoginViewPresenter {
    
    private let state: AfterLoginViewState
    private let mainViewState: MainViewState
    
    init(state: AfterLoginViewState, mainViewState: MainViewState) {
        self.state = state
        self.mainViewState = mainViewState
    }
    
    func onViewStateChanged(viewState: ViewState) {
        mainViewState.viewState = viewState
    }
    
    func onError(message: String) {
        UIApplication.showAlert(message: message)
    }
}
