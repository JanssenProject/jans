//
//  AfterLoginViewState.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 03.10.2023.
//

import SwiftUI

protocol AfterLoginViewProtocol {
    
    var sub: String { get }
    var name: String? { get }
    var nickname: String? { get }
    var givenName: String? { get }
    var middleName: String? { get }
    var inum: String? { get }
    var familyName: String? { get }
    var jansAdminUIRole: String? { get }
}

final class AfterLoginViewState: AfterLoginViewProtocol {
    
    private let userInfo: UserInfo
    
    init(userInfo: UserInfo) {
        self.userInfo = userInfo
    }
    
    var sub: String { userInfo.sub }
    var name: String? { userInfo.name }
    var nickname: String? { userInfo.nickname }
    var givenName: String? { userInfo.givenName }
    var middleName: String? { userInfo.middleName }
    var inum: String? { userInfo.inum }
    var familyName: String? { userInfo.familyName }
    var jansAdminUIRole: String? { userInfo.jansAdminUIRole?.description }
}
