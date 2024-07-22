//
//  AfterLoginViewAssembler.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

struct AfterLoginViewAssembler {
    
    static func assembleNavigationWrapped(mainViewState: MainViewState, userInfo: UserInfo) -> AfterLoginView {

        let state = AfterLoginViewState(userInfo: userInfo.additionalInfo)
        let presenter = AfterLoginViewPresenterImpl(mainViewState: mainViewState)
        let interactor = AfterLoginViewInteractorImpl(presenter: presenter)
        let view = AfterLoginView(state: state, interactor: interactor)
        
        return view
    }
}
