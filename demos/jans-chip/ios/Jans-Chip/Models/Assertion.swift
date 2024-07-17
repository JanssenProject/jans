//
//  Assertion.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 08.07.2024.
//

import Foundation
import RealmSwift

final class Assertion: Object, Codable, ObjectKeyIdentifiable {
    
    private enum CodingKeys: String, CodingKey {
        case basePath = "base_path"
        case optionsEndpoint = "options_endpoint"
        case optionsGenerateEndpoint = "options_generate_endpoint"
        case resultEndpoint = "result_endpoint"
    }
    
    @Persisted var basePath: String?
    @Persisted var optionsEndpoint: String?
    @Persisted var optionsGenerateEndpoint: String?
    @Persisted var resultEndpoint: String?
}
