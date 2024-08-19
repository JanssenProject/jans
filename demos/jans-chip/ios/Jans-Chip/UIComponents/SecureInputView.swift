//
//  SecureInputView.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 29.07.2024.
//

import SwiftUI

struct SecureInputView: View {
    
    @State var titleKey = ""
    @Binding var inputValue: String
    
    @State private var text: String = ""
    @State private var isSecured = true
    
    var body: some View {
        ZStack(alignment: .trailing) {
            TextField(titleKey, text: $text)
                .onChange(of: text) { newValue in
                    guard isSecured else { inputValue = newValue; return }
                    if newValue.count >= inputValue.count {
                        let newItem = newValue.filter { $0 != Character("●") }
                        inputValue.append(newItem)
                    } else {
                        inputValue.removeLast()
                    }
                    text = String(newValue.map { _ in Character("●") })
                }
            Button {
                isSecured.toggle()
                text = isSecured ? String(inputValue.map { _ in Character("●") }) :  inputValue
            } label: {
                (isSecured ? Image(systemName: "eye.slash") : Image(systemName: "eye"))
                    .tint(.gray)
            }
            .padding(.trailing, 12)
        }
    }
}
