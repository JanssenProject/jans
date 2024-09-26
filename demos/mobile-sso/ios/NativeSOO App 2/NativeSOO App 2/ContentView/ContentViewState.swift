//
//  ContentViewState.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/21/22.
//

import SwiftUI

final class ContentViewState: ObservableObject {

    @Published var hostUrl: String = Configuration.baseURL
    @Published var showingAlert: Bool = false
    @Published var showingWebView: Bool = false
    @Published var errorText: String = "Something went wrong!"
    @Published var resultText: String = "Please click Login first, to see result"
    @Published var authorizeWebViewURL: URL?
}
