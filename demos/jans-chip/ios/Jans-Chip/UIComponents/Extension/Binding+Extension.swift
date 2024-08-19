//
//  Binding+Extension.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 29.07.2024.
//

import SwiftUI

extension Binding where Value == String {
    
    func defaultValue(_ fallback: String) -> Binding<String> {
        return Binding<String>(get: {
            return self.wrappedValue
        }) { value in
            self.wrappedValue = value
        }
    }
}
