//
//  SSARegRequest.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 26.07.2024.
//

import Foundation

public struct SSARegRequest: Codable, ErrorHandler {
    
    var errorMessage: String?
    var isSuccess: Bool = true
    
    private enum CodingKeys: String, CodingKey {
        case clientName = "client_name"
        case evidence
        case jwks
        case scope
        case responseTypes = "response_types"
        case grantTypes = "grant_types"
        case ssa = "software_statement"
        case applicationType = "application_type"
        case redirectUris = "redirect_uris"
    }
    
    var clientName: String?
    var evidence: String?
    var jwks: String?
    var scope: String?
    var responseTypes: [String]?
    var grantTypes: [String]?
    var ssa: String?
    var applicationType: String?
    var redirectUris: [String]?
}
