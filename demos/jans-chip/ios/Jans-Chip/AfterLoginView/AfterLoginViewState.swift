//
//  AfterLoginViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

protocol AfterLoginViewProtocol {
    
    var name: String { get }
    var userInfo: [UserAdditionalInfo] { get }
}

final class AfterLoginViewState: AfterLoginViewProtocol {
    
    var userInfo: [UserAdditionalInfo] = []
    
    var name: String {
        AuthAdaptor().getAllCredentials()?.first?.userDisplayName ?? ""
    }
    
    init(userInfo: [String: String]) {
        self.userInfo = userInfo.map { UserAdditionalInfo(key: $0.key, value: $0.value) }
    }
}
