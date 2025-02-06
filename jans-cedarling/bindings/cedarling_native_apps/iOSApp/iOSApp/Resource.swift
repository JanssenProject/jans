//
//  Resource.swift
//  iOSApp
//
//  Created by Arnab Dutta on 05/02/25.
//

import Foundation

struct Resource: Codable {
    var payload: [String: String] = ["": ""]
    var resource_type: String = ""
    var resource_id: String = ""
    
    init () {
        if let resource = loadJSON(filename: "resource", type: Resource.self) {
            self.payload = resource.payload
            self.resource_type = resource.resource_type
            self.resource_id = resource.resource_id
            
        }
    }
    
    public func toString() -> String {
        do {
            let resourceData = try JSONEncoder().encode(self)
            if let resourceString = String(data: resourceData, encoding: .utf8){
                return resourceString;
            }
            return ""
        } catch {
            print("❌ Error in strinfigy resource object")
            return ""
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
