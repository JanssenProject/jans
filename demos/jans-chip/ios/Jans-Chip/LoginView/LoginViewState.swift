//
//  LoginViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

final class LoginViewState: ObservableObject {
    
    @State var userName = "admin"
    @State var password = "Gluu1234."
    @Published var loadingVisible = false
}
