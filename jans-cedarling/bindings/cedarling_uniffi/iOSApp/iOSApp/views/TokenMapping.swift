//
//  TokenMapping.swift
//  iOSApp
//
//  Created by Arnab Dutta on 23/03/26.
//

import Foundation
struct TokenMapping: Codable, Identifiable {
    let mapping: String
    let payload: String

    var id: String { mapping }
    
    public func toTokenInput() -> TokenInput {
        return TokenInput(
            mapping: self.mapping,
            payload: self.payload
        )
    }
}
