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
    
    /// Creates a `TokenInput` initialized from this `TokenMapping`.
    /// - Returns: A `TokenInput` containing the same `mapping` and `payload` values as this `TokenMapping`.
    public func toTokenInput() -> TokenInput {
        return TokenInput(
            mapping: self.mapping,
            payload: self.payload
        )
    }
}
