// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

import Foundation

class Helper{
    static func loadJSON<T: Decodable>(filename: String, type: T.Type) -> T? {
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
    
    static func formatJSON(jsonString: String) -> String {
        // Try to parse and format the JSON
        if let data = jsonString.data(using: .utf8),
           let jsonObject = try? JSONSerialization.jsonObject(with: data, options: .mutableContainers),
           let prettyPrintedData = try? JSONSerialization.data(withJSONObject: jsonObject, options: .prettyPrinted) {
            
            if let prettyPrintedString = String(data: prettyPrintedData, encoding: .utf8) {
                return prettyPrintedString
            }
        } else {
            return "Invalid JSON"
        }
        return ""
    }
    
    static func dictionaryFromFile(filename: String) -> [String: Any] {
           do {
               if let policyStoreUrl = Bundle.main.path(forResource: filename, ofType: "json") {
                   
                   let fileURL = URL(fileURLWithPath: policyStoreUrl)
                   let jsonString = try String(contentsOf: fileURL, encoding: .utf8)
                   guard let jsonData = jsonString.data(using: .utf8) else { return ["": ""] }
                   
                   if let dictionary = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any] {
                       return dictionary
                   }
               }
           } catch {
               print("Error reading or decoding JSON file: \(error)")
           }
        return ["": ""]
       }

    static func dictionaryFromFile(filename: String) -> [String: String] {
           do {
               if let policyStoreUrl = Bundle.main.path(forResource: filename, ofType: "json") {
                   
                   let fileURL = URL(fileURLWithPath: policyStoreUrl)
                   let jsonString = try String(contentsOf: fileURL, encoding: .utf8)
                   guard let jsonData = jsonString.data(using: .utf8) else { return ["": ""] }
                   
                   if let dictionary = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: String] {
                       return dictionary
                   }
               }
           } catch {
               print("Error reading or decoding JSON file: \(error)")
           }
        return ["": ""]
       }
    
    static func dictionaryToString(_ dictionary: [String: Any]) -> String {
            do {
                let jsonData = try JSONSerialization.data(withJSONObject: dictionary, options: .prettyPrinted)
                return String(data: jsonData, encoding: .utf8)!
            } catch {
                print("Error converting dictionary to JSON string: \(error)")
                return ""
            }
        }
}
