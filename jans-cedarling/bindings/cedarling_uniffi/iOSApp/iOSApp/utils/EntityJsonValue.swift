//
//  EntityJsonValue.swift
//  iOSApp
//
//  Created by Arnab Dutta on 24/04/25.
//

import Foundation

enum EntityJsonValue {
    case string(String)
    case number(Double)
    case bool(Bool)
    case array([EntityJsonValue])
    case dictionary([String: EntityJsonValue])
}

extension EntityJsonValue {
    static func from(any value: Any) -> EntityJsonValue? {
        switch value {
        case let string as String:
            return .string(string)
        case let number as NSNumber:
            if CFGetTypeID(number) == CFBooleanGetTypeID() {
                return .bool(number.boolValue)
            } else {
                return .number(number.doubleValue)
            }
        case let dict as [String: Any]:
            return .dictionary(dict.compactMapValues { EntityJsonValue.from(any: $0) })
        case let array as [Any]:
            return .array(array.compactMap { EntityJsonValue.from(any: $0) })
        default:
            return nil
        }
    }
}
