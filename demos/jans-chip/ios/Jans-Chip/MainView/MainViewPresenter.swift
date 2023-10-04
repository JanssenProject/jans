//
//  MainViewPresenter.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import UIKit

protocol MainViewPresenter: AnyObject {
    
    func login()
}

final class MainViewPresenterImpl: MainViewPresenter {
    
    private let state: MainViewState
    
    init(state: MainViewState) {
        self.state = state
    }
    
    func login() {
        
    }
}
