//
//  RegisterViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

final class RegisterViewState: ObservableObject {
    
    @Published var issuer = "https://duttarnab-coherent-imp.gluu.info"
    @Published var scopes = "openid, authorization_challenge"
    @State var loadingVisible = false
}
