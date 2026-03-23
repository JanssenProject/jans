// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

import Foundation

enum TokenError: Error {
    case fileNotFound
    case dataCorrupted
}
class Helper{
    /// Loads and decodes a JSON file from the main bundle into the specified `Decodable` type.
    /// - Parameters:
    ///   - filename: The base name of the JSON file in the main bundle (without the ".json" extension).
    ///   - type: The `Decodable` type to decode the file into.
    /// - Returns: An instance of `T` decoded from the file, or `nil` if the file is not found or decoding fails.
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
    
    /// Loads and parses a JSON file from the main bundle into a dictionary.
    /// - Parameters:
    ///   - filename: The resource name of the JSON file (without the `.json` extension).
    /// - Returns: A `[String: Any]` containing the parsed JSON, or `["": ""]` if the file is missing or cannot be read or decoded.
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
    
    /// Loads an array of `TokenMapping` from a JSON file in the main app bundle.
    /// - Parameter fileName: The resource filename (without the ".json" extension) to load.
    /// - Returns: An array of `TokenMapping` decoded from the file; returns an empty array if the file is missing or cannot be read or decoded.
    static func loadTokens(fromFileName fileName: String) -> [TokenMapping] {
        // 1. Locate the file in the App Bundle
        guard let url = Bundle.main.url(forResource: fileName, withExtension: "json") else {
            print("Token file not found: \(fileName).json")
            return []
        }

        do {
            // 2. Read the raw data from the file
            let data = try Data(contentsOf: url)

            // 3. Decode the JSON into our array of structs
            let decoder = JSONDecoder()
            let tokens = try decoder.decode([TokenMapping].self, from: data)

            return tokens

        } catch {
            print("Failed to load tokens from \(fileName).json: \(error)")
            return []
        }
    }

    /// Loads a JSON file from the main bundle and returns its contents as a `[String: String]` dictionary.
    /// - Parameters:
    ///   - filename: The resource name of the JSON file (without the `.json` extension).
    /// - Returns: A dictionary parsed from the JSON file, or `["": ""]` if the file is missing or cannot be read/decoded.
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
    
    static func toJSONString<T: Codable>(_ object: T, prettyPrinted: Bool = true) -> String? {
        let encoder = JSONEncoder()
        if prettyPrinted {
            encoder.outputFormatting = .prettyPrinted
        }

        do {
            let data = try encoder.encode(object)
            return String(data: data, encoding: .utf8)
        } catch {
            print("❌ Failed to encode object to JSON: \(error)")
            return nil
        }
    }
    
    static func processLogs(logs: [String]) -> String {
        var combinedLogs = logs.joined(separator: ", ")
        combinedLogs = "[\(combinedLogs)]"
        return combinedLogs
    }
    
    /// Removes newline and carriage return characters from the given string.
    /// - Parameter text: The input string to clean.
    /// - Returns: The input string with all `\n` (line feed) and `\r` (carriage return) characters removed.
    static func removeNewLines(from text: String) -> String {
        return text.replacingOccurrences(of: "\n", with: "").replacingOccurrences(of: "\r", with: "")
    }
    
    /// Loads a ZIP resource from the main bundle and returns its raw bytes.
    /// 
    /// The `fileName` may include or omit the `.zip` extension; the method will look up the resource in `Bundle.main`, read its contents, and return them as `[UInt8]`.
    /// - Parameter fileName: The resource name or filename to load (with or without extension).
    /// - Returns: A `[UInt8]` containing the file data, or `nil` if the resource is not found or cannot be read.
    static func zipToBytes(fileName: String) -> [UInt8]? {
        // 1. Convert String filename to URL from the Bundle
        // This handles cases where the user includes ".zip" or leaves it off
        let name = (fileName as NSString).deletingPathExtension
        let extensionName = (fileName as NSString).pathExtension.isEmpty ? "zip" : (fileName as NSString).pathExtension
        
        guard let fileURL = Bundle.main.url(forResource: name, withExtension: extensionName) else {
            print("File not found in bundle.")
            return nil
        }

        // 2. Load and convert
        do {
            let data = try Data(contentsOf: fileURL)
            return [UInt8](data)
        } catch {
            print("Error reading data: \(error)")
            return nil
        }
    }
}
