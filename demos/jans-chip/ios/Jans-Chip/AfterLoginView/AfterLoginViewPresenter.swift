//
//  AfterLoginViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol AfterLoginViewPresenter {
    
    func onViewStateChanged(viewState: ViewState)
    func onError(message: String)
}

final class AfterLoginViewPresenterImpl: AfterLoginViewPresenter {
    
    private let mainViewState: MainViewState
    
    init(mainViewState: MainViewState) {
        self.mainViewState = mainViewState
    }
    
    func onViewStateChanged(viewState: ViewState) {
        mainViewState.viewState = viewState
    }
    
    func onError(message: String) {
        UIApplication.showAlert(message: message)
    }
}
