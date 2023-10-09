//
//  RegisterViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

final class RegisterViewState: ObservableObject {
    
    @State var issuer = ""
    @State var scopes = "openid"
    @State var loadingVisible = false
}
