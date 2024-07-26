//
//  AttestationResultRequest.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 16.07.2024.
//

import Foundation

public struct AttestationResultRequest: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case id
        case type
        case response
    }
    
    var id: String?
    var type: String?
    var response: AttestationDataResponse
}
