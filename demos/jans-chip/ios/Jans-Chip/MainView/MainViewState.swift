//
//  MainViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

enum ViewState {
    case initial, register, login, afterLogin
}

final class MainViewState: ObservableObject {
    
    @Published var viewState: ViewState = .initial
}
