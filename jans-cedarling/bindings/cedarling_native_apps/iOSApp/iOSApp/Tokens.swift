//
//  Request.swift
//  iOSApp
//
//  Created by Arnab Dutta on 05/02/25.
//

import Foundation

struct Tokens: Codable {
    var accessToken: String = ""
    var idToken: String = ""
    var userInfoToken: String = ""
    
    init () {
        if let tokens = loadJSON(filename: "tokens", type: Tokens.self) {
            self.accessToken = tokens.accessToken
            self.idToken = tokens.idToken
            self.userInfoToken = tokens.userInfoToken
            
        }
    }
    
    func loadJSON<T: Decodable>(filename: String, type: T.Type) -> T? {
        guard let url = Bundle.main.url(forResource: filename, withExtension: "json") else {
            print("❌ JSON file not found in bundle")
            return nil
        }
        
        do {
            let data = try Data(contentsOf: url)
            let decoder = JSONDecoder()
            return try decoder.decode(T.self, from: data)
        } catch {
            print("❌ Error decoding JSON: \(error)")
            return nil
        }
    }
}
