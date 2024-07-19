//
//  PublicKeyCredentialRowView.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 17.07.2024.
//

import SwiftUI

struct PublicKeyCredentialRowView: View {
    
    private let publicKeyCredentialRow: PublicKeyCredentialRow
    private let isSelected: Bool
    
    init(publicKeyCredentialRow: PublicKeyCredentialRow, isSelected: Bool) {
        self.publicKeyCredentialRow = publicKeyCredentialRow
        self.isSelected = isSelected
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Image(publicKeyCredentialRow.icon)
                .resizable()
                .scaledToFit()
                .frame(width: 25, height: 25)
            VStack(alignment: .leading, spacing: 6) {
                Text(publicKeyCredentialRow.heading)
                    .font(.headline)
                Text(publicKeyCredentialRow.subheading)
                    .font(.subheadline)
            }
            Spacer()
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 12)
        .background(backgroundColor)
    }
    
    var backgroundColor: Color {
        isSelected ? .gray : .white
    }
}

#Preview {
    PublicKeyCredentialRowView(publicKeyCredentialRow: 
                                PublicKeyCredentialRow(publicKeyCredential:
                                                        PublicKeyCredentialSource()),
                               isSelected: false
    )
}
