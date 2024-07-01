//
//  GluuTextField.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/21/22.
//

import SwiftUI

struct GluuTextField: View {

    private let font: Font = .system(size: 14)
    private let textColor: Color = .black
    private let defaultHeight: Double = 24

    let title: String
    let text: String
    let placeholder: String

    var onChangeText: (String) -> ()

    init(title: String, text: String, placeholder: String, onChangeText: @escaping (String) -> ()) {
        self.title = title
        self.text = text
        self.placeholder = placeholder
        self.onChangeText = onChangeText
    }

    var body: some View {
        VStack(alignment: .leading) {
            if !title.isEmpty {
                Text(title)
                    .font(font)
                    .foregroundColor(textColor)
            }

            HStack {
                GeometryReader(content: { geo in
                    TextField(placeholder, text: Binding(get: {text}, set: onChangeText ))
                        .frame(width: abs(geo.size.width - 32), height: 20, alignment: .leading)
                        .modifier(GluuTextFieldModifier())
                })
            }
        }
    }
}

struct GluuTextFieldModifier: ViewModifier {

    var placeHolder: String = ""
    var font: Font = .system(size: 14)
    var textColor: Color = .black

    func body(content: Content) -> some View {
        content
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(Color.gray, lineWidth: 2)
            )
            .accentColor(.white)
            .foregroundColor(textColor)
            .font(font)
    }
}
