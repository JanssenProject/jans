//
//  WebView.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/23/22.
//

import SwiftUI
import WebKit
import Foundation

struct Webview : UIViewRepresentable {

    private let request: URLRequest
    private let webview = WKWebView()

    init(url: URL) {
        self.request = URLRequest(url: url)
    }

    class Coordinator: NSObject, WKUIDelegate, WKNavigationDelegate, WKScriptMessageHandler {
        var parent: Webview

        init(_ parent: Webview) {
            self.parent = parent
        }

        // Delegate methods go here

        func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
            // alert functionality goes here
            print("WKWebView message: \(message)")
        }

        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            print("WKWebView didReceive - name: \(message.name), message: \(message.body)")
        }

        func webView(_ webView: WKWebView, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
            guard let serverTrust = challenge.protectionSpace.serverTrust else {
                completionHandler(.cancelAuthenticationChallenge, nil)
                return
            }
            let exceptions = SecTrustCopyExceptions(serverTrust)
            SecTrustSetExceptions(serverTrust, exceptions)
            completionHandler(.useCredential, URLCredential(trust: serverTrust))
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIView(context: Context) -> WKWebView  {
        webview
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.uiDelegate = context.coordinator
        uiView.navigationDelegate = context.coordinator
        uiView.load(request)
    }

    func goBack(){
        webview.goBack()
    }

    func goForward(){
        webview.goForward()
    }

    func reload(){
        webview.reload()
    }
}
