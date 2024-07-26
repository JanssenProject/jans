//
//  AssertionResultRequest.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 25.07.2024.
//

import Foundation

struct AssertionResultRequest: Codable, ErrorHandler {
    
    var errorMessage: String?
    var isSuccess: Bool = true
    
    private enum CodingKeys: String, CodingKey {
        case id
        case type
        case rawId
    }
    
    var id: String?
    var type: String?
    var rawId: String?
    var response: Response?
}
