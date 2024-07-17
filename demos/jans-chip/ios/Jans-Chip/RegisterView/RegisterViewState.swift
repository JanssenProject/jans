//
//  RegisterViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

final class RegisterViewState: ObservableObject {
    
    @Published var issuer = "https://admin-ui-test.gluu.org"
    @Published var loadingVisible = false
    
    let checkListData:[CheckListItem] = [
        CheckListItem(isChecked: true, title: "openid"),
        CheckListItem(isChecked: true, title: "authorization_challenge"),
        CheckListItem(isChecked: false, title: "profile")
    ]
    
    var scopes: String {
        scopeArray.joined(separator: " ")
    }
    
    init() {
        checkListData.forEach {
            if $0.isChecked {
                scopeArray.insert($0.title)
            }
        }
    }
    
    private var scopeArray: Set<String> = []
    
    func scopesChanged(scope: String, insert: Bool) {
        if insert {
            scopeArray.insert(scope)
        } else {
            scopeArray.remove(scope)
        }
    }
}
