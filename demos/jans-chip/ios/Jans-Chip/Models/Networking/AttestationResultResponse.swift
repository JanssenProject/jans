//
//  AttestationResultResponse.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 16.07.2024.
//

import Foundation

public struct AttestationResultResponse: Codable {
    
    var isSuccessful: Bool = false
    var errorMessage: String?
}
