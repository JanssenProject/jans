//
//  NetworkManager.swift
//  NativeSSO
//
//  Created by Nazar Yavornytskyy on 11/21/22.
//

import Foundation

final class NetworkManager: NSObject {

    private override init() {

    }

    static let shared = NetworkManager()

    // MARK: - Public

    func register(callback: ((String, String) -> Void)?) {
        if let clientID = KeychainManager.shared.clientID, let clientSecret = KeychainManager.shared.clientSecret {
            callback?(clientID, clientSecret)
            return
        }

        let data: [String : Any] = [
            "grant_types": ["authorization_code", "urn:ietf:params:oauth:grant-type:token-exchange", "implicit"],
            "subject_type": "pairwise",
            "application_type": "web",
            "scope" : "openid profile device_sso email",
            "redirect_uris" : ["iOSNativeSSO://\(Configuration.domain)/jans-auth-rp/home.htm"],
            "client_name" : "jans test app",
            "additional_audience" : [ ],
            "response_types" : [ "code", "id_token" ]
        ]
        let requestBody = try? JSONSerialization.data(withJSONObject: data, options: [])
        let baseURL = Configuration.host
        let endpoint = Configuration.path + "/register"

        var urlComponents = URLComponents()
        urlComponents.scheme = "https"
        urlComponents.host = baseURL
        urlComponents.path = endpoint
        let url = urlComponents.url!
        var request = URLRequest(url: url)

        request.httpMethod = "POST"
        request.httpBody = requestBody

        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")

        let session = URLSession(configuration: URLSessionConfiguration.default, delegate: self, delegateQueue: nil)

        let task = session.dataTask(with: request) { (data, response, error) in
            if let error = error {
                print("error: \(error)")
                return
            }
            print("Response: \((response as! HTTPURLResponse).statusCode)")
            if let data = data {
                do {
                    let decoder = JSONDecoder()
                    let responseRegister: Register = try decoder.decode(Register.self, from: data)

                    let client_id = responseRegister.client_id
                    let client_secret = responseRegister.client_secret

                    KeychainManager.shared.clientID = client_id
                    KeychainManager.shared.clientSecret = client_secret

                    callback?(client_id, client_secret)
                } catch {
                    print(error)
                }
            }
        }
        task.resume()
    }

    func authorize(clientId: String, redirectUri: String? = nil, callback: ((URL) -> Void)?) {
        let baseURL = Configuration.host
        let endpoint = Configuration.path + "/authorize"

        var urlComponents = URLComponents()
        urlComponents.scheme = "https"
        urlComponents.host = baseURL
        urlComponents.path = endpoint

        urlComponents.queryItems = [
            URLQueryItem(name: "response_type", value: "code+id_token"),
            URLQueryItem(name: "client_id", value: clientId),
            URLQueryItem(name: "scope", value: "openid+profile+device_sso+email"),
            URLQueryItem(name: "redirect_uri", value: "iOSNativeSSO://\(Configuration.domain)/jans-auth-rp/home.htm"),
            URLQueryItem(name: "state", value: "bea59c20-3cf7-4b36-9cfc-38754e5d2e3e"),
            URLQueryItem(name: "nonce", value: "26ad6654-8ed8-4fa3-abd2-3cd6f0c20022"),
            URLQueryItem(name: "prompt", value: ""),
            URLQueryItem(name: "ui_locales", value: ""),
            URLQueryItem(name: "claims_locales", value: ""),
            URLQueryItem(name: "acr_values", value: ""),
            URLQueryItem(name: "request_session_id", value: "false")
        ]

        if let url = urlComponents.url {
            callback?(url)
        }
    }

    func getToken(code: String, redirectUri: String? = nil, callback: ((Token) -> Void)?) {
        let baseURL = Configuration.host
        let endpoint = Configuration.path + "/token"

        var urlComponents = URLComponents()
        urlComponents.scheme = "https"
        urlComponents.host = baseURL
        urlComponents.path = endpoint

        let client_id = KeychainManager.shared.clientID ?? ""
        let client_secret = KeychainManager.shared.clientSecret ?? ""
        let encodedAutorization = "\(client_id):\(client_secret)".base64Encoded() ?? ""

        let url = urlComponents.url!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        request.addValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.addValue("Basic \(encodedAutorization)", forHTTPHeaderField: "Authorization")

        var requestBodyComponents = URLComponents()
        requestBodyComponents.queryItems = [
            URLQueryItem(name: "grant_type", value: "authorization_code"),
            URLQueryItem(name: "code", value: code),
            URLQueryItem(name: "redirect_uri", value: "iOSNativeSSO://\(Configuration.domain)/jans-auth-rp/home.htm")
        ]

        request.httpBody = requestBodyComponents.query?.data(using: .utf8)

        let session = URLSession(configuration: URLSessionConfiguration.default, delegate: self, delegateQueue: nil)

        let task = session.dataTask(with: request) { (data, response, error) in
            if let error = error {
                print("error: \(error)")
                return
            }
            print("Response: \((response as! HTTPURLResponse).statusCode)")
            if let data = data {
                do {
                    let decoder = JSONDecoder()
                    let token: Token = try decoder.decode(Token.self, from: data)
                    callback?(token)
                } catch {
                    print(error)
                }
            }
        }
        task.resume()
    }

    func getUserInfo(token: Token, callback: ((String) -> Void)?) {
        let baseURL = Configuration.host
        let endpoint = Configuration.path + "/userinfo"

        var urlComponents = URLComponents()
        urlComponents.scheme = "https"
        urlComponents.host = baseURL
        urlComponents.path = endpoint

        let url = urlComponents.url!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        request.addValue("Bearer \(token.access_token)", forHTTPHeaderField: "Authorization")

        let session = URLSession(configuration: URLSessionConfiguration.default, delegate: self, delegateQueue: nil)

        let task = session.dataTask(with: request) { (data, response, error) in
            if let error = error {
                print("error: \(error)")
                return
            }
            print("Response: \((response as! HTTPURLResponse).statusCode)")
            if let data = data {
                let result = String(data: data, encoding: .utf8)
                callback?(result ?? "none")
            }
        }
        task.resume()
    }
}

extension NetworkManager: URLSessionDelegate {

    public func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
           //Trust the certificate even if not valid
           let urlCredential = URLCredential(trust: challenge.protectionSpace.serverTrust!)

           completionHandler(.useCredential, urlCredential)
        }
}

extension URLRequest {

    public func cURL(pretty: Bool = false) -> String {
        let newLine = pretty ? "\\\n" : ""
        let method = (pretty ? "--request " : "-X ") + "\(self.httpMethod ?? "GET") \(newLine)"
        let url: String = (pretty ? "--url " : "") + "\'\(self.url?.absoluteString ?? "")\' \(newLine)"

        var cURL = "curl "
        var header = ""
        var data: String = ""

        if let httpHeaders = self.allHTTPHeaderFields, httpHeaders.keys.count > 0 {
            for (key,value) in httpHeaders {
                header += (pretty ? "--header " : "-H ") + "\'\(key): \(value)\' \(newLine)"
            }
        }

        if let bodyData = self.httpBody, let bodyString = String(data: bodyData, encoding: .utf8),  !bodyString.isEmpty {
            data = "--data '\(bodyString)'"
        }

        cURL += method + url + header + data

        return cURL
    }
}

extension String {

    func base64Encoded() -> String? {
        data(using: .utf8)?.base64EncodedString()
    }

    func base64Decoded() -> String? {
        guard let data = Data(base64Encoded: self) else { return nil }
        return String(data: data, encoding: .utf8)
    }
}
