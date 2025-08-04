//
//  DataParserView.swift
//  iOSApp
//
//  Created by Arnab Dutta on 14/02/25.
//

import Foundation
import SwiftUI

struct DataParserView: View {
    var title: String?
    var jsonString: String
    @State private var jsonData: Any?

    var body: some View {
        NavigationView {
            ScrollView {
                if let jsonData = jsonData {
                    if let jsonArray = jsonData as? [[String: Any]] {
                        VStack {
                            ForEach(jsonArray.indices, id: \.self) { index in
                                let item = jsonArray[index]
                                let title = getTitle(for: item, at: index)
                                JSONTreeNodeView(key: title, value: item, isRoot: true)
                            }
                        }
                    } else {
                        JSONTreeNodeView(key: "Root", value: jsonData, isRoot: true)
                    }
                } else {
                    Text(Labels.INVALID_JSON)
                }
            }
            .padding()
            .navigationTitle(title ?? Labels.LOGS_TREE)
        }
        .onAppear {
            parseJSON()
        }
    }

    func parseJSON() {
        guard let data = Helper.removeNewLines(from: jsonString).data(using: .utf8),
              let jsonObject = try? JSONSerialization.jsonObject(with: data, options: []) else {
            return
        }
        jsonData = jsonObject
    }

    func getTitle(for item: [String: Any], at index: Int) -> String {
        // Use 'id' field if present, else fallback to index
        if let id = item["id"] {
            return "\(id)"
        }
        return "[\(index)]"
    }
    
    func jsonArrayToMultilineString(jsonArrayString: String) -> String? {
        guard let data = jsonArrayString.data(using: .utf8) else { return nil }
        
        do {
            // Decode JSON array from string
            let jsonArray = try JSONDecoder().decode([String].self, from: data)
            
            // Join array elements with newlines
            return jsonArray.joined(separator: "\n")
        } catch {
            print("Error decoding JSON: \(error)")
            return nil
        }
    }
}
