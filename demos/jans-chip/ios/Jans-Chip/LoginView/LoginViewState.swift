//
//  LoginViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

final class LoginViewState: ObservableObject {
    
    var issuer = "https://admin-ui-test.gluu.org"
    var loadingVisible = false
    
    private let authAdapter = AuthAdaptor()
    
    let checkListData:[CheckListItem] = [
        CheckListItem(isChecked: true, title: "openid"),
        CheckListItem(isChecked: true, title: "authorization_challenge"),
        CheckListItem(isChecked: true, title: "profile")
    ]
    
    var scopes: String {
        scopeArray.joined(separator: " ")
    }
    
    var creds: [PublicKeyCredentialSource] {
        authAdapter.getAllCredentials() ?? []
    }
    
    var passkeyList: [PublicKeyCredentialRow] {
        var list: [PublicKeyCredentialRow] = []
        creds.forEach {
            list.append(PublicKeyCredentialRow(publicKeyCredential: $0))
        }
        
        return list
    }
    
    var titleText: String {
        passkeyList.isEmpty ? "No passkey enrolled" : "Choose your passkey"
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
