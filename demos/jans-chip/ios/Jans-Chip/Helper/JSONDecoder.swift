//
//  JSONDecoder.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 31.07.2024.
//

import Foundation

final class JSONDecoding {
    private let jsonEncoder = JSONEncoder()
    private init() {}
    
    public static let shared = JSONDecoding()
    
    func objectToJSON(object: Codable) -> String? {
        if let jsonData = try? jsonEncoder.encode(object) {
            return String(data: jsonData, encoding: String.Encoding.utf8)
        }
        
        return nil
    }
}
