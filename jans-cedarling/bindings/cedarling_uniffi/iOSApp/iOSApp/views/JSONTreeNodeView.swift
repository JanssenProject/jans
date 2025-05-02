//
//  JSONTreeNodeView.swift
//  iOSApp
//
//  Created by Arnab Dutta on 14/02/25.
//

import Foundation
import SwiftUI

struct JSONTreeNodeView: View {
    let key: String
    let value: Any
    let isRoot: Bool
    @State private var isExpanded: Bool = false

    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                if isExpandable {
                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .foregroundColor(.blue)
                        .onTapGesture { isExpanded.toggle() }
                } else {
                    Spacer().frame(width: 16) // Placeholder for alignment
                }

                Text("\(key):")
                    .font(.headline)
                    .foregroundColor(.primary)

                if isExpandable {
                    Spacer()
                } else {
                    Text(formatValue(value))
                        .foregroundColor(.secondary)
                }
            }
            .padding(.leading, isRoot ? 0 : 10)
            .padding(.vertical, 4)
            .background(isRoot ? Color.clear : Color.gray.opacity(0.1))
            .cornerRadius(8)

            if isExpanded {
                if let dictionary = value as? [String: Any] {
                    VStack(alignment: .leading) {
                        ForEach(dictionary.keys.sorted(), id: \.self) { key in
                            JSONTreeNodeView(key: key, value: dictionary[key]!, isRoot: false)
                        }
                    }
                    .padding(.leading, 20)
                } else if let array = value as? [Any] {
                    VStack(alignment: .leading) {
                        ForEach(array.indices, id: \.self) { index in
                            JSONTreeNodeView(key: "[\(index)]", value: array[index], isRoot: false)
                        }
                    }
                    .padding(.leading, 20)
                }
            }
        }
        .animation(.easeInOut(duration: 0.2), value: isExpanded)
    }

    private var isExpandable: Bool {
        return value is [String: Any] || value is [Any]
    }

    private func formatValue(_ value: Any) -> String {
        if let str = value as? String {
            return "\"\(str)\""
        } else if let num = value as? NSNumber {
            return "\(num)"
        } else {
            return "{...}"
        }
    }
}
